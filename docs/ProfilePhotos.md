# Photos de profil et autorisation IA

Le profil permet de charger une photo JPEG, PNG ou WebP de 5 Mo maximum, sans recadrage. Le backend décode réellement l’image et la réencode en PNG sans métadonnées ; il limite les dimensions à 20 mégapixels pour borner la mémoire nécessaire au décodage. La photo personnalisée est utilisée dans le profil et les avatars des contributeurs. Un admin peut la remplacer ou rétablir Google depuis la gestion des utilisateurs.

La connexion synchronise la photo Google uniquement lorsqu’aucune photo personnalisée n’existe. Le retour à Google restaure la dernière URL Google connue ; la prochaine connexion l’actualise. En l’absence de photo, les avatars affichent les initiales.

## Stockage privé GCS

En production, configurer `PROFILE_PHOTOS_BUCKET` dans l’environnement de l’API. `docker-compose.yml` transmet cette variable. Sans bucket, le chargement en mode GCS est indisponible ; l’autorisation IA et les photos Google restent utilisables.

## Test en local

`docker compose up --build` démarre un conteneur `gcs` utilisant `fsouza/fake-gcs-server:1.56.1`. Il crée le bucket `thezaurus-photos-local` et conserve les objets dans le volume Docker `gcs-data`. L’API utilise le même client et le même code GCS qu’en production, dirigé vers `http://gcs:4443` via `quarkus.google.cloud.storage.host-override`. Cette configuration de l’extension Quarkus désactive les credentials pour l’émulateur.

L’API du bucket est consultable sur `http://localhost:4443/storage/v1/b/thezaurus-photos-local/o`. Le port est publié uniquement sur la boucle locale. Pour lancer l’API hors Docker, démarrer `docker compose up -d gcs firestore`, puis `./mvnw quarkus:dev` dans `api/` : le profil `dev` utilise par défaut `http://localhost:4443` et le même bucket. `GCS_EMULATOR_HOST` permet de changer l’adresse de l’émulateur. La production utilise le véritable service GCS avec ADC ; aucun stockage disque alternatif n’existe dans l’application.

Avec la configuration OAuth locale habituelle :

1. Ouvrir `http://localhost:3000`, se connecter avec Google, puis ouvrir **Mon profil**.
2. Charger une photo ; vérifier le nouvel avatar et recharger la page.
3. Activer puis retirer l’autorisation IA ; vérifier que la photo reste affichée.
4. Cliquer sur **Utiliser ma photo Google** ; vérifier le retour à la dernière photo Google connue.
5. Avec un compte admin, tester également le remplacement et la réinitialisation depuis la gestion des utilisateurs.

Le stockage émulé passe par les mêmes validations de fichiers, contrôles d’accès et service destiné aux agents que GCS. La connexion et la récupération de la photo Google nécessitent toujours une connexion Internet et le client OAuth configuré dans `.env`. L’émulateur ne reproduit pas les contrôles IAM de production.

### Test d’intégration avec le client GCS réel

Après `docker compose up -d gcs`, lancer depuis `api/` :

```sh
GCS_EMULATOR_TEST_HOST=http://localhost:4443 ./mvnw test -Dtest=ProfilePhotoEmulatorResourceTest
```

Sous PowerShell, définir `$env:GCS_EMULATOR_TEST_HOST='http://localhost:4443'`, puis lancer `./mvnw.cmd test -Dtest=ProfilePhotoEmulatorResourceTest`. Ce test vérifie chargement, affichage, autorisation IA, retrait et suppression via l’API et le client GCS réels ; seul le dépôt des utilisateurs est simulé. Il nettoie sa photo après exécution. Sans cette variable, ce test est ignoré pour que la suite habituelle ne dépende pas de Docker.

## Configuration du bucket de production

Suivre le [guide de configuration du bucket GCP](CloudStorage.md) : création depuis la console ou le CLI, droits du compte de service, configuration de l’API et vérification après déploiement.

Ces commandes sont des instructions de déploiement ; l’implémentation ne crée pas de ressource cloud. Les objets portent des noms aléatoires sous `profile-photos/`. Les réponses photo passent par le backend authentifié et ne sont pas mises en cache. Les anciens objets sont supprimés après remplacement ou réinitialisation ; si ce nettoyage échoue, le backend journalise l’échec sans annuler le choix enregistré.

Le remplacement et la réinitialisation échangent la référence de la photo dans une transaction Firestore. Le nettoyage utilise la référence retournée par la transaction validée, même si un admin et la personne concernée modifient simultanément la photo. Les objets GCS sont supprimés après la transaction, jamais pendant une tentative susceptible d’être rejouée.

## Contrats backend

- `PUT /api/me/profile/photo` : corps binaire JPEG, PNG ou WebP ; remplace uniquement sa photo personnalisée.
- `DELETE /api/me/profile/photo` : rétablit la dernière photo Google connue.
- `PUT /api/admin/users/{email}/photo` et `DELETE` : mêmes opérations pour un admin.
- `POST /api/me/profile/photo/google` : corps `{ "idToken": "…" }`, utilisé à la connexion. Le backend vérifie signature, expiration, audience OAuth, issuer et correspondance de l’email vérifié avant d’extraire la photo. Ce contrôle fonctionne aussi derrière IAP.
- `PUT /api/me/profile/ai-photo` : corps `{ "allowed": true|false }`. La cible vient exclusivement de l’identité authentifiée. Aucun endpoint admin ne permet de modifier cette autorisation pour autrui.
- `GET /api/users/{email}/photo` : affichage ordinaire pour les utilisateurs authentifiés disposant d’un rôle ; indépendant de l’autorisation IA. L’URL Google et la clé GCS restent privées.

`ProfilePhotoService.getForAgent(email)` est le point d’entrée interne des futurs agents. Il relit l’utilisateur et refuse l’accès sans autorisation explicite (`ForbiddenException`), retourne la photo personnalisée en priorité, ou signale son absence (`NotFoundException`). Il revérifie l’autorisation après la lecture distante pour appliquer un retrait intervenu pendant celle-ci. Le raccordement réseau et l’authentification machine seront ajoutés avec les agents ; leur intégration doit utiliser ce service.

L’autorisation couvre la photo courante même après remplacement par un admin. Le retrait concerne les récupérations futures ; les visuels existants et les copies déjà transmises sont conservés.
