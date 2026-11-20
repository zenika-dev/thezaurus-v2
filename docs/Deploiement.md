# Déploiement sur Google Cloud Run

## Architecture

Le workflow [Release](../.github/workflows/release.yml) construit les images puis
appelle [_deploy.yml](../.github/workflows/_deploy.yml). Le manifeste rendu par ce
workflow est la source de vérité de la production ; une modification manuelle
sur Cloud Run sera remplacée au prochain déploiement. Le Compose override sert
au développement local.

L’application conserve son service multi-conteneurs : `front` (ingress, port 3000)
et `api` (port 8080), avec une dépendance de démarrage du front sur l’API.

L’agent visuels est un **service Cloud Run indépendant**, avec sa propre URL HTTPS,
son identité, son scaling et ses révisions. TheZaurus et les autres applications
l’appellent avec leur propre clé dans `X-API-Key`. L’accès Cloud Run IAM est désactivé
pour ce service, afin de permettre des clients hébergés hors GCP ; les routes
`/visuals` sont protégées par l’agent. Les endpoints de santé et la documentation
OpenAPI ne contiennent pas de données métier et restent publics.

Le compte de l’agent utilise Vertex AI via les ADC gérés de Cloud Run. Il n’a besoin
ni de Firestore, ni du bucket des photos, ni de fichier de credentials monté.
Chaque client fournit uniquement les pixels qu’il est autorisé à transmettre.
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
| `RUNTIME_SERVICE_ACCOUNT` | Identité du front et de l’API |
| `CLOUD_RUN_VISUALS_SERVICE` | Nom du service visuels indépendant, distinct de `CLOUD_RUN_SERVICE` |
| `VISUALS_RUNTIME_SERVICE_ACCOUNT` | Identité propre à l’agent : Vertex AI et lecture de son secret de clés |
| `NEXTAUTH_URL` | URL publique HTTPS ; enregistrer aussi `/api/auth/callback/google` dans le client OAuth |
| `GOOGLE_CLIENT_ID` | Client OAuth Google |
| `FIRESTORE_DATABASE_ID`, `FIRESTORE_COLLECTION_PREFIX` | Base et préfixe de production |
| `PROFILE_PHOTOS_BUCKET` | Bucket GCS privé, voir [CloudStorage.md](CloudStorage.md) |
| `MAILGUN_DOMAIN`, `SMTP_FROM` | Domaine Mailgun et expéditeur requis par le déploiement existant |
| `MAILGUN_BASE_URL` | API Mailgun, `https://api.eu.mailgun.net` par défaut |
| `SCHEDULER_SERVICE_ACCOUNT`, `FEEDBACK_REMINDER_AUDIENCE`, `SCHEDULER_CRON` | Configuration du workflow Cloud Scheduler existant ; voir [FeedbackReminders.md](FeedbackReminders.md) |
| `FEEDBACK_REMINDER_ENABLED` | Interrupteur des rappels, `false` par défaut |
| `VISUALS_API_KEY_VERSION` | Version de la clé cliente TheZaurus (`thezaurus-visuals-api-key`), `1` par défaut |
| `VISUALS_API_KEYS_VERSION` | Version du registre de clés de l’agent (`visuals-client-api-keys`), `1` par défaut |
| `VISUALS_AGENT_TIMEOUT` | Timeout API → agent, `PT120S` par défaut ; garder une marge sous le timeout Cloud Run de 300 s |
| `GEMINI_CROP_MODEL` | Modèle de cadrage, `gemini-3.1-flash-lite` par défaut |
| `GOOGLE_CLOUD_LOCATION` | Localisation Vertex AI, `global` par défaut ; choisir une localisation compatible avec le modèle |

Le déploiement de l’application résout `VISUALS_AGENT_URL` à partir du service
`CLOUD_RUN_VISUALS_SERVICE`. Le manifeste de l’agent active `VISUALS_AI_ENABLED=true`
et `VISUALS_REQUIRE_API_KEY=true`. Les paramètres du `talks-agent` restent décrits par son
[workflow de déploiement](../.github/workflows/_deploy-agent.yml).

## Identités et secrets à préparer avant la première release

Activer Cloud Run, Artifact Registry, Secret Manager, Firestore et Vertex AI
(`aiplatform.googleapis.com`) dans le projet et vérifier les quotas du modèle.
Le compte WIF construit/pousse les images et déploie Cloud Run ; il lui faut
notamment Artifact Registry Writer, Cloud Run Admin et Service Account User sur
le compte d’exécution. Il ne faut pas donner ces droits de déploiement au runtime.

Le compte `RUNTIME_SERVICE_ACCOUNT` conserve ses accès Firestore, aux photos
([CloudStorage.md](CloudStorage.md)) et aux secrets existants de l’application :
`thezaurus-google-client-secret`, `thezaurus-nextauth-secret`,
`thezaurus-slack-bot-token`, `thezaurus-slack-signing-secret` et
`thezaurus-mailgun-sending-key`. Il lit aussi sa clé cliente `thezaurus-visuals-api-key`.

