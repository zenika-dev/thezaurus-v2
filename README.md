# TheZaurus-v2

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-4695EB?style=for-the-badge&logo=quarkus&logoColor=white)
![Firestore](https://img.shields.io/badge/Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![Next.js](https://img.shields.io/badge/next.js-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![MUI](https://img.shields.io/badge/MUI-%230081CB.svg?style=for-the-badge&logo=mui&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)

Ce projet est la refonte moderne de l'application **TheZaurus** : [zenika-open-source/thezaurus](https://github.com/zenika-open-source/thezaurus).

TheZaurus permet la gestion et le suivi des partages de connaissances chez Zenika : talks, articles de blog et participations à des conférences.

---

## 🏛️ Architecture du projet

Le projet s'articule autour des composants suivants :

- **Frontend (`front/`)** : Application React / Next.js (App Router), stylisée avec TailwindCSS & Material UI (MUI), typée avec TypeScript et sécurisée avec NextAuth.
- **Backend API (`api/`)** : API REST développée avec Java 21 et Quarkus, exposant les ressources et intégrant le SDK Bolt pour Slack.
- **Base de données** : Google Cloud Firestore (avec émulateur local Firebase pour le développement).

```
thezaurus-v2/
├── api/             # Backend Java Quarkus (JAX-RS, Firestore, Slack Bot)
├── front/           # Frontend Next.js / React / TypeScript
├── docs/            # Documentation détaillée du projet
├── docker-compose.* # Orchestration des conteneurs locaux et Cloud
```

---

## 🔌 API & Endpoints

L'API Quarkus gère les entités principales dans Firestore :

- **Talks** (`/talks`) : CRUD des présentations et talks
- **Blog Posts** (`/blog-posts`) : CRUD des articles de blog
- **Conferences** (`/conferences`) : CRUD des conférences
- **Users** (`/users`) : Gestion des utilisateurs et des rôles
- **Status** (`/status`) : Statut simple de l'application
- **Health Checks** (`/q/health`) : Métriques de santé Quarkus (Liveness / Readiness)
- **OpenAPI & Swagger UI** (`/q/openapi`, `/q/swagger-ui`) : Documentation interactive de l'API

---

## ⚡ Démarrage rapide

1. **Configurer l'environnement :**
   ```bash
   cp .env-template .env
   ```
   *(Renseignez vos identifiants OAuth Google `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET` dans le `.env`)*

## Configuration

Toute la configuration passe par un fichier `.env` **à la racine du projet**, lu automatiquement par Docker Compose (et jamais versionné) :

```bash
cp .env-template .env
```

### Variables d'environnement

| Variable | Requise | Utilisée par | Description |
|---|---|---|---|
| **Mode de fonctionnement** | | | |
| Mode de lancement | — | Docker Compose | `docker compose up` (défaut) : charge automatiquement `docker-compose.override.yml` → mode **dev** avec émulateur Firestore local. `docker compose -f docker-compose.yml up` (override exclu explicitement) : mode **prod**, connexion au vrai Firestore GCP. |
| **Authentification (front)** | | | |
| `GOOGLE_CLIENT_ID` | ✅ | front, api | Client OAuth Google — console GCP > *APIs & Services > Credentials > OAuth 2.0 Client IDs*. `http://localhost:3000/api/auth/callback/google` doit être dans les *Authorized redirect URIs*. Sert aussi d'audience JWT à l'API en dev. |
| `GOOGLE_CLIENT_SECRET` | ✅ | front | Secret du client OAuth. Affiché uniquement à sa création (bouton *Add secret* si perdu). |
| `NEXTAUTH_URL` | ✅ | front | URL du front en local : `http://localhost:3000`. |
| `NEXTAUTH_SECRET` | ✅ | front | Signature des sessions NextAuth. À générer : `openssl rand -base64 32`. |
| **Firestore GCP** (mode `prod` uniquement) | | | |
| `GOOGLE_CLOUD_PROJECT_ID` | mode prod | api | Projet GCP cible. Défaut du compose : `thezaurus-494709` (projet de l'équipe). |
| `FIRESTORE_DATABASE_ID` | mode prod | api | Base Firestore. Défaut : `thezaurus-dev`. ⚠️ Ne jamais pointer `thezaurus-prod` en local. |
| `FIRESTORE_COLLECTION_PREFIX` | — | api | Préfixe des collections (ex : `dev` → `dev_talks`). Défaut : `dev`. |
| `GCP_REGION` | — | Infra GCP | Région GCP par défaut (ex : `europe-west1`). |
| `GCLOUD_ADC` | Windows, mode prod | Docker Compose | Chemin du fichier *Application Default Credentials* monté dans le conteneur API. Inutile sur Linux/Mac (défaut : `~/.config/gcloud/...`) ; sous Windows : `C:/Users/<vous>/AppData/Roaming/gcloud/application_default_credentials.json`. |
| **Bot Slack** (optionnel — voir la section dédiée) | | | |
| `SLACK_BOT_TOKEN` | — | api | Bot User OAuth Token (`xoxb-...`). Absent = bot désactivé. |
| `SLACK_SIGNING_SECRET` | — | api | Vérification de l'origine des requêtes Slack. |
| `SLACK_APP_TOKEN` | — | api | Token app-level (`xapp-...`), si utilisé. |
| **SMTP & Rappels post-talk (`api`)** | | | |
| `FEEDBACK_REMINDER_ENABLED` | — | api | Active le job quotidien d'envoi de rappels (`true`/`false`, défaut: `false`). |
| `FEEDBACK_REMINDER_CRON` | — | api | Expression cron pour les rappels (défaut : `0 0 9 * * ?` à 09:00). |
| `FEEDBACK_REMINDER_TIME_ZONE` | — | api | Fuseau horaire du cron (défaut : `Europe/Paris`). |
| `THEZAURUS_PUBLIC_URL` | — | api | URL publique contractuelle utilisée pour composer les liens dans les emails de rappel. Si omise, repli sur `NEXTAUTH_PUBLIC_URL` puis `NEXTAUTH_URL`. |
| `SMTP_HOST` | si rappels actifs | api | Hôte du serveur SMTP (ex: `mailpit` en dev, `smtp.sendgrid.net` en prod). |
| `SMTP_PORT` | si rappels actifs | api | Port SMTP (ex: `1025` pour Mailpit, `587` pour STARTTLS). |
| `SMTP_FROM` | si rappels actifs | api | Adresse email d'expédition (ex: `thezaurus@zenika.com`). |
| `SMTP_USERNAME` | — | api | Utilisateur SMTP pour l'authentification. |
| `SMTP_PASSWORD` | — | api | Mot de passe / token SMTP. |
| `SMTP_LOGIN` | — | api | Mode de login SMTP (`NONE`, `REQUIRED`, `OPTIONAL`). |
| `SMTP_START_TLS` | — | api | STARTTLS (`DISABLED`, `REQUIRED`, `OPTIONAL`). |
| `SMTP_TLS` | — | api | TLS direct (`true`/`false`). |
| `SMTP_MOCK` | — | api | `false` = envoi réel via SMTP ; `true` = simulation en mémoire (défaut hors Compose : `true` en dev). |
| **Agent / Reasoning Engine** | | | |
| `REASONING_ENGINE_URL` | — | api | URL du Reasoning Engine pour l'agent talk. |
| **Déploiement Cloud Run** (`docker-compose.cloud.yml` uniquement) | | | |
| `NEXTAUTH_PUBLIC_URL` | déploiement | front | URL publique du front déployé, utilisée comme `NEXTAUTH_URL` en prod. À ajouter aux *Authorized redirect URIs* du client OAuth. |
| `GOOGLE_IAP_AUDIENCE` | déploiement | api | Audience du JWT IAP vérifiée par l'API en prod. ⚠️ Non câblée à ce jour — à valider avec la personne qui gère le déploiement. |

### Mode dev (émulateur — défaut)

Rien à configurer : `docker compose up` démarre un émulateur Firestore local avec la stack (port 9000, données en RAM, réinitialisées à chaque `docker compose down`). Aucun credential GCP requis — seules les variables d'authentification du front sont à renseigner.

### Mode prod (vrai Firestore GCP)

Lancez `docker-compose.yml` seul, en excluant explicitement l'override dev :

```bash
docker compose -f docker-compose.yml up --build
```

Sans `docker-compose.override.yml`, l'émulateur Firestore n'existe plus dans la stack et l'API tourne en profil `prod` : elle se connecte au vrai Firestore. Il faut alors des *Application Default Credentials* : installez la [gcloud CLI](https://cloud.google.com/sdk/docs/install) puis :

```bash
gcloud auth application-default login
```

⚠️ **Sous Windows**, gcloud écrit ce fichier dans `%APPDATA%\gcloud\`, pas dans `~/.config/gcloud` : renseignez `GCLOUD_ADC` (voir tableau). Après un changement de mode, relancez avec `docker compose up -d --force-recreate --remove-orphans`.

### Configuration du bot Slack (`/talk`)

L'API expose un bot Slack (commandes slash `/talk`) via le SDK [Bolt for Java](https://github.com/slackapi/java-slack-sdk). Cette intégration est **optionnelle** : si les variables ci-dessous ne sont pas renseignées, l'application démarre normalement mais le bot Slack reste désactivé (aucune commande n'est enregistrée, aucun appel n'est fait à l'API Slack).

Les variables `SLACK_BOT_TOKEN` et `SLACK_SIGNING_SECRET` sont décrites dans le [tableau des variables d'environnement](#variables-denvironnement) ; les étapes 5 et 6 ci-dessous indiquent où les récupérer dans Slack.

#### Créer l'application Slack à partir du manifest

Le fichier [`api/src/main/resources/manifest.yaml`](api/src/main/resources/manifest.yaml) décrit entièrement la configuration de l'application Slack (nom du bot, commandes slash, interactivité, scopes OAuth...). Il permet de créer l'app Slack en une fois plutôt que de configurer chaque écran manuellement :

1. Rendez-vous sur https://api.slack.com/apps puis cliquez sur **Create New App**.
2. Choisissez **From a manifest** et sélectionnez le workspace Slack sur lequel vous voulez installer l'app (idéalement un workspace de dev/test).
3. Collez le contenu de `manifest.yaml` (onglet **YAML**), puis remplacez chaque occurrence de `https://your-url.zenika.com` par l'URL publique de votre API (URL de déploiement, ou URL ngrok en local — voir ci-dessous).
4. Validez la création (**Create**), puis vérifiez le résumé (**Review summary & create app**).
5. Dans **OAuth & Permissions**, cliquez sur **Install to Workspace** et autorisez l'app, puis copiez le **Bot User OAuth Token** (commence par `xoxb-`) dans `SLACK_BOT_TOKEN`.
6. Dans **Basic Information > App Credentials**, copiez le **Signing Secret** dans `SLACK_SIGNING_SECRET`.
7. Renseignez ces deux valeurs dans votre `.env`, puis (re)démarrez l'application.

#### Tester en local avec ngrok

Slack doit pouvoir atteindre votre API sur une URL HTTPS publique pour délivrer les commandes slash sur `/slack/events`. En local, vous pouvez exposer votre API avec [ngrok](https://ngrok.com/) :

```bash
ngrok http 8080
```

Utilisez ensuite l'URL HTTPS fournie par ngrok (ex : `https://xxxx.ngrok-free.app/slack/events`) comme `url` des commandes slash et comme `request_url` d'interactivité dans le manifest de l'app Slack.

⚠️ L'URL ngrok change à chaque redémarrage (sauf domaine réservé) : il faut alors mettre à jour la configuration de l'app Slack (Slash Commands + Interactivity) avec la nouvelle URL.

#### Commandes disponibles

- `/talk` : ouvre une modale permettant de créer un talk (titre, speakers, agence, description, statut, visibilité, conférence, date)

## Déploiement Local (Docker Compose)

1. Assurez-vous d'avoir Docker installé et le `.env` configuré (section précédente).
2. Lancez :
   ```bash
   docker compose up --build
   ```

3. **Accéder aux services :**
   - **Frontend** : [http://localhost:3000](http://localhost:3000)
   - **API Backend** : [http://localhost:8080](http://localhost:8080)
   - **Swagger UI** : [http://localhost:8080/q/swagger-ui/](http://localhost:8080/q/swagger-ui/)
   - **Firebase Emulator UI** : [http://localhost:4000/firestore/local-dev/data](http://localhost:4000/firestore/local-dev/data)

---

## 📚 Documentation détaillée

Pour approfondir, consultez les guides disponibles dans le dossier [`docs/`](docs/) :

- 📖 **[Guide d'installation et de développement](docs/Installation.md)** : 
  - Configuration et variables d'environnement (`.env`)
  - Lancement local avec Docker Compose et émulateur Firestore
  - Synchronisation du contrat OpenAPI et génération automatique des types TypeScript
  - Règles de formatage du code Java (Spotless)
  - Dépannage et pièges fréquents
- 🤖 **[Guide du Bot Slack](docs/SlackBot.md)** :
  - Configuration de l'application Slack depuis le manifest
  - Exposition locale avec ngrok
  - Commandes slash (`/talk`)
- 🚢 **[Guide de déploiement Cloud Run](docs/Deploiement.md)** :
  - Architecture multi-conteneurs Cloud Run (Ingress & Backend)
  - Prérequis et variables d'environnement de production
  - Commandes de déploiement et consultation des logs

---

## 🚢 Déploiement

Le déploiement en production s'effectue sur **Google Cloud Run** via `gcloud run compose`. Consultez le [Guide de déploiement détaillé](docs/Deploiement.md) pour les étapes complètes.

---

## Rappels après un talk

Le job quotidien utilise le modèle de message administrable. En Docker Compose
local, le `.env` configure l’envoi vers Mailpit, consultable sur http://localhost:8025.
Hors de cette configuration locale, le job reste désactivé par défaut.
Voir le [guide des rappels](docs/FeedbackReminders.md) pour la configuration et les conditions d’exécution.

Made with ❤️ by Zenika
