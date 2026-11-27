# Infrastructure as Code (Terraform) et pipelines

Ce document décrit l'infrastructure GCP de TheZaurus-v2 gérée par Terraform, les pipelines GitHub Actions
qui l'utilisent, et **où modifier quoi** en cas de besoin.

> Aucune valeur secrète ni identifiant personnel n'est stocké dans ce document ou dans le code :
> les secrets sont dans Secret Manager, les IDs GitHub autorisés dans `infra/shared/variables.tf`.

---

## 1. Vue d'ensemble

Un seul projet GCP héberge la prod et le staging. Les environnements sont isolés **par le nommage**
(service, base Firestore, bucket, job Scheduler, secrets), et partagent les comptes de service (SA).

| Dossier | Contenu | Appliqué par | Comment |
|---|---|---|---|
| `infra/bootstrap` | Bucket du state Terraform (`<projet>-tfstate`) | un admin | en local, une seule fois |
| `infra/shared` | SAs, IAM, Workload Identity Federation (WIF), secrets (conteneurs), Artifact Registry | un admin | **en local uniquement** |
| `infra/modules/environment` | Module commun : Cloud Run (coquille), Firestore + index, bucket photos, job Scheduler | — | appelé par les envs |
| `infra/envs/staging` | Staging : appelle le module avec les noms `*-staging`, destructible | CI ou local | `infra-env.yml` |
| `infra/envs/prod` | Prod : appelle le module avec les noms existants, protégée | CI (règle d'usage) | `infra-env.yml` ; local possible en dernier recours (§8) |

**Ordre d'application** : `bootstrap` → `shared` → `envs/*`.

**State** : un seul bucket GCS versionné, un préfixe par dossier (`bootstrap`, `shared`, `envs/staging`,
`envs/prod`), déclaré dans chaque `terraform.tf`. Le verrou GCS empêche deux applies simultanés sur le même state.

**Ce que Terraform ne gère pas** :
- les **révisions** Cloud Run (images, variables, conteneurs) : gérées par `_deploy.yml`
  (`gcloud run services replace`). Terraform ignore `template` (`lifecycle.ignore_changes`) ;
- les **valeurs** des secrets : ajoutées à la main dans Secret Manager. Terraform ne gère que les conteneurs.

---

## 2. Comptes de service

Définis dans `infra/shared/service_account.tf`, droits dans `infra/shared/iam.tf`.

| SA (ressource Terraform) | Rôle | Droits principaux |
|---|---|---|
| `runtime` | Identité de l'application Cloud Run (prod et staging) | `datastore.user`, `secretmanager.secretAccessor`, `aiplatform.user` (projet) ; `storage.objectUser` sur chaque bucket photos (module) |
| `ci_deploy` | Build et déploiement Cloud Run par GitHub Actions | `artifactregistry.createOnPushWriter`, `run.developer` (projet) ; actAs **uniquement** sur `runtime` |
| `ci_agent` | Déploiement de l'agent IA par GitHub Actions | `aiplatform.editor` |
| `scheduler` | Identité du token OIDC du job de rappel | aucun rôle projet (l'API vérifie son email : `FEEDBACK_REMINDER_INVOKER`) |
| `terraform_sa` | Terraform en CI (`infra-env.yml`) | `run.admin`, `datastore.owner`, `cloudscheduler.admin`, `storage.admin` ; actAs sur `runtime` et `scheduler`. **Aucun droit IAM admin ni Secret Manager** : `shared` reste appliqué en local |

Principe : moindre privilège. Pas de `iam.serviceAccountUser` au niveau du projet (il permettrait d'agir
en tant que n'importe quel SA, y compris `terraform_sa`).

---

## 3. Qui peut s'authentifier sur GCP depuis GitHub (WIF)

Fichier : `infra/shared/oidc.tf`. Deux pools **séparés** : une condition IAM vise un pool entier, un provider
plus permissif dans le même pool contournerait la restriction.

| Pool | Utilisé par | Condition (toutes doivent être vraies) | SA obtenu |
|---|---|---|---|
| `github-actions-pool` | `release.yml` → `_deploy.yml`, `_image-*.yml`, `_deploy-agent.yml` | le token vient du repo | `ci_deploy`, `ci_agent` |
| `github-action-terraform` | `infra-env.yml` | repo + `actor_id` autorisé + `job_workflow_ref` = `infra-env.yml@refs/heads/main` + `run_attempt == 1` | `terraform_sa` |

Conséquences pour le pool Terraform :
- seul **`infra-env.yml` sur `main`** obtient un token. Un autre workflow ou une autre branche est refusé ;
- seules les **personnes listées** dans `tf_admin_actor_ids` peuvent lancer un run qui s'authentifie.
  Toute personne avec un accès en écriture au repo peut cliquer sur « Run workflow », mais l'authentification
  GCP échouera ;
- un **« Re-run »** est refusé (`run_attempt` = 2) : il garde l'acteur d'origine, quelqu'un d'autre pourrait
  donc relancer un run sous l'identité d'une personne autorisée. Il faut lancer un nouveau run.

---

## 4. Pipeline infra : `infra-env.yml`

Lancement manuel : Actions → *Infra Env (terraform)* → *Run workflow* (branche `main`).
Validation en **deux runs** (pas de GitHub Environments disponibles) :

1. **`plan`** (ou `plan-destroy`) : calcule le plan, l'affiche en entier dans le résumé du run, le sauvegarde
   en artefact (2 jours). Rien n'est modifié.
2. **Relire le plan**, puis lancer **`apply`** avec `plan_run_id` = ID du run 1 (indiqué dans le résumé).
   Le plan relu est appliqué tel quel, sans recalcul. Ne pas lancer le run 2 = refuser le plan.

### Contrôles, dans l'ordre d'exécution

Le run `apply` ne recalcule rien : il télécharge l'artefact `tfplan-<env>` du run `plan_run_id`
(fichier de plan `tfplan` + `plan-meta.txt`) et l'applique. Chaque contrôle vérifie que ce qui va être
appliqué est bien ce qui a été relu.

**1. Guard** (job `guard`, avant toute connexion à GCP)
Refuse une branche autre que `main`, un `plan-destroy` en production, et un `apply` sans `plan_run_id` numérique.

**2. Origine du plan** (step « Vérifier l'origine du plan »)
*Risque couvert : appliquer un plan que personne n'a relu.*
N'importe quel workflow du repo, sur n'importe quelle branche, peut publier un artefact nommé
`tfplan-production` contenant un plan et un `plan-meta.txt` fabriqués (par exemple un plan qui supprime
des ressources). Si on donnait l'ID de ce run en `plan_run_id`, `terraform_sa` l'appliquerait.
Le step interroge donc l'API GitHub sur le run `plan_run_id`. Ces informations sont fournies par GitHub et
le workflow ne peut pas les falsifier. Le run doit être :
- le workflow `.github/workflows/infra-env.yml`,
- lancé depuis `main`, en `workflow_dispatch` (lancement manuel),
- terminé avec succès,
- dans ce repo (pas un fork).

Seul ce workflow, sur `main`, peut s'authentifier avec `terraform_sa` (§3) : son plan a été calculé sur
le vrai state et affiché dans son résumé.

**3. Cohérence du plan** (step « Vérifier le plan », à partir de `plan-meta.txt` écrit par le run plan)
- **Même environnement** : un plan calculé pour staging ne peut pas être appliqué en production, et inversement.
- **Même code Terraform** : `INFRA_TREE` est l'empreinte git du dossier `infra/` au moment du plan.
  Si un commit a modifié `infra/` sur `main` entre le plan et l'apply, l'apply est refusé : le code relu n'est
  plus celui de `main`. Un commit hors de `infra/` (application, docs) n'invalide pas le plan.
- **Pas de plan de destruction en production** (deuxième vérification après le guard).

**4. State inchangé** (Terraform lui-même, au moment de `terraform apply tfplan`)
*Risque couvert : appliquer un plan calculé sur un state qui n'existe plus.*
Le fichier de plan mémorise la version (serial) du state sur laquelle il a été calculé. Si le state a été
modifié depuis (un autre apply, même partiel ou en échec, en CI ou en local), Terraform refuse :
`Saved plan is stale`. Il faut relancer un `plan`.
Ce contrôle porte sur le **state**, pas sur GCP : une modification faite à la main dans la console après
le plan n'est pas détectée.

**5. Un seul run à la fois** (`concurrency: infra`)
Les runs Terraform s'exécutent l'un après l'autre et ne sont jamais annulés en cours (un apply interrompu
laisse l'infra à moitié modifiée).

| Erreur | Contrôle | Quoi faire |
|---|---|---|
| `n'est pas un plan infra-env.yml valide` | 2 | vérifier le `plan_run_id` (run de plan lancé depuis `main`, réussi) |
| `Ce plan vise 'staging', pas 'production'` | 3 | choisir le même environnement que le plan |
| `Le code infra/ a changé depuis le plan` | 3 | relancer un `plan` sur le `main` actuel |
| `Saved plan is stale` | 4 | relancer un `plan` |

La version de Terraform est fixée dans le workflow (`terraform_version`) : utiliser la **même version en local**,
une version plus récente pourrait migrer le format du state.

---

## 5. Pipeline de livraison : `release.yml`

Lancement manuel, choix de l'environnement et des services. Détails dans les commentaires du workflow.

- **staging** : depuis **n'importe quelle branche** (prerelease `X.Y.Z-<branche>.N`). L'agent IA n'est pas déployé en staging.
- **production** : depuis **`main` uniquement** (guard du workflow).
- Configuration de chaque environnement : `.github/envs/common.env`, puis `.github/envs/<environment>.env`
  (le second l'emporte). Les noms doivent correspondre à `infra/envs/<env>/main.tf`.

---

## 6. Protections contre la suppression

| Protection | Où | Portée |
|---|---|---|
| `prevent_destroy` | `bootstrap`, `shared` (SAs, pools WIF, secrets, registry, bucket de state) | Terraform refuse tout plan qui détruit la ressource |
| `delete_protection_state` Firestore | `envs/prod` (`firestore_delete_protection = true`) | côté **GCP** : suppression bloquée même depuis la console ou gcloud |
| `deletion_policy = ABANDON` Firestore | module, si `protect = true` | un destroy retire la base du state **sans la supprimer** |
| `deletion_protection` Cloud Run | module, si `protect = true` | le service ne peut pas être supprimé par Terraform |
| `force_destroy = false` bucket photos | module, si `protect = true` | un bucket non vide n'est pas supprimé |
| Pas de `plan-destroy` en prod | `infra-env.yml` | double contrôle (guard + vérification du plan) |

Staging (`protect = false`) peut être détruit puis recréé.
Après la suppression d'une base Firestore, son nom reste **réservé quelques minutes**
(`Database ID ... is not available ... retry in N seconds`) : attendre, refaire un plan, puis l'apply.

---

## 7. Où changer quoi

| Besoin | Fichier | Puis |
|---|---|---|
| **Autoriser une personne à lancer `infra-env.yml`** | `infra/shared/variables.tf` → `tf_admin_actor_ids` (ID **numérique** : `gh api users/<login> --jq .id`) | plan + apply `shared` en local |
| Retirer une personne | idem, retirer l'ID | plan + apply `shared` |
| Autoriser une autre branche ou un autre workflow à utiliser `terraform_sa` | `infra/shared/oidc.tf` → `tf_admin_workflow_refs` **et** le guard + la vérification d'origine dans `infra-env.yml` | apply `shared` ; voir §9 (risque) |
| Changer le repo autorisé | `infra/shared/variables.tf` → `github_repository` | apply `shared` |
| Ajouter / retirer un rôle d'un SA | `infra/shared/iam.tf` (`project_roles`, `terraform_sa_project_roles`, actAs) | apply `shared` |
| Ajouter un secret | `infra/shared/secrets.tf` (`secret_keys` si partagé, `env_secrets` si propre à un env), puis ajouter la valeur à la main dans Secret Manager | apply `shared` ; référencer le secret dans `_deploy.yml` |
| Ajouter une ressource à chaque environnement | `infra/modules/environment/` (+ variable sans `default`, puis valeur dans `envs/staging` et `envs/prod`) | plan/apply staging, puis prod |
| Changer un nom de ressource d'un env | `infra/envs/<env>/main.tf` **et** `.github/envs/<env>.env` | ⚠️ un renommage = destruction + recréation |
| Changer la config de déploiement (URL, préfixe, bucket…) | `.github/envs/<env>.env` | prochain `release.yml` |
| Changer la version de Terraform | `terraform_version` dans `infra-env.yml` (×2) + versions locales | toute l'équipe en même temps |
| Changer la version du provider Google | `required_providers` dans chaque `terraform.tf` + `.terraform.lock.hcl` (`terraform init -upgrade`) | plan sur chaque dossier |
| Changer le SA ou le provider WIF de Terraform en CI | `.github/envs/common.env` (`TF_SERVICE_ACCOUNT`, `TF_WIF_PROVIDER`) | — |

---

## 8. Terraform en local et tests

### Ce qui marche en local

Les restrictions WIF (§3) ne concernent **que la CI** et `terraform_sa`. En local, Terraform utilise
**ses propres identifiants** (Application Default Credentials) : `plan` / `apply` fonctionnent sur
**n'importe quel dossier** (`shared`, `envs/staging`, `envs/prod`) si le compte GCP personnel a les droits
nécessaires. Le verrou du state empêche seulement deux applies simultanés sur le même state.

« Prod via la CI » est donc une **règle d'usage**, pas un blocage technique : la CI garantit un plan relu et
une trace dans Actions. Un apply local sur la prod est réservé aux admins, en dernier recours (CI
indisponible, réparation du state), avec la même version de Terraform, et doit être signalé à l'équipe.
Les protections (`prevent_destroy`, delete protection Firestore, `deletion_protection`) s'appliquent aussi en local.

### Une modification d'infra sur staging (depuis une branche)

La CI n'accepte que `main`. Pour tester une branche, on applique **en local** sur staging, avec ses propres
identifiants GCP :

```bash
gcloud auth application-default login   # chacun le lance lui-même
cd infra/envs/staging
terraform init
terraform plan
terraform apply
```

- Prérequis : la **même version de Terraform** que la CI, et des droits GCP personnels suffisants.
- Ne **pas** donner `iam.serviceAccountTokenCreator` sur `terraform_sa` pour « tester comme la CI » :
  ce serait donner ses droits sur la prod.
- Staging reflète **la dernière branche appliquée**. Le prochain plan depuis `main` affichera le retour
  à l'état de `main`, c'est normal.

### Une modification du workflow `infra-env.yml` (avant merge)

`workflow_dispatch` n'est proposé que pour un workflow présent sur `main`. Procédure **temporaire** :

1. `oidc.tf` : ajouter `infra-env.yml@refs/heads/<branche>` à `tf_admin_workflow_refs` ; apply `shared` en local.
2. `infra-env.yml` : ajouter un déclencheur `workflow_call` et autoriser la branche dans le guard et dans
   la vérification d'origine (blocs commentés `TEMPORAIRE`).
3. Créer un workflow de test déclenché par `push` sur la branche, qui appelle `infra-env.yml` avec
   `environment` / `action` / `plan_run_id`. Chaque test = modifier ce fichier, commit, push.
4. **Après le test** : supprimer le workflow de test, les blocs `TEMPORAIRE`, la ligne ajoutée dans `oidc.tf`,
   puis apply `shared` en local. Sans cela, la branche garde l'accès à `terraform_sa`.

---

## 9. Risques acceptés

Choix assumés (projet unique, SAs partagés, pas de GitHub Environments) :

- **Déploiement applicatif** : le pool `github-actions-pool` accepte tout workflow du repo, quelle que soit
  la branche. La règle « prod = `main` » de `release.yml` n'est assurée que par le guard YAML, qu'une branche
  peut modifier.
- **SA runtime partagé** : une version déployée en staging tourne avec le même SA que la prod et peut donc
  accéder aux données Firestore et aux secrets de prod.
- **Bot Slack et clé Mailgun partagés** : le job de rappel de staging est en pause et
  `FEEDBACK_REMINDER_ENABLED=false` en staging.
- **Terraform en CI** : `terraform_sa` sert aux deux environnements. La restriction à `main` + `actor_id` est
  le garde-fou principal : ne pas l'élargir à toutes les branches.

Pour réduire ces risques plus tard : un SA de déploiement et un SA runtime par environnement, ou un projet
GCP dédié au staging.

---

## 10. Dépannage

| Symptôme | Cause | Solution |
|---|---|---|
| Auth GCP refusée dans `infra-env.yml` | branche ≠ `main`, acteur non listé, ou « Re-run » | lancer un nouveau run depuis `main` par une personne listée |
| `Saved plan is stale` | le state a changé depuis le plan (autre apply, même partiel) | relancer un `plan`, puis `apply` avec le nouveau `plan_run_id` |
| `Le code infra/ a changé depuis le plan` | commit sur `infra/` entre le plan et l'apply | relancer un `plan` |
| `Database ID ... is not available` | base Firestore supprimée récemment | attendre le délai indiqué, refaire plan + apply |
| Plan `shared` avec des destructions inattendues | code committé mais jamais appliqué (dérive) | `git log -- infra/shared` pour identifier le commit ; appliquer si c'est voulu |
