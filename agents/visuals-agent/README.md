# Visuals Agent

Service Quarkus autonome en Java 21. Il vit dans `agents` et expose une API HTTP utilisable par d’autres applications, sans dépendance à l’API métier, à sa base ou au frontend TheZaurus. Le recadrage manuel du POC n’est plus exposé.

Java2D assemble les photos et les textes sur les huit templates embarqués. Les fonds nettoyés conservent les éléments graphiques d’origine ; les textes sont rendus avec les polices Nunito embarquées et les dimensions ajustées pendant la validation du POC. Gemini analyse uniquement les boîtes du sujet et du visage ; il ne génère ni ne modifie les pixels du visuel.

## Lancer localement

Depuis l’application standard, `docker compose up --build` démarre aussi l’agent via
`docker-compose.override.yml`. Dans la fiche d’un talk enregistré avec un à trois
intervenants et une conférence, le bouton **Télécharger le visuel** sélectionne le modèle
adapté au nombre d’intervenants et télécharge son PNG. Enregistrer les modifications du talk
avant de générer. L’API contrôle l’autorisation photo IA pour chaque personne.
En cas de refus, sa photo reste utilisée par le rendu Java avec `autoCrop: false` :
le plus grand rectangle au ratio requis est centré horizontalement et aligné en haut,
sans zoom supplémentaire ni appel Gemini. Le logo Zenika remplace uniquement une photo absente. Le cadrage automatique utilise la configuration Vertex/ADC ou
`GOOGLE_API_KEY` du Compose local. `VISUALS_AI_ENABLED=false` permet un rendu sans
appel Gemini. En local, le service reste interne au réseau Compose. En production,
il est déployé comme service Cloud Run indépendant par le workflow Release ; voir le
[guide de déploiement](../../docs/Deploiement.md).

Le bouton est aussi disponible dans la fiche d’un article de blog enregistré avec
un ou deux auteurs. Il appelle `/visuals/blogs/image` et télécharge un PNG
1080 × 1350 avec le modèle correspondant au nombre d’auteurs. Le même cadrage déterministe sans IA s’applique aux auteurs sans autorisation photo IA ; les auteurs sans photo utilisent le logo Zenika.

Pour lancer uniquement l'agent, depuis la racine du dépôt, sur toute plateforme disposant de Docker Compose :

```sh
docker compose up --build visuals-agent
```

Le service écoute sur le port 8087 dans le réseau Compose. Pour un lancement Java
hors conteneur, compiler avec le wrapper Maven standard (`./mvnw package`, ou
`mvnw.cmd package` sous Windows), puis exécuter depuis ce module :

```sh
java -Djava.awt.headless=true -jar target/quarkus-app/quarkus-run.jar
```

Java 21 est requis. Configurer les variables ci-dessous dans l'environnement du
processus avant le lancement Java ; le chargement du `.env` racine par Docker Compose
est utilisé pour le lancement conteneurisé. Hors conteneur, le service écoute par
défaut sur `http://127.0.0.1:8087`.

| Variable | Usage |
| --- | --- |
| `GOOGLE_CLOUD_PROJECT_ID` | Projet Vertex AI ; alias `GOOGLE_CLOUD_PROJECT` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Chemin du fichier ADC ; alias local `GCLOUD_ADC` |
| `GOOGLE_CLOUD_LOCATION` | Région Vertex, `global` par défaut ; alias `GCP_REGION` |
| `GOOGLE_API_KEY` | Alternative à Vertex ADC ; alias `GEMINI_API_KEY` |
| `GEMINI_CROP_MODEL` | `gemini-3.1-flash-lite` par défaut |
| `VISUALS_AI_ENABLED` | `true` par défaut ; `false` désactive l’analyse Gemini |
| `VISUALS_API_KEY` | Clé unique optionnelle pour le lancement local |
| `VISUALS_API_KEYS_JSON` | Registre JSON `{application: clé}` de clés distinctes, fourni par Secret Manager en production |
| `VISUALS_REQUIRE_API_KEY` | `true` en production : refuse de démarrer sans clés |
| `VISUALS_AGENT_HOST`, `VISUALS_AGENT_PORT` | Adresse et port du service hors Compose |

ADC doit permettre d’appeler Vertex AI dans le projet configuré. Sans configuration Gemini/Vertex, une demande de cadrage automatique retourne une erreur de configuration. `autoCrop: false` permet un rendu sans appel IA, centré horizontalement et aligné en haut.

