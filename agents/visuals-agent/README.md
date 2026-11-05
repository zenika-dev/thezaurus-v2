# visuals-agent

Agent Quarkus (Java 21) de génération de visuels de communication pour les **talks de conférence**, **articles de blog** et **événements NightClazz** reposant sur le SDK officiel [Google GenAI pour Java (`googleapis/java-genai`)](https://github.com/googleapis/java-genai) et le modèle **Gemini 2.5 Flash Image** (`gemini-2.5-flash-image`).

## Structure du projet

```text
agents/visuals-agent/
├── pom.xml
├── src/main/
│   ├── docker/Dockerfile.jvm
│   ├── java/com/zenika/thezaurus/visuals/
│   │   ├── client/          # Intégration SDK Google GenAI (com.google.genai.Client)
│   │   ├── exception/       # Gestion centralisée des erreurs HTTP
│   │   ├── model/           # DTOs dédiés par type de visuel, réponses et événements SSE
│   │   ├── resource/        # Endpoints REST et SSE (Server-Sent Events)
│   │   └── service/         # Catalogue de templates et orchestration de génération
│   └── resources/
│       ├── application.properties
│       ├── templates.json   # Définition des 8 templates et prompts associés
│       └── templates/       # Fichiers d'images des templates (conf-1, conf-2, blog, nc...)
└── src/test/                # Tests unitaires et @QuarkusTest (100 % sans appel IA)
```

## Catalogue des 8 templates intégrés

| Nom du template | Catégorie | Fichier image | Cas d'usage (routage automatique) |
| --- | --- | --- | --- |
| `generate-image-speaker-event` | `TALK` | `templates/conf-1.png` | Talk à une conférence avec **1 speaker** |
| `generate-image-2-speaker-event` | `TALK` | `templates/conf-1-binome.png` | Talk en binôme avec **2 speakers** |
| `generate-image-3-speaker-event` | `TALK` | `templates/conf-1-trinome.png` | Talk en trinôme avec **3 speakers** |
| `generate-image-2-talks-event` | `TALK` | `templates/conf-2.png` | **2 talks distincts** à une même conférence (`talks` de taille 2) |
| `generate-image-blog-post` | `BLOG` | `templates/blog.png` | Article de blog avec **1 auteur (`writers`)** |
| `generate-image-2-blog-post` | `BLOG` | `templates/blog-2.png` | Article de blog avec **2 co-auteurs (`writers`)** |
| `generate-image-nightclazz` | `NIGHT_CLAZZ` | `templates/nc.png` | Événement NightClazz (**1 ou 2 speakers**, sans partenaire) |
| `generate-image-nightclazz-partner` | `NIGHT_CLAZZ` | `templates/nc-partenaire.png` | Événement NightClazz **en partenariat** (`partner` renseigné, 1 speaker) |

### Fournir ou surcharger des templates

Deux options sont disponibles :
1. **Dans le composant (`src/main/resources/templates/`)** : remplacez directement les fichiers PNG ou ajoutez vos propres entrées dans `src/main/resources/templates.json`.
2. **Via un dossier externe (`VISUALS_TEMPLATES_DIR`)** : montez un volume ou configurez la variable d'environnement `VISUALS_TEMPLATES_DIR=/chemin/vers/dossier` pour surcharger les fichiers d'images sans recompiler l'application.

## Endpoints exposés

| Méthode | Endpoint | Body | Mode | Description |
| --- | --- | --- | --- | --- |
| `GET` | `/visuals/templates` | — | REST (`application/json`) | Liste les templates disponibles (`?category=TALK`, `BLOG` ou `NIGHT_CLAZZ`) |
| `POST` | `/visuals/talks` | `TalkVisualRequest` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel de talk |
| `POST` | `/visuals/talks/stream` | `TalkVisualRequest` | **SSE (`text/event-stream`)** | Flux SSE pour visuel de talk |
| `POST` | `/visuals/blogs` | `BlogVisualRequest` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel de blog |
| `POST` | `/visuals/blogs/stream` | `BlogVisualRequest` | **SSE (`text/event-stream`)** | Flux SSE pour visuel de blog |
| `POST` | `/visuals/nightclazz` | `NightClazzVisualRequest` | REST Synchrone (`@RunOnVirtualThread`) | Génère un visuel NightClazz |
| `POST` | `/visuals/nightclazz/stream` | `NightClazzVisualRequest` | **SSE (`text/event-stream`)** | Flux SSE pour visuel NightClazz |

### Structures de données en entrée

#### 1. Talk (`TalkVisualRequest` — `/visuals/talks`)
- `title` : Titre du talk (obligatoire sauf si `talks` est fourni).
- `conference` : Conférence visée (chaîne simple `"Devoxx France"` ou objet `{"name": "...", "logoBase64": "..."}`).
- `speakers` : Liste des speakers (1, 2 ou 3 chaînes ou objets `{"name": "...", "photoBase64": "..."}`).
- `talks` : *(Optionnel, pour `conf-2.png`)* Liste de 2 talks `[{"title": "...", "speaker": {"name": "...", "photoBase64": "..."}}]`.

#### 2. Blog (`BlogVisualRequest` — `/visuals/blogs`)
- `title` : Titre de l'article de blog (obligatoire).
- `writers` : Liste des rédacteurs / auteurs (1 ou 2 chaînes ou objets `{"name": "...", "photoBase64": "..."}`).

#### 3. NightClazz (`NightClazzVisualRequest` — `/visuals/nightclazz`)
- `title` : Titre du NightClazz (obligatoire).
- `date`, `hour`, `location` : Informations pratiques (ex. `"19/11"`, `"18h30-21h"`, `"Zenika Nantes"`).
- `speakers` : Liste des speakers (1 ou 2 chaînes ou objets `{"name": "...", "photoBase64": "..."}`).
- `partner` : *(Optionnel, active le template `nc-partenaire.png`)* Partenaire (chaîne simple ou objet `{"name": "...", "logoBase64": "..."}`).

## Configuration

L'agent charge automatiquement le fichier `.env` situé à la **racine du dépôt** (`../../.env` — initialisé depuis le `.env-template` racine) :

```bash
cp ../../.env-template ../../.env
```

| Variable d'environnement | Propriété Quarkus | Défaut | Description |
| --- | --- | --- | --- |
| `VISUALS_AGENT_PORT` | `quarkus.http.port` | `8082` | Port d'écoute HTTP du service |
| `GEMINI_MODEL` | `visuals.gemini.model` | `gemini-2.5-flash-image` | Modèle utilisé pour générer les visuels |
| `GEMINI_TIMEOUT_MS` | `visuals.gemini.timeout-ms` | `120000` | Timeout HTTP des requêtes vers Gemini (en ms) |
| `GOOGLE_API_KEY` | `visuals.gemini.api-key` | *(vide)* | Clé d'API Gemini Developer API |
| `GOOGLE_GENAI_USE_VERTEXAI` | `visuals.gemini.vertex-ai` | `false` | Activer Vertex AI (`true`) au lieu de Gemini Developer API |
| `GOOGLE_CLOUD_PROJECT_ID` | `visuals.gemini.project` | *(vide)* | Projet GCP (si Vertex AI est activé) |
| `GCP_REGION` / `GOOGLE_CLOUD_LOCATION` | `visuals.gemini.location` | `europe-west1` | Région GCP (si Vertex AI est activé) |
| `VISUALS_TEMPLATES_DIR` | `visuals.templates.dir` | *(vide)* | Dossier externe optionnel pour surcharger les images de templates |
| `VISUALS_SSE_HEARTBEAT_MS` | `visuals.sse.heartbeat-interval-ms` | `3000` | Intervalle des événements `HEARTBEAT` en mode SSE |

## Exécuter les tests (sans appel IA)

Tous les tests unitaires et d'intégration mockent l'interface `GenAiVisualClient` afin de garantir **zéro coût et zéro appel réseau vers Gemini** :

```bash
./mvnw test
```
