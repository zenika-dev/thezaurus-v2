# Bucket Google Cloud Storage — photos de profil

Ce guide décrit la configuration du bucket privé utilisé par TheZaurus pour les photos personnalisées. Le fonctionnement du profil, l’autorisation IA et les tests sont décrits dans [ProfilePhotos.md](ProfilePhotos.md).

## Variables d’environnement

| Variable | Environnement | Description |
|---|---|---|
| `PROFILE_PHOTOS_BUCKET` | API en production | Nom du bucket, sans préfixe `gs://`. Requis pour charger des photos personnalisées. |
| `GOOGLE_CLOUD_PROJECT_ID` | API en production | Projet GCP de l’application. |
| `GOOGLE_APPLICATION_CREDENTIALS` | API hors Cloud Run | Chemin des Application Default Credentials, si nécessaire. Docker Compose utilise le montage `GCLOUD_ADC` existant. |
| `GCS_EMULATOR_HOST` | API en profil `dev` | Adresse HTTP de l’émulateur : `http://gcs:4443` dans Compose, `http://localhost:4443` hors Docker. |

En production, l’API utilise les Application Default Credentials du compte de service Cloud Run. Aucune clé JSON supplémentaire n’est nécessaire sur Cloud Run. Le bucket reste privé : le navigateur récupère les images via l’API authentifiée, jamais directement via une URL publique GCS.

## 1. Identifier le projet et le compte de service