Le compte **`VISUALS_RUNTIME_SERVICE_ACCOUNT`** reçoit `roles/aiplatform.user` sur
le projet Vertex et `roles/secretmanager.secretAccessor` uniquement sur le secret
**`visuals-client-api-keys`**. Le compte WIF doit pouvoir déployer ce second service
et utiliser son compte d’exécution. Aucun client de l’agent n’a besoin d’un droit
IAM Cloud Run pour l’appeler.

Créer deux secrets avant la première release :

- `visuals-client-api-keys` : objet JSON associant chaque application à une clé
  aléatoire distincte (au moins 32 octets aléatoires recommandés) ;
- `thezaurus-visuals-api-key` : uniquement la valeur de la clé TheZaurus, identique
  à l’entrée `thezaurus` du JSON, lisible par l’API de TheZaurus.

Exemple de structure du registre (remplacer les valeurs d’illustration) :

```json
{
  "thezaurus": "CLE_ALEATOIRE_RESERVEE_A_THEZAURUS",
  "application-externe": "AUTRE_CLE_ALEATOIRE_INDEPENDANTE"
}
```

Le registre complet ne doit être distribué à aucun client. Fournir à chaque
application uniquement sa clé, à conserver dans son backend ou son gestionnaire
de secrets, jamais dans un bundle navigateur. Le JSON doit contenir au moins un
client, des valeurs textuelles non vides et des clés distinctes ; une configuration
invalide ou absente bloque le démarrage en production sans journaliser les secrets.

Pour ajouter, révoquer ou faire tourner une clé, publier une nouvelle version du
registre et mettre à jour `VISUALS_API_KEYS_VERSION`, puis relancer uniquement le
service visuels. La nouvelle liste s’applique aux instances de la nouvelle révision ;
les clés des autres applications restent inchangées. Un rollback vers une ancienne
révision peut restaurer son ancien jeu de clés. La rotation de la clé TheZaurus
nécessite aussi une nouvelle version de son secret et `VISUALS_API_KEY_VERSION`.

Aucune création de secret ni modification de rôles IAM n’est effectuée par le workflow.
L’exposition HTTPS du service visuels avec authentification par clé est définie dans
son manifeste (`run.googleapis.com/invoker-iam-disabled: true`).

## Livrer

Pour cette évolution du cadrage, livrer l’agent visuels avant l’API : celle-ci
transmet désormais `autoCrop` par personne. Sélectionner les deux services lors
de la première release ; le workflow respecte cet ordre. Les anciens clients
restent compatibles avec le nouvel agent.

1. Fusionner les PR nécessaires sur `main`, dans l’ordre de leur stack.
2. Ouvrir **Actions → Release → Run workflow**, sur `main`.
3. Cocher **Service indépendant de génération de visuels** pour construire et livrer
   l’agent seul, y compris pour les autres applications clientes.
4. Cocher **API et front** pour livrer TheZaurus. Si l’agent est également sélectionné,
   son déploiement précède celui de l’application ; sinon son service doit déjà exister.
5. Cocher **Agent (talks-agent)** uniquement pour son déploiement séparé.
6. Approuver l’environnement `production` si sa protection le demande.

L’agent utilise la version partagée `agents-vX.Y.Z` du dossier `agents/` et l’image
`visuals-agent` dans `GCP_REGISTRY`. L’image est vérifiée par digest avant déploiement :
utilisateur non-root, santé, refus sans clé ou avec une clé invalide, deux clients
indépendants, PNG et limites de participants, sans appel Vertex.

Le workflow du service visuels vérifie après déploiement son accès HTTPS et le
refus d’un rendu sans clé. L’application garde son smoke test front/NextAuth.
Chaque service possède son rollback de trafic indépendant. Un échec de l’application
ne révoque pas le déploiement de l’agent qui sert aussi les autres clients.

## Intégrer une autre application

Récupérer l’URL dans le résumé du déploiement, faire ajouter une clé pour le client,
puis appeler les mêmes endpoints depuis son serveur, quel que soit son hébergeur :

```sh
curl --fail-with-body "$VISUALS_URL/visuals/blogs/image" \
  -H "X-API-Key: $VISUALS_CLIENT_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Mon article","writers":["Alice"],"autoCrop":false}' \
  --output article.png
```

Un appel sans clé ou avec une clé révoquée reçoit 401. Aucune identité Google n’est
nécessaire pour le client. Les données, limites et modèles disponibles sont décrits
dans le [contrat de l’agent](../agents/visuals-agent/README.md#api).

## Vérifier après déploiement

Depuis l’application avec un compte autorisé, télécharger un visuel de talk et
un visuel d’article, dont un cas avec photo autorisée pour vérifier Vertex AI,
puis un cas avec autorisation refusée pour vérifier le cadrage supérieur sans IA,
et un cas sans photo pour vérifier le logo Zenika.
Ce contrôle authentifié n’est pas couvert par le smoke test NextAuth ni par le
test d’image sans IA. Ne pas envoyer de portrait réel sans autorisation photo IA.

Consulter le résumé du workflow (digests et révision précédente) et les logs :

```sh
gcloud run services logs read SERVICE --project PROJECT --region REGION --limit 100
```

L’application conserve son front comme seul ingress et ses réglages OAuth/IAP.
Le service visuels possède son propre ingress HTTPS et exige les clés clientes ;
ne pas exposer directement le backend TheZaurus sur le port 8080.