## API

L’architecture de la PR 190 est conservée : `resource` expose REST/SSE, `model` porte les DTO des champs pris en charge, `service` orchestre catalogue, cadrage et rendu, `client` charge les images, `exception` centralise les erreurs. Les packages `crop` et `render` isolent l’analyse Gemini et le rendu déterministe.

La documentation interactive est disponible sur `/q/swagger-ui`, le contrat sur `/q/openapi` et la santé sur `/q/health`.

| Route | Résultat |
| --- | --- |
| `GET /visuals/templates` | Catalogue du rendu déterministe, filtrable par catégorie |
| `POST /visuals/talks`, `/visuals/blogs`, `/visuals/nightclazz` | JSON contenant le PNG en base64, les cadrages et l’utilisation IA |
| `POST /visuals/{talks,blogs,nightclazz}/image` | PNG directement, avec le même corps de requête |
| `POST /visuals/{talks,blogs,nightclazz}/stream` | SSE : STARTED, GENERATING puis COMPLETED ou FAILED |

Exemple de corps JSON :

```json
{
  "title": "Une conférence",
  "conference": "Devoxx France",
  "speakers": [{"name": "Prénom Nom", "photo": {"base64": "BASE64_DE_LA_PHOTO"}}],
  "autoCrop": true
}
```

Les entrées conservent les structures principales de la PR 190 : `speakers` pour talks/NightClazz, `writers` pour blogs ; `conference` et `partner` acceptent une chaîne ou un objet `{name, logoBase64}`. NightClazz accepte aussi `date`, `hour` et `location`. Les personnes peuvent être des noms simples (logo Zenika par défaut) ou des objets avec photo. Le base64 brut et les data URL PNG/JPEG sont acceptés ; les URL distantes sont refusées avec HTTP 400. `autoCrop` est optionnel, activé par défaut. Il peut aussi être défini par personne : `{"name":"Alice","photo":{"base64":"..."},"autoCrop":false}`. Un refus par personne reste prioritaire même si le `autoCrop` de la requête vaut `true` ; un refus global désactive le cadrage IA pour toutes les personnes. Les valeurs autres qu’un booléen sont refusées. Sans IA, le plus grand rectangle au bon ratio est centré horizontalement et aligné sur le haut de la source.

Les anciens alias de personne `speakerName`, `authorName`, `speakerPhotoBase64` et
`authorPhotoBase64` sont aussi acceptés. Les champs modernes `name` et `photo`
ont priorité lorsqu’ils sont renseignés. Les URL distantes restent volontairement
refusées : les applications clientes fournissent les pixels autorisés en base64.

Lors d’une mise à jour, déployer cette version de l’agent avant l’API TheZaurus :
les anciennes versions de l’agent refusent le nouveau champ `autoCrop` par personne.
Les clients existants qui ne fournissent pas ce champ restent compatibles avec le nouvel agent.

Le catalogue conserve les huit noms publics et leurs chemins d’images. Il ne
reproduit pas le catalogue de prompts Gemini de la PR 190 : `description` décrit
le modèle de rendu, `fields` énumère les paramètres du DTO attendu, et `prompt`
est `null` (comme `promptUsed` dans les réponses). Le choix validé est un rendu
Java déterministe avec analyse du cadrage uniquement ; aucun prompt de génération
d’image n’est exécuté ni reconstitué pour maintenir artificiellement ce champ.

Pour deux talks, envoyer `conference` et `talks: [{"title":"Premier talk","speakers":["Alice"]},{"title":"Second talk","speakers":["Bob"]}]` sans `speakers` racine. Chaque talk a exactement un speaker. Le routage sélectionne automatiquement les huit templates. Les noms publics restent ceux de la PR 190 (`generate-image-speaker-event`, etc.). Les identifiants internes sont `conf-1`, `conf-1-binome`, `conf-1-trinome`, `conf-2`, `blog`, `blog-2`, `nc` et `nc-partenaire`. Les modèles blog produisent un PNG 1080 × 1350 ; les autres un PNG 1080 × 1080.

Une application cliente envoie les photos en base64 et récupère le PNG sans interface intermédiaire. En production, chaque application joint sa propre clé `X-API-Key: ...`, enregistrée dans `VISUALS_API_KEYS_JSON`. `VISUALS_API_KEY` reste disponible pour le lancement local à clé unique. La configuration d’un accès distant et de son authentification appartient au déploiement du service.

