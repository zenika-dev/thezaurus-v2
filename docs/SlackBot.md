# Bot Slack (/talk)

Ce document décrit l'intégration, la configuration et l'utilisation du bot Slack pour **TheZaurus-v2**.

---

## 📌 Présentation

L'API backend expose un bot Slack permettant de créer et gérer des talks directement via des commandes slash (`/talk`) grâce au SDK [Bolt for Java](https://github.com/slackapi/java-slack-sdk).

> [!NOTE]
> Cette intégration est **totalement optionnelle**. Si les variables d'environnement Slack ne sont pas renseignées, l'application démarre normalement en désactivant le module Slack (aucun appel externe ni enregistrement de webhook).

---

## ⚙️ Variables d'environnement

Les variables suivantes sont configurables dans votre fichier `.env` :

| Variable | Requise | Description |
|---|---|---|
| `SLACK_BOT_TOKEN` | Optionnelle | Token OAuth de l'utilisateur bot (`xoxb-...`). Si absent, le bot Slack est désactivé. |
| `SLACK_SIGNING_SECRET` | Optionnelle | Secret de signature permettant à l'API de vérifier l'authenticité des requêtes Slack. |
| `SLACK_APP_TOKEN` | Optionnelle | Token de niveau application (`xapp-...`), utilisé si le mode Socket est activé. |

---

## 🚀 Configuration de l'application Slack

### 1. Création via le Manifest YAML

Le projet inclut un fichier [`api/src/main/resources/manifest.yaml`](../api/src/main/resources/manifest.yaml) décrivant l'ensemble de la configuration requise (nom, commandes, scopes OAuth, URL d'interactivité).

1. Rendez-vous sur la console [Slack API](https://api.slack.com/apps) et cliquez sur **Create New App**.
2. Choisissez **From an app manifest**, puis sélectionnez votre espace de travail Slack de dev/test.
3. Collez le contenu de [`manifest.yaml`](../api/src/main/resources/manifest.yaml) (onglet **YAML**).
4. Remplacez toutes les occurrences de `https://your-url.zenika.com` par l'URL publique de votre API (URL de production ou URL ngrok de développement).
5. Cliquez sur **Create** puis **Review summary & create app**.
6. Dans le menu **OAuth & Permissions**, cliquez sur **Install to Workspace** et autorisez l'application.
7. Copiez le **Bot User OAuth Token** (commençant par `xoxb-`) et définissez-le dans `SLACK_BOT_TOKEN`.
8. Dans le menu **Basic Information > App Credentials**, copiez le **Signing Secret** et définissez-le dans `SLACK_SIGNING_SECRET`.
9. Enregistrez ces valeurs dans votre `.env` à la racine et redémarrez l'API.

---

## 💻 Test en local avec ngrok

Pour recevoir les événements Slack et les commandes slash en local, Slack doit pouvoir accéder à votre API via une URL HTTPS publique.

1. Lancez un tunnel ngrok vers le port de l'API (8080) :
   ```bash
   ngrok http 8080
   ```
2. Récupérez l'URL HTTPS attribuée (ex : `https://xxxx.ngrok-free.app`).
3. Mettez à jour votre application Slack :
   - **Slash Commands** (`/talk`) : URL fixée à `https://xxxx.ngrok-free.app/slack/events`
   - **Interactivity & Shortcuts** : URL fixée à `https://xxxx.ngrok-free.app/slack/events`

> [!WARNING]
> En version gratuite de ngrok, l'URL change à chaque redémarrage. Pensez à actualiser les URLs dans la console Slack API à chaque nouvelle session de test.

---

## 💬 Commandes disponibles

| Commande | Description |
|---|---|
| `/talk` | Ouvre une modale interactive permettant de saisir et publier un nouveau talk (titre, speakers, agence, description, statut, visibilité, conférence, date). |