Prérequis pour les commandes : installer le [Google Cloud CLI](https://cloud.google.com/sdk/docs/install), exécuter `gcloud auth login` et utiliser un compte autorisé à créer le bucket et à modifier ses permissions.

Utiliser un bucket distinct par environnement et choisir sa région en fonction de celle de l’application, par exemple `europe-west1`.

Dans la console Google Cloud, ouvrir **Cloud Run → service TheZaurus → Security / Sécurité** et relever le compte de service utilisé à l’exécution. Il faut donner accès à ce compte, qui peut être différent du compte de déploiement CI.

Depuis le CLI, remplacer les valeurs suivantes :

```sh
PROJECT_ID="mon-projet-gcp"
REGION="europe-west1"
SERVICE="mon-service-cloud-run"
BUCKET="mon-projet-thezaurus-photos-prod"

gcloud run services describe "$SERVICE" \
  --project="$PROJECT_ID" --region="$REGION" \
  --format='value(spec.template.spec.serviceAccountName)'
```

Noter le résultat comme `API_SERVICE_ACCOUNT`. Les noms de buckets doivent être uniques dans Google Cloud.

## 2. Créer le bucket privé

### Depuis la console

1. Ouvrir **Cloud Storage → Buckets → Create / Créer**.
2. Choisir le nom du bucket et une localisation régionale, par exemple `europe-west1`.
3. Choisir la classe de stockage **Standard**.
4. Activer **Public access prevention / Prévention de l’accès public**.
5. Choisir **Uniform / Uniforme** pour le contrôle d’accès.
6. Créer le bucket.

### Avec `gcloud`

```sh
gcloud services enable storage.googleapis.com --project="$PROJECT_ID"

gcloud storage buckets create "gs://$BUCKET" \
  --project="$PROJECT_ID" \
  --location="$REGION" \
  --default-storage-class=STANDARD \
  --uniform-bucket-level-access \
  --public-access-prevention
```

Ces opérations nécessitent des droits d’administration du stockage pour la personne qui configure le bucket. Elles ne sont pas exécutées par l’application.

## 3. Autoriser le compte de service de l’API

Dans **Bucket → Permissions → Grant access / Accorder l’accès**, ajouter le compte de service de l’API avec le rôle **Storage Object User** (`roles/storage.objectUser`) sur ce bucket uniquement. Ce rôle permet les lectures, créations, modifications et suppressions d’objets nécessaires à la gestion des photos.

Équivalent CLI :

```sh
API_SERVICE_ACCOUNT="mon-api@mon-projet-gcp.iam.gserviceaccount.com"

gcloud storage buckets add-iam-policy-binding "gs://$BUCKET" \
  --member="serviceAccount:$API_SERVICE_ACCOUNT" \
  --role=roles/storage.objectUser
```

Le compte de service de l’API n’a pas besoin du rôle `Storage Admin`. Aucun accès `allUsers` ou `allAuthenticatedUsers` n’est requis.

## 4. Configurer et redéployer l’API

Définir dans l’environnement du conteneur **API** :

```dotenv
PROFILE_PHOTOS_BUCKET=mon-projet-thezaurus-photos-prod
```

Avec le déploiement Docker Compose du projet, renseigner cette variable dans l’environnement de déploiement ou son fichier `.env`, puis suivre [Deploiement.md](Deploiement.md). `docker-compose.yml` la transmet à l’API.

Avec le déploiement GitHub Actions, définir la variable `PROFILE_PHOTOS_BUCKET` dans chaque environnement GitHub cible (Settings → Environments → environnement → Variables). Le workflow `_deploy.yml` vérifie sa présence et l’injecte dans le conteneur API du manifeste Cloud Run. Une configuration manuelle du service Cloud Run est écrasée au déploiement suivant.

Vérifier que le profil de production est actif et qu’aucune surcharge `QUARKUS_GOOGLE_CLOUD_STORAGE_HOST_OVERRIDE` ne pointe vers un émulateur. Le conteneur frontend n’a besoin ni du nom du bucket ni d’identifiants GCS.

## 5. Vérifier le fonctionnement

1. Se connecter à TheZaurus, ouvrir **Mon profil** et charger une image JPEG, PNG ou WebP de moins de 5 Mo.
2. Recharger le profil et vérifier que l’image reste affichée.
3. Dans la console du bucket, vérifier la présence d’un objet sous `profile-photos/`. Les noms sont aléatoires ; aucun email n’est inclus dans le nom de l’objet.
4. Remplacer la photo puis utiliser **Utiliser ma photo Google** : la photo personnalisée est retirée et son ancien objet est supprimé par l’API.
5. Vérifier que l’autorisation IA conserve sa valeur pendant ces changements.

L’API réencode les images en PNG sans métadonnées et limite leur taille à 20 mégapixels. Les photos Google sont téléchargées depuis Google ; elles n’occupent pas d’objet dans ce bucket.

## Développement local

`docker compose up --build` démarre automatiquement `fake-gcs-server` et le bucket `thezaurus-photos-local`, sans compte GCP ni credentials. Le volume Docker `gcs-data` conserve les objets. Consulter [le parcours de test local](ProfilePhotos.md#test-en-local) et [la liste des objets](http://localhost:4443/storage/v1/b/thezaurus-photos-local/o).

## Dépannage

| Symptôme | Vérification |
|---|---|
| Stockage non configuré / HTTP 503 au chargement | `PROFILE_PHOTOS_BUCKET` est renseigné dans l’API et le conteneur a été redéployé. |
| GCS renvoie 403 | Le compte de service d’exécution possède `roles/storage.objectUser` sur le bon bucket. |
| Bucket introuvable | Le bucket existe et sa valeur est un nom simple, sans `gs://` ni chemin. |
| Échec des credentials hors Cloud Run | Exécuter `gcloud auth application-default login` avec un compte autorisé et vérifier le montage `GCLOUD_ADC` pour Docker. |
| La photo Google échoue mais le chargement fonctionne | Vérifier la synchronisation à la connexion et l’accès HTTPS à `*.googleusercontent.com`. L’API suit jusqu’à trois redirections, en validant chaque destination. |
| Les objets locaux semblent perdus | Vérifier que Compose réutilise le même projet et le volume `gcs-data`. |

Références : [rôles IAM Cloud Storage](https://cloud.google.com/storage/docs/access-control/iam-roles), [accès uniforme](https://cloud.google.com/storage/docs/using-uniform-bucket-level-access), [prévention de l’accès public](https://cloud.google.com/storage/docs/using-public-access-prevention).
