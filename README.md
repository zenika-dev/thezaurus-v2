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

## Contributions 

### Configuration de Firestore (Émulateur vs Cloud GCP)

La configuration se fait via le fichier `.env` situé à la racine du projet (pour Docker Compose) ou dans le dossier `api/` (pour le mode Quarkus Dev local).

#### Mode Émulateur (par défaut) vs GCP 
Le composant Back end est connecté à un émulateur GCP pour éviter de solliciter GCP (et engendrer des couts).
Par défault, les données sont stockées localement en mémoire vive et est paramétré dans le fichier `.env` :
  ```properties
  QUARKUS_PROFILE=dev
  FIRESTORE_EMULATOR_HOST=firestore:9000  # ou localhost:9000 en mode Quarkus Dev local
  ```

S'il y a besoin d'avoir plus de données et/ou vérifier le bon fonctionnement de la connexion avec GCP, il est possible, à partir du fichier `.env` de vous brancher sur l'instance Firestore.
⚠️ Actuellement il existe une instance de dév et une de prod. Merci de ne pas utiliser la base de prod.

* Paramètres à définir dans `.env` :
  ```properties
  QUARKUS_PROFILE=prod
  FIRESTORE_EMULATOR_HOST=               # Laissez vide ou commentez cette ligne
  GOOGLE_CLOUD_PROJECT_ID=votre-projet-gcp
  FIRESTORE_DATABASE_ID=votre-base-id
  GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/votre/cle-service-account.json
  ```

### Configuration du bot Slack (`/talk`, `/ping`)

L'API expose un bot Slack (commandes slash `/talk` et `/ping`) via le SDK [Bolt for Java](https://github.com/slackapi/java-slack-sdk). Cette intégration est **optionnelle** : si les variables ci-dessous ne sont pas renseignées, l'application démarre normalement mais le bot Slack reste désactivé (aucune commande n'est enregistrée, aucun appel n'est fait à l'API Slack).

#### Variables d'environnement

À définir dans le fichier `.env` (racine du projet pour Docker Compose, ou `api/` en mode Quarkus Dev local) :

```properties
SLACK_BOT_TOKEN=xoxb-...
SLACK_SIGNING_SECRET=...
```

| Variable | Description | Où la récupérer dans Slack |
|---|---|---|
| `SLACK_BOT_TOKEN` | Bot User OAuth Token utilisé pour appeler l'API Slack | **OAuth & Permissions** > `Bot User OAuth Token` |
| `SLACK_SIGNING_SECRET` | Secret utilisé pour vérifier que les requêtes reçues proviennent bien de Slack | **Basic Information** > `App Credentials` > `Signing Secret` |

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

- `/ping` : commande de test, répond « Pong ! 🏓 »
- `/talk` : ouvre une modale permettant de créer un talk (titre, speakers, agence, description, statut, visibilité, conférence, date)

## Déploiement Local (Docker Compose)

Vous pouvez lancer l'ensemble de l'application (UI, API et l'émulateur Firestore local) avec Docker Compose :

1. Assurez-vous d'avoir Docker installé et configuré.
2. Créez ou modifiez le fichier `.env` à la racine du projet en vous basant sur la configuration ci-dessus.
3. Lancez :
   ```bash
   docker-compose up --build
   ```
4. L'application sera disponible aux adresses suivantes :
   - **Frontend** : `http://localhost:3000`
   - **API** : `http://localhost:8080`
   - **Swagger UI** : `http://localhost:8080/q/swagger-ui/`
   - **Firestore Emulator** : `http://localhost:9000` (utilisé uniquement si `QUARKUS_PROFILE=dev` et `FIRESTORE_EMULATOR_HOST` est défini)

### Utilisation et vérification de l'émulateur Firestore

L'image `google/cloud-sdk:emulators` démarre un émulateur Firestore local. 

* **Fonctionnement** : Il simule localement l'API Firestore en mémoire vive (RAM). Les données sont réinitialisées à chaque arrêt des conteneurs via `docker compose down`.
* **Redirection automatique** : L'API Quarkus détecte la variable d'environnement `FIRESTORE_EMULATOR_HOST=firestore:9000` et redirige automatiquement tous les appels vers le conteneur Firestore local au lieu de la production.
* **Vérification des données** : Pour inspecter les documents stockés par l'application dans l'émulateur, vous pouvez effectuer des requêtes HTTP GET directement sur l'API REST de l'émulateur :
  
  ```bash
  # Lister les articles de blog
  curl -X GET "http://localhost:9000/v1/projects/thezaurus-dev/databases/thezaurus-dev/documents/blog_posts"

  # Lister les conférences
  curl -X GET "http://localhost:9000/v1/projects/thezaurus-dev/databases/thezaurus-dev/documents/conferences"

  # Lister les talks
  curl -X GET "http://localhost:9000/v1/projects/thezaurus-dev/databases/thezaurus-dev/documents/talks"
  ```

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

Made with ❤️ by Zenika
