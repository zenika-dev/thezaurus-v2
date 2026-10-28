# visuals-agent

Agent Quarkus (Java 21) de génération de visuels de communication pour les **talks** et **articles de blog** reposant sur le SDK officiel [Google GenAI pour Java (`googleapis/java-genai`)](https://github.com/googleapis/java-genai) et le modèle **Gemini 4 Argon** (`gemini-4-argon`).

## Structure du projet

```text
agents/visuals-agent/
├── pom.xml
├── src/main/
│   ├── docker/Dockerfile.jvm
│   ├── java/com/zenika/thezaurus/visuals/
│   │   ├── client/          # Intégration SDK Google GenAI (com.google.genai.Client)
│   │   ├── exception/       # Gestion centralisée des erreurs HTTP
│   │   ├── model/           # DTOs de requêtes, réponses, événements SSE et jobs Async
│   │   ├── resource/        # Endpoints REST, SSE (Server-Sent Events) et Async (202)
│   │   └── service/         # Catalogue de templates et orchestration de génération
│   └── resources/
│       ├── application.properties
│       ├── templates.json   # Définition des templates et prompts associés
│       └── templates/       # Fichiers d'images des templates (remplaçables)
└── src/test/                # Tests unitaires et @QuarkusTest (100 % sans appel IA)
```

## Fournir vos propres templates de visuels

Deux options sont disponibles :
1. **Dans le composant (`src/main/resources/templates/`)** : remplacez directement les fichiers `talk-single.png`, `talk-duo.png`, `talk-trio.png`, `blog-single.png`, `blog-duo.png` ou ajoutez vos propres entrées dans `src/main/resources/templates.json`.
2. **Via un dossier externe (`VISUALS_TEMPLATES_DIR`)** : montez un volume ou configurez la variable d'environnement `VISUALS_TEMPLATES_DIR=/chemin/vers/dossier` pour surcharger les fichiers d'images sans recompiler l'application.

## Endpoints exposés

| Méthode | Endpoint | Mode | Description |
| --- | --- | --- | --- |
| `GET` | `/visuals/templates` | REST (`application/json`) | Liste les templates disponibles (`?category=TALK` ou `BLOG`) |
| `POST` | `/visuals` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel avec routage automatique du template selon les inputs |
| `POST` | `/visuals/stream` | **SSE (`text/event-stream`)** | Génère un visuel en flux continu avec heartbeats anti-timeout |
| `POST` | `/visuals/talks` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel de talk (type forcé à `talk`) |
| `POST` | `/visuals/blogs` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel de blog (type forcé à `blog`) |
| `POST` | `/visuals/talks/stream` | **SSE (`text/event-stream`)** | Flux SSE pour visuel de talk |
| `POST` | `/visuals/blogs/stream` | **SSE (`text/event-stream`)** | Flux SSE pour visuel de blog |

### Structure de données en entrée (`VisualRequest`)

Le modèle de template **n'est pas fourni en entrée** : l'API résout automatiquement le template adapté en fonction des données reçues :

- `title` : Titre du talk ou de l'article de blog (obligatoire).
- `type` : `"talk"` ou `"blog"` (optionnel si déductible de la présence de `conference`/`speakers` ou `writers`).
- `conference` : Nom de la conférence (chaîne simple ou objet `{"name": "...", "logoBase64": "..."}`).
- `speakers` : Liste des speakers (liste de chaînes ou d'objets `{"name": "...", "photoBase64": "..."}`).
  - 1 speaker ➔ Template `generate-image-speaker-event`
  - 2 speakers ➔ Template `generate-image-2-speaker-event`
  - 3 speakers ➔ Template `generate-image-3-speaker-event`
- `writers` : Liste des rédacteurs / auteurs (liste de chaînes ou d'objets `{"name": "...", "photoBase64": "..."}`).
  - 1 rédacteur ➔ Template `generate-image-blog-post`
  - 2 rédacteurs ➔ Template `generate-image-2-blog-post`

## Configuration

Pour configurer l'agent en local, copiez le fichier `.env-template` vers `.env` puis complétez les valeurs :

```bash
cp .env-template .env
```

| Variable d'environnement | Propriété Quarkus | Défaut | Description |
| --- | --- | --- | --- |
| `VISUALS_AGENT_PORT` | `quarkus.http.port` | `8082` | Port d'écoute HTTP du service |
| `GEMINI_MODEL` | `visuals.gemini.model` | `gemini-4-argon` | Modèle utilisé pour générer les visuels |
| `GEMINI_TIMEOUT_MS` | `visuals.gemini.timeout-ms` | `120000` | Timeout HTTP des requêtes vers Gemini (en ms) |
| `GOOGLE_API_KEY` | `visuals.gemini.api-key` | *(vide)* | Clé d'API Gemini Developer API |
| `GOOGLE_GENAI_USE_VERTEXAI` | `visuals.gemini.vertex-ai` | `false` | Activer Vertex AI (`true`) au lieu de Gemini Developer API |
| `GOOGLE_CLOUD_PROJECT_ID` | `visuals.gemini.project` | *(vide)* | Projet GCP (si Vertex AI est activé) |
| `GOOGLE_CLOUD_LOCATION` | `visuals.gemini.location` | `europe-west1` | Région GCP (si Vertex AI est activé) |
| `VISUALS_TEMPLATES_DIR` | `visuals.templates.dir` | *(vide)* | Dossier externe optionnel pour surcharger les images de templates |
| `VISUALS_SSE_HEARTBEAT_MS` | `visuals.sse.heartbeat-interval-ms` | `3000` | Intervalle des événements `HEARTBEAT` en mode SSE |

## Exécuter les tests (sans appel IA)

Tous les tests unitaires et d'intégration mockent l'interface `GenAiVisualClient` afin de garantir **zéro coût et zéro appel réseau vers Gemini** :

```bash
./mvnw test
```
