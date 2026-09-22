# Guide d'installation et de développement

Ce guide détaille l'installation, la configuration et le workflow de développement pour **TheZaurus-v2**.

---

## 📋 Prérequis

- [Docker](https://docs.docker.com/get-docker/) et Docker Compose
- [Node.js](https://nodejs.org/) (version 20+ recommandée pour le développement front-end)
- [Java 21+](https://adoptium.net/) et Maven (optionnel, si vous exécutez l'API hors Docker)

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
| **Firestore** | | | |
| `GOOGLE_CLOUD_PROJECT_ID` | — | api | Identifiant du projet GCP (ex. `thezaurus-494709`). En local, l'émulateur utilise `local-dev`. |
| `FIRESTORE_DATABASE_ID` | — | api | Nom de la base Firestore (défaut : `thezaurus-dev`). |
| `FIRESTORE_COLLECTION_PREFIX` | — | api | Préfixe des collections (ex : `dev` → `dev_talks`). Défaut : `dev`. |
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

## 🚀 Lancement de l'environnement local (Docker Compose)

En développement local, `docker compose up` démarre l'ensemble de la stack avec un émulateur Firestore local (port 9000, données en mémoire). Aucun identifiant GCP n'est requis : seules les variables d'authentification OAuth Google du front sont nécessaires.

```bash
docker compose up --build
```

---

## 🌐 URLs et Services locaux

Une fois la stack démarrée, les services suivants sont accessibles :

| Service | URL | Description |
|---|---|---|
| **Frontend** | [http://localhost:3000](http://localhost:3000) | Interface utilisateur Next.js |
| **API Backend** | [http://localhost:8080](http://localhost:8080) | API REST Quarkus |
| **Swagger UI** | [http://localhost:8080/q/swagger-ui/](http://localhost:8080/q/swagger-ui/) | Documentation interactive OpenAPI |
| **Emulator UI (Firebase)** | [http://localhost:4000/firestore/local-dev/data](http://localhost:4000/firestore/local-dev/data) | Console visuelle Firestore (parcours des données) |
| **Émulateur Firestore** | `http://localhost:9000` | Port gRPC / REST de l'émulateur |

### Inspection des données de l'émulateur

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

- **Erreur `client_secret_basic client authentication method requires a client_secret`** :  
  Vérifiez que la variable `GOOGLE_CLIENT_SECRET` est correctement renseignée dans votre `.env`.
- **Mode Quarkus Dev hors Docker** :  
  Lancer `./mvnw quarkus:dev` dans le dossier `api/` utilisera automatiquement l'émulateur sur `localhost:9000` (cf. `%dev.quarkus.google.cloud.firestore.host-override` dans `api/src/main/resources/application.properties`).
