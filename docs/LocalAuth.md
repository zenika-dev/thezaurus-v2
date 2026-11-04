# Authentification locale sans Google

Mode explicitement activé, uniquement avec `next dev`. Les builds production et les tests ne proposent jamais le provider local. L'API doit être sur une URL HTTP de bouclage (`localhost`, `127.0.0.1` ou `::1`).

Dans `front/.env.local` (non committé) :

```dotenv
LOCAL_AUTH_ENABLED=true
API_URL=http://localhost:8088
NEXTAUTH_URL=http://localhost:3008
NEXTAUTH_SECRET=<secret local aléatoire>
```

Choisir un port frontend libre par worktree et adapter `NEXTAUTH_URL` et la commande ci-dessous ensemble. Garder un secret local aléatoire, propre à ce frontend, et utiliser exactement la même origine dans le navigateur. Aucun identifiant Google ni callback OAuth supplémentaire n'est nécessaire. Ne pas utiliser de variable `NEXT_PUBLIC_*` pour activer ce mode.

L'API de développement doit utiliser `MOCK_AUTH=true`, le profil `dev` et Firestore émulé. Ce mock API existant donne l'identité `dev@zenika.com`, avec les rôles ADMIN et CONSULTANT ; le frontend reprend les rôles reçus sans en attribuer lui-même. Le contrôle de l'identité ne prouve pas la configuration du stockage : vérifier les émulateurs avant toute utilisation. Ne jamais pointer ce mock vers des données de production ni exposer ses ports publiquement.

Lors d'un audit avec services partagés, commencer par `docker ps`. Réutiliser l'API mock disponible (par exemple `thezaurus-ui8-api`, publiée uniquement sur `127.0.0.1:8088`) et Firestore/GCS/Mailpit ; ne pas redémarrer ou reconfigurer les services d'un autre worktree. Vérifier la configuration sans afficher les autres variables ni secrets :

```powershell
docker inspect thezaurus-ui8-api --format '{{range .Config.Env}}{{println .}}{{end}}' |
  Select-String '^(MOCK_AUTH|QUARKUS_PROFILE|FIRESTORE_HOST_OVERRIDE|GCS_EMULATOR_HOST|SMTP_MOCK|FEEDBACK_REMINDER_ENABLED)='
curl.exe --fail http://localhost:8088/api/me
```

Attendre `MOCK_AUTH=true`, `QUARKUS_PROFILE=dev`, un `FIRESTORE_HOST_OVERRIDE` pointant vers l'émulateur, GCS émulé, `SMTP_MOCK=true` et `FEEDBACK_REMINDER_ENABLED=false`. `/api/me` doit répondre sans en-tête Google avec `dev@zenika.com` et un tableau de rôles connus. Une API sans mock doit refuser la connexion locale. Si aucune API mock sûre n'est disponible, démarrer seulement une instance API dédiée, avec l'image et le réseau locaux déjà disponibles, reliée aux mêmes émulateurs et publiée sur une boucle locale libre ; ne pas démarrer une deuxième stack Compose complète.

Lancer `npm ci` puis `npm run dev -- --port 3008` depuis `front`. Ouvrir exactement `http://localhost:3008`, puis « Connexion locale (développement) ». Le provider vérifie que `/api/me` renvoie `dev@zenika.com` et reprend ses rôles ; aucune identité Google n'est fabriquée. La session utilise les cookies signés NextAuth habituels.

Le frontend refuse les API distantes, HTTPS, les URL avec identifiants/chemin/query et les redirections de `/api/me`. Une réponse invalide, un rôle inconnu ou une API indisponible refuse la connexion. Les appels métier continuent à utiliser le proxy backend existant ; aucun faux jeton Google n'est ajouté.

### Frontend dans Docker

Privilégier le frontend sur l'hôte pour les worktrees. Dans un conteneur, `localhost` désigne ce conteneur, pas Windows ni l'API voisine. `API_URL=http://host.docker.internal:8088` ou un nom de service Docker est volontairement refusé par la garde locale.

Si le frontend doit tourner dans Docker, conserver `API_URL=http://127.0.0.1:8088` et établir un relais HTTP **lié uniquement à `127.0.0.1:8088` dans ce conteneur**, vers le service mock du réseau Docker (par exemple `thezaurus-ui8-api:8080`). Le relais doit avoir une destination fixe contrôlée, sans cible fournie par la requête ; il n'est ni un proxy public ni une configuration de production. Publier seulement le port frontend sur `127.0.0.1`, choisir son `NEXTAUTH_URL` externe et arrêter le relais avec ce frontend. Ce réglage de lancement est local au worktree : ne pas élargir la liste des hôtes autorisés dans le code et ne pas modifier les services partagés. Les scripts de relais et `.env.local` restent hors Git.

### Vérifier et désactiver

Après connexion, `/api/auth/session` expose l'identité locale et ses rôles ; les pages membres deviennent accessibles selon ces rôles. Utiliser la déconnexion normale puis se reconnecter avec le bouton local pour vérifier les deux parcours. Plusieurs frontends sur `localhost` partagent le domaine des cookies malgré leurs ports : utiliser des contextes navigateur isolés pour éviter les collisions entre secrets/sessions de worktrees.

Pour désactiver : supprimer `LOCAL_AUTH_ENABLED` (ou le mettre à `false`) et redémarrer. Les sessions locales existantes sont alors refusées. Ne jamais activer le mock de l'API sur un environnement exposé ou contenant des données de production.

Après redémarrage, `/api/auth/providers` ne doit plus contenir `local` et le bouton doit disparaître. Un cookie local encore présent perd ses rôles et est signalé expiré ; le garde existant déconnecte l'utilisateur. `next build` puis `next start` ne proposent jamais ce provider, même si `LOCAL_AUTH_ENABLED=true`. Le mode Google existant reste disponible. Arrêter son frontend et fermer son navigateur de test dès la fin ; laisser les services partagés actifs.

### Limites de l'API de démonstration partagée

L'image mock employée lors de l'audit peut être plus ancienne que le code courant : `/api/me/profile` et les routes photo peuvent répondre 404. Le mock ne crée pas nécessairement de profil utilisateur persistant. Cela ne bloque pas la session ni les listes membres ; valider les fonctions de profil nécessite une API locale compatible et des données d'émulateur adaptées. Une erreur de sérialisation du champ agence d'un article de démonstration a également été rencontrée : elle relève des données/API, pas de la connexion. Ces limites ne sont pas corrigées par ce socle.

### PR enfants

Tant que ce socle n'est pas fusionné, créer les nouvelles branches depuis sa branche publiée et cibler cette branche dans leur PR. Pour une branche existante contenant une copie cherry-pick d'UI8, sauvegarder son travail puis rebaser uniquement ses commits métier sur le socle (par exemple `git rebase --onto origin/feat/authentification-locale-sans-google <ancien-commit-auth> <branche-enfant>` si ce commit précède tous les commits métier). Si les changements sont encore non committés ou si l'ordre diffère, les isoler d'abord puis reporter seulement ces changements. Ne pas conserver deux variantes de l'auth ni écraser les corrections de landing propres aux sujets UI. Vérifier le diff contre la branche socle avant publication. Après fusion du socle dans `main`, mettre à jour les branches et leur base de PR vers `main`.
