# TheZaurus-v2

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-4695EB?style=for-the-badge&logo=quarkus&logoColor=white)
![Firestore](https://img.shields.io/badge/Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![MUI](https://img.shields.io/badge/MUI-%230081CB.svg?style=for-the-badge&logo=mui&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/vite-%23646CFF.svg?style=for-the-badge&logo=vite&logoColor=white)

Ce projet est une refonte du projet TheZaurus déjà existant dans un autre projet GitHub : https://github.com/zenika-open-source/thezaurus

## Architecture 

Le projet est pour le moment constitué :

- d'une interface en React 
- d'un composant back end Java Quarkus
- d'une base de données Firestore

## Front end 


### API Endpoints

Une API Quarkus est disponible dans le dossier `api` pour gérer les entités suivantes dans Firestore :

- **Talks** (`/talks`) : `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`
- **Blog Posts** (`/blog-posts`) : `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`
- **Conferences** (`/conferences`) : `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`
- **Status** (`/status`) : `GET` (Statut simple de l'application)
- **Health Checks** (`/q/health`) : Points de terminaison standards Quarkus (Liveness/Readiness)

## Configuration

Toute la configuration passe par un fichier `.env` **à la racine du projet**, lu automatiquement par Docker Compose (et jamais versionné) :

```bash
cp .env-template .env
```

### Variables d'environnement

| Variable | Requise | Utilisée par | Description |
|---|---|---|---|
| **Mode de fonctionnement** | | | |
| `COMPOSE_FILE` | — | Docker Compose | Absente (défaut) : mode **dev** avec émulateur Firestore local. Pour le mode **prod** (vrai GCP) : `docker-compose.yml:docker-compose.prod.yml`, accompagnée de `COMPOSE_PATH_SEPARATOR=:` (indispensable sous Windows, sans effet ailleurs). |
| **Authentification (front)** | | | |
| `GOOGLE_CLIENT_ID` | ✅ | front, api | Client OAuth Google — console GCP > *APIs & Services > Credentials > OAuth 2.0 Client IDs*. `http://localhost:3000/api/auth/callback/google` doit être dans les *Authorized redirect URIs*. Sert aussi d'audience JWT à l'API en dev. |
| `GOOGLE_CLIENT_SECRET` | ✅ | front | Secret du client OAuth. Affiché uniquement à sa création (bouton *Add secret* si perdu). |
| `NEXTAUTH_URL` | ✅ | front | URL du front en local : `http://localhost:3000`. |
| `NEXTAUTH_SECRET` | ✅ | front | Signature des sessions NextAuth. À générer : `openssl rand -base64 32`. |
| **Firestore GCP** (mode `prod` uniquement) | | | |
| `GOOGLE_CLOUD_PROJECT_ID` | mode prod | api | Projet GCP cible. Défaut du compose : `thezaurus-494709` (projet de l'équipe). |
| `FIRESTORE_DATABASE_ID` | mode prod | api | Base Firestore. Défaut : `thezaurus-dev`. ⚠️ Ne jamais pointer `thezaurus-prod` en local. |
| `FIRESTORE_COLLECTION_PREFIX` | — | api | Préfixe des collections (ex : `dev` → `dev_talks`). Défaut : `dev`. |
| `GCLOUD_ADC` | Windows, mode prod | Docker Compose | Chemin du fichier *Application Default Credentials* monté dans le conteneur API. Inutile sur Linux/Mac (défaut : `~/.config/gcloud/...`) ; sous Windows : `C:/Users/<vous>/AppData/Roaming/gcloud/application_default_credentials.json`. |
| **Bot Slack** (optionnel — voir la section dédiée) | | | |
| `SLACK_BOT_TOKEN` | — | api | Bot User OAuth Token (`xoxb-...`). Absent = bot désactivé. |
| `SLACK_SIGNING_SECRET` | — | api | Vérification de l'origine des requêtes Slack. |
| `SLACK_APP_TOKEN` | — | api | Token app-level (`xapp-...`), si utilisé. |
| **Déploiement Cloud Run** (`docker-compose.cloud.yml` uniquement) | | | |
| `NEXTAUTH_PUBLIC_URL` | déploiement | front | URL publique du front déployé, utilisée comme `NEXTAUTH_URL` en prod. À ajouter aux *Authorized redirect URIs* du client OAuth. |
| `GOOGLE_IAP_AUDIENCE` | déploiement | api | Audience du JWT IAP vérifiée par l'API en prod. ⚠️ Non câblée à ce jour — à valider avec la personne qui gère le déploiement. |

### Mode dev (émulateur — défaut)

Rien à configurer : `docker compose up` démarre un émulateur Firestore local avec la stack (port 9000, données en RAM, réinitialisées à chaque `docker compose down`). Aucun credential GCP requis — seules les variables d'authentification du front sont à renseigner.

### Mode prod (vrai Firestore GCP)

Décommentez dans votre `.env` :

```properties
COMPOSE_PATH_SEPARATOR=:
COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
```

L'override [docker-compose.prod.yml](docker-compose.prod.yml) désactive l'émulateur et passe l'API en profil `prod` : elle se connecte au vrai Firestore. Il faut alors des *Application Default Credentials* : installez la [gcloud CLI](https://cloud.google.com/sdk/docs/install) puis :

```bash
gcloud auth application-default login
```

⚠️ **Sous Windows**, gcloud écrit ce fichier dans `%APPDATA%\gcloud\`, pas dans `~/.config/gcloud` : renseignez `GCLOUD_ADC` (voir tableau). Après un changement de mode, relancez avec `docker compose up -d --force-recreate --remove-orphans`.

### Configuration du bot Slack (`/talk``)

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
3. L'application sera disponible aux adresses suivantes :
   - **Frontend** : `http://localhost:3000`
   - **API** : `http://localhost:8080`
   - **Swagger UI** : `http://localhost:8080/q/swagger-ui/`
   - **Émulateur Firestore** : `http://localhost:9000` (mode `dev` uniquement)

### Vérifier les données de l'émulateur (mode dev)

L'API REST de l'émulateur permet d'inspecter les documents directement :

```bash
curl "http://localhost:9000/v1/projects/local-dev/databases/(default)/documents/dev_talks"
```

(même principe pour `dev_blog_posts` et `dev_conferences` — le préfixe vient de `FIRESTORE_COLLECTION_PREFIX`)

> **Pièges connus**
> - Sous Windows, `COMPOSE_FILE` avec plusieurs fichiers exige `COMPOSE_PATH_SEPARATOR=:` (le séparateur par défaut y est `;`, pas `:`). Décommentez toujours les deux lignes ensemble.
> - En mode `prod`, si l'API loggue `Error reading credential file ... /tmp/credentials.json: File does not exist` : le fichier ADC n'existait pas quand le conteneur a été créé, et Docker a monté un dossier vide à la place. Vérifiez que `gcloud auth application-default login` a bien créé le fichier (et supprimez un éventuel **dossier** `application_default_credentials.json` créé par Docker à cet emplacement), puis recréez le conteneur : `docker compose up -d --force-recreate api`.
> - Si le front loggue `client_secret_basic client authentication method requires a client_secret` : `GOOGLE_CLIENT_SECRET` manque dans votre `.env`.

> **Note** : le mode Quarkus Dev hors Docker (`./mvnw quarkus:dev` dans `api/`) utilise l'émulateur sur `localhost:9000`, voir `api/.env-template`.

## Deploiement

Le déploiement est pour le moment manuel. Il faut s'assurer de :

- 1) Définir les variables d'environnement du projet
  ```bash
  export $(grep -v '^#' .env | xargs)
  ```
- 2) Lancer la commande
  ```bash
  gcloud run compose up docker-compose.cloud.yml --allow-unauthenticated
  ```

⚠️ Le fichier `.env` est celui de votre configuration locale. Donc avant de déployer, merci de bien respecter les variables d'environnement présentes dans ce fichier, notamment :

  ```bash
  FIRESTORE_DATABASE_ID=thezaurus-prod
  FIRESTORE_COLLECTION_PREFIX=prod
  ```

## Formatage du code (module `api`)

Le style Java est imposé par [Spotless](https://github.com/diffplug/spotless) avec le formateur
[palantir-java-format](https://github.com/palantir/palantir-java-format) : indentation de 4 espaces, 120 colonnes,
imports triés et imports inutilisés supprimés. Aucun réglage d'IDE n'est nécessaire, et les réglages personnels ne
font plus foi.

`spotless:check` est branché sur la phase `validate` : **tout build du module `api` échoue si un fichier est mal
formaté**. Pour reformater les sources :

```bash
cd api && ./mvnw spotless:apply
```

Pour vérifier sans rien modifier :

```bash
cd api && ./mvnw spotless:check
```

Le commit de reformatage initial est listé dans `.git-blame-ignore-revs`. Pour que `git blame` l'ignore :

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

Made with ❤️ by Zenika
