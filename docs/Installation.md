# Guide d'installation et de développement

Ce guide détaille l'installation, la configuration et le workflow de développement pour **TheZaurus-v2**.

---

## 📋 Prérequis

- [Docker](https://docs.docker.com/get-docker/) et Docker Compose
- [Node.js](https://nodejs.org/) (version 20+ recommandée pour le développement front-end)
- [Java 21+](https://adoptium.net/) et Maven (optionnel, si vous exécutez l'API hors Docker)
- [Google Cloud SDK (`gcloud`)](https://cloud.google.com/sdk/docs/install) (uniquement pour le mode connecté à GCP)

---

## ⚙️ Configuration de l'environnement

Toute la configuration passe par un fichier `.env` **à la racine du projet**, lu automatiquement par Docker Compose (et jamais versionné) :

```bash
cp .env-template .env
```

### Variables d'environnement

| Variable | Requise | Utilisée par | Description |
|---|---|---|---|
| **Authentification (front)** | | | |
| `GOOGLE_CLIENT_ID` | ✅ | front, api | Client OAuth Google — console GCP > *APIs & Services > Credentials > OAuth 2.0 Client IDs*. `http://localhost:3000/api/auth/callback/google` doit être dans les *Authorized redirect URIs*. Sert aussi d'audience JWT à l'API en dev. |
| `GOOGLE_CLIENT_SECRET` | ✅ | front | Secret du client OAuth. Affiché uniquement à sa création (bouton *Add secret* si perdu). |
| `NEXTAUTH_URL` | ✅ | front | URL du front en local : `http://localhost:3000`. |
| `NEXTAUTH_SECRET` | ✅ | front | Signature des sessions NextAuth. À générer : `openssl rand -base64 32`. |
| **Firestore GCP** | | | |
| `GOOGLE_CLOUD_PROJECT_ID` | mode prod | api | Identifiant du projet GCP (ex. `thezaurus-494709`). En mode dev avec émulateur, `local-dev` est utilisé. |
| `FIRESTORE_DATABASE_ID` | mode prod | api | Nom de la base Firestore cible (défaut : `thezaurus-dev`). ⚠️ Ne jamais cibler la base de production en local. |
| `FIRESTORE_COLLECTION_PREFIX` | — | api | Préfixe des collections (ex : `dev` → `dev_talks`). Défaut : `dev`. |
| `GCP_REGION` | déploiement | api, front | Région GCP de déploiement Cloud Run (défaut : `europe-west1`). |
| `GCLOUD_ADC` | Windows (mode prod) | Docker Compose | Chemin du fichier *Application Default Credentials* monté dans le conteneur API. Automatique sur Linux/Mac (`~/.config/gcloud/...`) ; sous Windows : `C:/Users/<vous>/AppData/Roaming/gcloud/application_default_credentials.json`. |
| **Bot Slack** (optionnel) | | | |
| `SLACK_BOT_TOKEN` | — | api | Bot User OAuth Token (`xoxb-...`). Absent = bot désactivé. Voir [SlackBot.md](./SlackBot.md). |
| `SLACK_SIGNING_SECRET` | — | api | Vérification de l'origine des requêtes Slack. |
| `SLACK_APP_TOKEN` | — | api | Token app-level (`xapp-...`), si utilisé. |
| **Agent Talk / Reasoning Engine** | | | |
| `REASONING_ENGINE_URL` | — | api | URL du Reasoning Engine Vertex AI pour l'agent IA. |
| **Déploiement Cloud Run** (`docker-compose.cloud.yml` uniquement) | | | |
| `NEXTAUTH_PUBLIC_URL` | déploiement | front | URL publique du front déployé, utilisée comme `NEXTAUTH_URL` en prod. À ajouter aux *Authorized redirect URIs* du client OAuth. |
| `GOOGLE_IAP_AUDIENCE` | déploiement | api | Audience du JWT IAP vérifiée par l'API en prod. |

---

## 🚀 Lancement de l'environnement (Docker Compose)

### 1. Mode Dev (Émulateur Firestore — Par défaut)

Par défaut, `docker compose up` combine `docker-compose.yml` et `docker-compose.override.yml`. Cela démarre :
- L'émulateur Firestore local (port 9000, données en mémoire).
- L'API Quarkus en profil `dev` pointant sur l'émulateur.
- Le frontend Next.js en mode développement avec rechargement à chaud (*watch*).

```bash
docker compose up --build
```

### 2. Mode Prod (Vrai Firestore GCP & Front optimisé)

Pour exécuter la stack en conditions de production (connexion directe à la base Firestore sur GCP et frontend compilé en mode standalone/runner), lancez `docker-compose.yml` seul en excluant l'override :

```bash
docker compose -f docker-compose.yml up --build
```

#### Prérequis pour le mode prod :
1. Générer les identifiants Google (*Application Default Credentials*) :
   ```bash
   gcloud auth application-default login
   ```
2. Vérifier que `GOOGLE_CLOUD_PROJECT_ID` et `FIRESTORE_DATABASE_ID` (ex. `thezaurus-dev`) sont renseignés dans votre `.env`.
3. *(Sous Windows)* Renseigner `GCLOUD_ADC` dans le `.env` si le chemin des identifiants diffère.

---

## 🌐 URLs et Services locaux

Une fois la stack démarrée, les services suivants sont accessibles :

| Service | URL | Description |
|---|---|---|
| **Frontend** | [http://localhost:3000](http://localhost:3000) | Interface utilisateur Next.js |
| **API Backend** | [http://localhost:8080](http://localhost:8080) | API REST Quarkus |
| **Swagger UI** | [http://localhost:8080/q/swagger-ui/](http://localhost:8080/q/swagger-ui/) | Documentation interactive OpenAPI |
| **Emulator UI (Firebase)** | [http://localhost:4000/firestore/local-dev/data](http://localhost:4000/firestore/local-dev/data) | Console visuelle Firestore (mode dev uniquement) |
| **Émulateur Firestore** | `http://localhost:9000` | Port gRPC / REST de l'émulateur (mode dev uniquement) |

### Inspection des données de l'émulateur (mode dev)

En plus de l'Emulator UI sur le port 4000, l'API REST de l'émulateur permet d'interroger directement les données :

```bash
curl "http://localhost:9000/v1/projects/local-dev/databases/(default)/documents/dev_talks"
```

---

## 🔄 Contrat OpenAPI et génération des types TypeScript

Les types TypeScript du front décrivant les données de l'API sont **générés automatiquement** depuis le contrat OpenAPI produit par Quarkus.

```
Resources JAX-RS ──(build Maven)──> api/openapi.json ──(openapi-typescript)───> front/shared/api/schema.d.ts
                                                     ├─(script maison)────────> front/shared/api/enums.ts
                                                     └─(script maison)────────> front/shared/api/contract.ts
```

- `schema.d.ts` : types bruts générés.
- `enums.ts` : enums runtime sous forme de tableaux (listes déroulantes, validations Zod).
- `contract.ts` : alias de types lisibles (`BackendBlogPost`, `BackendTalk`, `BackendConference`, …).

Les fichiers intermédiaires sont versionnés afin d'assurer l'indépendance des jobs CI et la traçabilité dans les PRs.

### Procédure de mise à jour du contrat :

Après toute modification d'une ressource REST ou d'un modèle côté `api` :

1. Régénérer la spécification OpenAPI côté backend :
   ```bash
   cd api && ./mvnw package -DskipTests && cp target/openapi/openapi.json openapi.json
   ```
2. Régénérer les types TypeScript côté frontend :
   ```bash
   cd front && npm run generate:api
   ```

---

## 🎨 Formatage du code (Module `api`)

Le style Java est imposé par [Spotless](https://github.com/diffplug/spotless) avec [palantir-java-format](https://github.com/palantir/palantir-java-format) (indentation 4 espaces, 120 colonnes, tri et nettoyage des imports).

`spotless:check` est exécuté lors de la phase `validate` : **le build Maven échouera si un fichier est mal formaté**.

- **Appliquer le formatage automatique** :
  ```bash
  cd api && ./mvnw spotless:apply
  ```
- **Vérifier le formatage** :
  ```bash
  cd api && ./mvnw spotless:check
  ```
- **Ignorer le commit de reformatage initial dans `git blame`** :
  ```bash
  git config blame.ignoreRevsFile .git-blame-ignore-revs
  ```

---

## 🛠️ Dépannage et pièges connus

- **Erreur `Error reading credential file ... /tmp/credentials.json: File does not exist` (mode prod)** :  
  Le fichier ADC n'existait pas lors du premier lancement et Docker a monté un dossier vide à la place. Exécutez `gcloud auth application-default login`, supprimez le dossier vide créé sous ce nom si nécessaire, puis recréez le conteneur :
  ```bash
  docker compose up -d --force-recreate api
  ```
- **Erreur `client_secret_basic client authentication method requires a client_secret`** :  
  Vérifiez que la variable `GOOGLE_CLIENT_SECRET` est correctement renseignée dans votre `.env`.
- **Mode Quarkus Dev hors Docker** :  
  Lancer `./mvnw quarkus:dev` dans le dossier `api/` utilisera automatiquement l'émulateur sur `localhost:9000` (cf. `%dev.quarkus.google.cloud.firestore.host-override` dans `api/src/main/resources/application.properties`).
