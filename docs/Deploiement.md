# Déploiement sur Google Cloud Run

## Architecture

Le workflow [Release](../.github/workflows/release.yml) construit les images puis
appelle [_deploy.yml](../.github/workflows/_deploy.yml). Le manifeste rendu par ce
workflow est la source de vérité de la production ; une modification manuelle
sur Cloud Run sera remplacée au prochain déploiement. Le Compose override sert
au développement local.

Un même service Cloud Run contient trois conteneurs :

- `front` : seul conteneur ingress, port 3000 ; il appelle l’API sur `localhost:8080`.
- `api` : backend Quarkus, port 8080 ; il contrôle les photos et appelle l’agent sur `localhost:8087`.
- `visuals-agent` : rendu Java2D et cadrage Vertex AI, port 8087, sans URL publique distincte.

Les startup probes imposent l’ordre `visuals-agent` → `api` → `front`.
Les images sont référencées par digest. L’agent reste non-root et utilise
l’identité du service Cloud Run pour Vertex AI, sans clé Google ni fichier ADC monté.
Firestore et le bucket privé des photos restent des services GCP externes.
Le `talks-agent` conserve son déploiement séparé sur Agent Runtime.

## Configuration GitHub

Les variables suivantes doivent être disponibles au niveau du dépôt pour la
construction des images :

| Variable | Usage |
| --- | --- |
| `GCP_REGISTRY` | Chemin Artifact Registry, par exemple `europe-west1-docker.pkg.dev/PROJECT/REPOSITORY` |
| `GCP_PROJECT_ID`, `GCP_REGION` | Projet et région de déploiement |
| `WIF_PROVIDER`, `WIF_SERVICE_ACCOUNT` | Fédération GitHub → GCP et compte de déploiement |

L’environnement GitHub `production` apporte ses règles d’approbation et les
variables d’exécution ci-dessous. Les variables communes peuvent être héritées
du dépôt ; le compte de déploiement doit être distinct du compte d’exécution.

| Variable | Usage / défaut |
| --- | --- |
| `CLOUD_RUN_SERVICE` | Nom du service existant |
| `GCP_PROJECT_ID`, `GCP_REGION` | Projet et région de production |
| `WIF_PROVIDER`, `WIF_SERVICE_ACCOUNT` | Authentification du workflow |
| `RUNTIME_SERVICE_ACCOUNT` | Identité des trois conteneurs |
| `NEXTAUTH_URL` | URL publique HTTPS ; enregistrer aussi `/api/auth/callback/google` dans le client OAuth |
| `GOOGLE_CLIENT_ID` | Client OAuth Google |
| `FIRESTORE_DATABASE_ID`, `FIRESTORE_COLLECTION_PREFIX` | Base et préfixe de production |
| `PROFILE_PHOTOS_BUCKET` | Bucket GCS privé, voir [CloudStorage.md](CloudStorage.md) |
| `MAILGUN_DOMAIN`, `SMTP_FROM` | Domaine Mailgun et expéditeur requis par le déploiement existant |
| `MAILGUN_BASE_URL` | API Mailgun, `https://api.eu.mailgun.net` par défaut |
| `SCHEDULER_SERVICE_ACCOUNT`, `FEEDBACK_REMINDER_AUDIENCE`, `SCHEDULER_CRON` | Configuration du workflow Cloud Scheduler existant ; voir [FeedbackReminders.md](FeedbackReminders.md) |
| `FEEDBACK_REMINDER_ENABLED` | Interrupteur des rappels, `false` par défaut |
| `VISUALS_API_KEY_VERSION` | Version numérique du secret partagé, `1` par défaut |
| `VISUALS_AGENT_TIMEOUT` | Timeout API → agent, `PT120S` par défaut ; garder une marge sous le timeout Cloud Run de 300 s |
| `GEMINI_CROP_MODEL` | Modèle de cadrage, `gemini-3.1-flash-lite` par défaut |
| `GOOGLE_CLOUD_LOCATION` | Localisation Vertex AI, `global` par défaut ; choisir une localisation compatible avec le modèle |

`VISUALS_AGENT_URL=http://localhost:8087` et `VISUALS_AI_ENABLED=true` sont définis
par le manifeste. Les paramètres du `talks-agent` restent décrits par son
[workflow de déploiement](../.github/workflows/_deploy-agent.yml).

