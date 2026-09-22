# Déploiement sur Google Cloud Run

Ce document détaille la procédure de déploiement de **TheZaurus-v2** sur **Google Cloud Run**.

---

## 🏛️ Architecture Cloud Run

TheZaurus-v2 s'exécute sur Cloud Run selon une architecture **multi-conteneurs (sidecar)** définie dans `docker-compose.yml` :

- **`front` (Conteneur Ingress)** : Frontend Next.js (port 3000) exposé publiquement (`x-google-cloudrun: ingress-container: true`), qui sert l'application et redirige les appels API vers le conteneur backend via `http://localhost:8080` / `http://api:8080`.
- **`api` (Conteneur Backend)** : API REST Java Quarkus (port 8080), connectée nativement à Google Cloud Firestore via les identifiants gérés du Service Account Cloud Run.
- **Base de données** : Google Cloud Firestore en mode natif (`thezaurus-prod` ou `thezaurus-dev`).

---

## 📋 Prérequis

1. **Google Cloud SDK (`gcloud`)** :
   - Assurez-vous d'avoir la CLI [gcloud installée](https://cloud.google.com/sdk/docs/install) et à jour.
   - Connectez-vous avec votre compte GCP :
     ```bash
     gcloud auth login
     ```
   - Sélectionnez le projet cible :
     ```bash
     gcloud config set project <your project id>
     ```
   - Définissez la région Cloud Run par défaut (**`europe-west1`**) :
     ```bash
     gcloud config set run/region europe-west1
     ```

2. **Permissions GCP** :
   - Rôles nécessaires : `roles/run.admin`, `roles/iam.serviceAccountUser`, `roles/datastore.user`, `roles/storage.admin`.

3. **Client OAuth Google** :
   - Rendez-vous sur la console GCP > *APIs & Services > Credentials > OAuth 2.0 Client IDs*.
   - Ajoutez l'URL publique de redirection autorisée de votre domaine :
     `https://<votre-domaine-cloud-run>/api/auth/callback/google`

---

## ⚙️ Variables d'environnement pour la production

Avant de déployer, préparez les variables de production requises :

| Variable | Description | Valeur par défaut / Exemple |
|---|---|---|
| `GOOGLE_CLOUD_PROJECT_ID` | Identifiant du projet GCP cible | `thezaurus-494709` |
| `GCP_REGION` | Région de déploiement Cloud Run | `europe-west1` |
| `FIRESTORE_DATABASE_ID` | Base Firestore cible | `thezaurus-prod` *(ou `thezaurus-dev` pour la pré-prod)* |
| `FIRESTORE_COLLECTION_PREFIX` | Préfixe des collections Firestore | `prod` *(ou `dev`)* |
| `GOOGLE_CLIENT_ID` | ID client OAuth Google | `xxxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Secret client OAuth Google | `GOCSPX-xxxx` |
| `NEXTAUTH_PUBLIC_URL` | URL publique du front déployé | `https://thezaurus.zenika.com` |
| `NEXTAUTH_SECRET` | Secret de signature des sessions NextAuth | `$(openssl rand -base64 32)` |
| `REASONING_ENGINE_URL` | URL de l'agent IA Vertex AI Reasoning Engine | `https://europe-west1-aiplatform.googleapis.com/...` |
| `SLACK_BOT_TOKEN` *(optionnel)* | Token OAuth du Bot Slack | `xoxb-...` |
| `SLACK_SIGNING_SECRET` *(optionnel)* | Secret de signature Slack | `...` |

---

## 🚀 Procédure de déploiement

### 1. Exporter les variables d'environnement

Chargez vos variables d'environnement de production en mémoire (soit depuis un fichier `.env.prod`, soit directement) :

```bash
export $(grep -v '^#' .env.prod | xargs)
```

Ou exportez-les manuellement :

```bash
export GOOGLE_CLOUD_PROJECT_ID=<your project id>
export GCP_REGION=europe-west1
export FIRESTORE_DATABASE_ID=<your database id>
export FIRESTORE_COLLECTION_PREFIX=prod
export NEXTAUTH_PUBLIC_URL=https://votre-service-url.run.app
export NEXTAUTH_SECRET="<your secret>"
export GOOGLE_CLIENT_ID="<your client id>"
export GOOGLE_CLIENT_SECRET="<your client secret>"
export REASONING_ENGINE_URL="<your reasoning engine url>"
```

### 2. Lancer le déploiement Cloud Run

Exécutez la commande `gcloud run compose` en spécifiant la région (**`europe-west1`**) et le fichier `docker-compose.yml` :

```bash
gcloud run compose up docker-compose.yml --region ${GCP_REGION:-europe-west1} --allow-unauthenticated
```

*(Ou simplement `gcloud run compose up --region ${GCP_REGION:-europe-west1} --allow-unauthenticated` car `docker-compose.yml` est le fichier chargé automatiquement).*

---

## 🔍 Vérification et logs

1. **Vérifier l'état du service déployé** :
   ```bash
   gcloud run services list --region ${GCP_REGION:-europe-west1}
   ```

2. **Consulter les logs en temps réel** :
   ```bash
   gcloud run services logs tail thezaurus-v2 --region ${GCP_REGION:-europe-west1}
   ```

3. **Mise à jour de la configuration de l'application Slack (si utilisée)** :
   - Si l'URL publique de l'application a changé, mettez à jour l'URL d'interactivité et des commandes slash (`/talk`) sur la console [Slack API](https://api.slack.com/apps) :
     `https://<votre-domaine-cloud-run>/slack/events`
