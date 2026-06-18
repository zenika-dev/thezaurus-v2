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

Made with ❤️ by Zenika