## Identités et secrets à préparer avant la première release

Activer Cloud Run, Artifact Registry, Secret Manager, Firestore et Vertex AI
(`aiplatform.googleapis.com`) dans le projet et vérifier les quotas du modèle.
Le compte WIF construit/pousse les images et déploie Cloud Run ; il lui faut
notamment Artifact Registry Writer, Cloud Run Admin et Service Account User sur
le compte d’exécution. Il ne faut pas donner ces droits de déploiement au runtime.

Le compte `RUNTIME_SERVICE_ACCOUNT` doit conserver ses accès Firestore et aux
photos ([CloudStorage.md](CloudStorage.md)), disposer de `roles/aiplatform.user`
sur le projet Vertex et de `roles/secretmanager.secretAccessor` sur les secrets
référencés par le manifeste :

- `thezaurus-google-client-secret` : secret OAuth Google ;
- `thezaurus-nextauth-secret` : signature des sessions ;
- `thezaurus-slack-bot-token` et `thezaurus-slack-signing-secret` : intégration Slack existante ;
- `thezaurus-mailgun-sending-key` : envoi des rappels déjà livré sur `main` ;
- **`thezaurus-visuals-api-key`** : nouvelle clé aléatoire non vide, réservée aux appels API → agent.

Créer le dernier secret et sa première version via Secret Manager avant le
premier déploiement de cette PR. L’API et l’agent lisent exactement la même version
numérique (`VISUALS_API_KEY_VERSION`) ; la valeur n’est ni une variable GitHub ni
une valeur inscrite dans le manifeste ou l’image. Pour une rotation, ajouter une
version du secret, changer `VISUALS_API_KEY_VERSION`, puis relancer une release.
Conserver les anciennes versions actives pour permettre un rollback.

Aucune création de secret ni modification IAM n’est effectuée par le workflow.
Un secret absent ou inaccessible empêche le démarrage de la nouvelle révision.
Une identité sans accès Vertex empêchera le cadrage des photos autorisées.

## Livrer

1. Fusionner les PR nécessaires sur `main`, dans l’ordre de leur stack.
2. Ouvrir **Actions → Release → Run workflow**, sur `main`.
3. Cocher **API, front et agent visuels** pour livrer l’application complète.
   Cette sélection teste les trois services, publie les versions `api`, `front`
   et `agents`, puis construit ou réutilise leurs images immuables. Elle inclut
   les visuels même lorsque **Agent (talks-agent)** est décoché.
4. Cocher également **Agent (talks-agent)** uniquement pour son déploiement séparé.
5. Approuver l’environnement `production` si sa protection le demande.

Le nouvel agent utilise la version partagée `agents-vX.Y.Z` déjà employée pour
le dossier `agents/`. Les images sont respectivement `api`, `front` et
`visuals-agent` dans `GCP_REGISTRY`. Le workflow vérifie l’image visuels par son
digest avant de déployer : utilisateur non-root, santé, refus sans clé, rendu
PNG et limites de participants, sans appel Vertex ni credentials cloud.

Cloud Run attend les startup probes avant de diriger le trafic vers la révision.
Le smoke test après déploiement vérifie le front et NextAuth. En cas d’échec du
déploiement ou du smoke test, le workflow restaure le trafic de la révision précédente
quand elle existe. Le retour arrière concerne les trois conteneurs ensemble.

## Vérifier après déploiement

Depuis l’application avec un compte autorisé, télécharger un visuel de talk et
un visuel d’article, dont un cas avec photo autorisée pour vérifier Vertex AI,
puis un cas sans photo ou avec autorisation refusée pour vérifier le logo Zenika.
Ce contrôle authentifié n’est pas couvert par le smoke test NextAuth ni par le
test d’image sans IA. Ne pas envoyer de portrait réel sans autorisation photo IA.

Consulter le résumé du workflow (digests et révision précédente) et les logs :

```sh
gcloud run services logs read SERVICE --project PROJECT --region REGION --limit 100
```

Le front est l’unique endpoint exposé ; ne pas créer un accès public supplémentaire
pour les ports 8080 ou 8087. Le workflow ne modifie pas la politique d’accès ingress
existante ni les réglages OAuth/IAP du service.