## Cadrage et coût

L’analyse de chaque photo est réutilisée dans un cache mémoire borné à 64 entrées. La géométrie du cadrage et le contrôle des marges sont déterministes. Une analyse valide donne lieu à un seul appel Gemini ; jusqu’à deux corrections supplémentaires sont autorisées si sa réponse JSON ou ses boîtes sont invalides. Une géométrie impossible dans le template ne déclenche pas de nouvelle analyse payante. La réponse JSON restitue les cadrages, leurs limites et les compteurs d’utilisation pour observer ces cas.

Les photos ne sont pas enregistrées par le service. Les nouvelles photos de validation et les galeries locales doivent rester sous `target/manual-fixtures` ou `target`, exclus de Git. Aucun portrait réel ni fichier ADC ne doit entrer dans l’image Docker ou les sources.

## Vérifier et conteneuriser

```sh
./mvnw verify
docker build -f src/main/docker/Dockerfile.jvm -t visuals-agent:local .
docker run --rm -p 127.0.0.1:8087:8087 -e VISUALS_AI_ENABLED=false visuals-agent:local
```

Les tests automatiques utilisent un transport simulé et ne consomment pas Gemini. Les tests réels nécessitent les credentials locaux et les photos de validation fournies. Les vérifications visuelles locales utilisent le navigateur invisible via la skill `background-browser`. Le service et son conteneur n’ont besoin ni de Node ni d’un navigateur pour produire les PNG. Pour Vertex dans un conteneur, fournir une identité ou monter le fichier ADC en lecture seule et définir `GOOGLE_APPLICATION_CREDENTIALS` ; ne pas incorporer de credentials dans l’image.

## ADC privé avec Docker Compose

L’override monte le fichier hôte en lecture seule, sans modifier son propriétaire
ni son mode (y compris `0600` sous Linux). Le point d’entrée démarre brièvement en
root, copie les credentials avec le mode `0400` pour l’UID/GID 10001 dans un tmpfs
`/run/visuals`, puis exécute Java via `gosu` sous cet utilisateur non privilégié.
Cette copie disparaît avec le conteneur et n’entre ni dans l’image ni dans un volume
persistant. Hors Compose, l’image reste non-root par défaut ; fournir une identité
ou un fichier déjà lisible par l’utilisateur du conteneur. Sur Cloud Run, les ADC
viennent de l’identité du service, sans fichier ni bootstrap root. La CI vérifie
l’agent avec `mvnw verify`, sans appel réel à Gemini.

## Contrat autonome et intégration TheZaurus

TheZaurus consomme uniquement les endpoints PNG `/visuals/talks/image` (1 à 3 intervenants)
et `/visuals/blogs/image` (1 à 2 auteurs). Les réponses JSON, les flux SSE, le catalogue,
les modèles NightClazz et double-talk ainsi que les alias d’entrée appartiennent au contrat
HTTP autonome du POC. Ils restent couverts par ses tests, sans bouton ni appel correspondant
dans l’application standard. Leur présence ne les intègre pas aux parcours de cette application.

Le timeout de l’appel depuis l’API TheZaurus est configurable avec `VISUALS_AGENT_TIMEOUT`
(durée ISO-8601, `PT120S` par défaut). Une clé `VISUALS_API_KEY` vide désactive volontairement
l’authentification de service pour l’usage local ; une clé non vide protège tous les chemins `/visuals`.

## Production

Le workflow Release construit l’image `visuals-agent` avec la version `agents-v*`,
la vérifie sans appel IA, puis déploie un service Cloud Run indépendant par digest.
Il peut être livré sans déployer TheZaurus. Chaque application, hébergée sur GCP
ou ailleurs, appelle son URL HTTPS avec sa clé `X-API-Key`. TheZaurus conserve
uniquement sa propre clé ; l’agent lit le registre des clés depuis Secret Manager.
Les routes `/visuals` exigent une clé valide. Les endpoints de santé et OpenAPI
restent publics. Le cadrage utilise l’identité Cloud Run propre à l’agent pour Vertex AI.
Les secrets, la révocation, les variables et un exemple d’appel client sont décrits
dans le [guide de déploiement](../../docs/Deploiement.md).
