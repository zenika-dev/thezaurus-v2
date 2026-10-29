# Issue 52 — Photo de profil et autorisation IA

Périmètre issu de l’entretien du 23 octobre 2026, validé par l’utilisateur avant implémentation.

## Autorisation IA

- Autorisation globale pour la création de visuels de talks et d’articles de blog ; refus par défaut, y compris pour les utilisateurs existants sans valeur enregistrée.
- Seule la personne concernée peut donner ou retirer son autorisation. Le backend vérifie cette propriété ; un admin ne peut pas modifier l’autorisation d’autrui.
- Ajouter, remplacer ou supprimer une photo personnalisée conserve l’autorisation courante, même si un admin effectue l’action.
- Le retrait bloque les récupérations futures par le service destiné aux agents. Les visuels existants et les photos déjà transmises ne sont pas rappelés.

## Photo de profil

- Une photo personnalisée remplace la photo Google partout dans Thezaurus et devient la photo fournie aux agents lorsque l’autorisation le permet.
- La personne concernée et les admins peuvent charger ou remplacer cette photo, ou rétablir la photo Google. Les droits sont vérifiés par le backend.
- Formats acceptés : JPEG, PNG et WebP ; taille maximale : 5 Mo ; aucun recadrage obligatoire.
- À chaque connexion sans photo personnalisée, la photo Google est synchronisée. Lorsqu’une photo personnalisée existe, aucune synchronisation Google ne remplace la photo ni ne met à jour la référence Google connue.
- « Utiliser ma photo Google » retire la photo personnalisée et restaure la dernière photo Google connue, y compris lorsque l’action est effectuée par un admin. La prochaine connexion actualise la photo Google.
- Sans photo disponible, afficher les initiales et retourner « photo indisponible » au service destiné aux agents.

## Écrans et architecture

- Profil personnel : gestion de la photo, retour à Google et autorisation IA.
- Gestion des utilisateurs : changement de photo et retour à Google par un admin ; aucune commande permettant de modifier l’autorisation IA d’autrui.
- Photos personnalisées dans un bucket GCS privé, restituées via le backend.
- Service backend destiné aux futurs agents : vérifier l’autorisation à chaque récupération, puis fournir la photo courante ou indiquer son absence. Distinguer le refus de l’absence de photo dans les tests.
- Le raccordement réseau, l’authentification des agents et la génération des visuels seront traités avec les futurs agents.

## Vérification attendue

- Refus par défaut ; activation et retrait par soi-même ; rejet backend des modifications d’autorisation par autrui, admin compris.
- Modification de photo autorisée pour soi-même et pour un admin, refusée aux autres utilisateurs ; autorisation IA préservée.
- Chargement des formats acceptés et rejet des fichiers invalides ou trop volumineux ; échec de chargement préservant la photo courante.
- Priorité de la photo personnalisée dans tous les affichages ; connexion Google sans écrasement ; retour à la dernière photo Google puis actualisation à la connexion suivante.
- Récupération IA refusée sans autorisation, autorisée avec photo, absence explicite sans photo et refus après retrait.
- Les mises à jour de rôles et des autres préférences préservent les champs photo et autorisation IA.

Fonctionnement et tests : [ProfilePhotos.md](ProfilePhotos.md). Configuration du bucket de production : [CloudStorage.md](CloudStorage.md).
