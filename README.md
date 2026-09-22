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

2. **Démarrer l'application avec Docker Compose :**
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
  - Configuration exhaustive des variables d'environnement (`.env`)
  - Modes de fonctionnement (Émulateur Firestore local vs Firestore GCP)
  - Synchronisation du contrat OpenAPI et génération automatique des types TypeScript
  - Règles de formatage du code Java (Spotless)
  - Dépannage et pièges fréquents
- 🤖 **[Guide du Bot Slack](docs/SlackBot.md)** :
  - Configuration de l'application Slack depuis le manifest
  - Exposition locale avec ngrok
  - Commandes slash (`/talk`)

---

## 🚢 Déploiement

Le déploiement est actuellement effectué sur **Google Cloud Run** :

1. Définir les variables d'environnement de production :
   ```bash
   export $(grep -v '^#' .env | xargs)
   ```
2. Lancer le déploiement Cloud Run :
   ```bash
   gcloud run compose up docker-compose.cloud.yml --allow-unauthenticated
   ```

> ⚠️ Avant de déployer, assurez-vous de cibler les variables de production (ex. `FIRESTORE_DATABASE_ID=thezaurus-prod` et `FIRESTORE_COLLECTION_PREFIX=prod`).

---

Made with ❤️ by Zenika
