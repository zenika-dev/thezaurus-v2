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

### Configuration Base de Données (Firestore)

Pour que l'API puisse se connecter à la base de données Firestore :

1. Allez dans le répertoire `api`.
2. Dupliquez le fichier `.env-template` et renommez-le en `.env`.
3. Renseignez-y l'identifiant de votre projet GCP (`GOOGLE_CLOUD_PROJECT_ID`) — par défaut `thezaurus-dev` est configuré — et le chemin absolu vers votre fichier de clé de compte de service (`GOOGLE_APPLICATION_CREDENTIALS`).

## Déploiement Local (Docker Compose)

Vous pouvez lancer l'ensemble de l'application (UI, API et un émulateur Firestore) localement avec Docker Compose :

1. Assurez-vous d'avoir Docker installé.
2. À la racine du projet, lancez :
   ```bash
   docker-compose up --build
   ```
3. L'application sera disponible aux adresses suivantes :
   - **Frontend** : `http://localhost:3000`
   - **API** : `http://localhost:8080`
   - **Swagger UI** : `http://localhost:8080/q/swagger-ui/`
   - **Firestore Emulator** : `http://localhost:9000` (utilisé automatiquement par l'API)

Made with ❤️ by Zenika
