# Rappels après un talk

Le job vérifie chaque jour à 09:00 (`Europe/Paris`) les talks `ACCEPTED` ou `DONE`
dont le replay ou l’audience manque. Une audience de zéro est renseignée.
Il utilise uniquement la date propre du talk (`date`).
La date doit être strictement antérieure au jour courant ; une date absente
ou invalide est ignorée.

L’envoi de rappels supporte deux canaux configurables de manière indépendante :
- **Email** : message collectif envoyé aux speakers du talk ayant activé la notification par email. Modèle géré dans **Administration → Templates de messages → Email de rappel**.
- **Slack** : message direct individuel envoyé à chaque speaker du talk ayant activé la notification Slack (`notifiesOnSlack == true`), avec mention personnalisée (`recipientMention` / `<@slackUserId>`). Modèle géré dans **Administration → Templates de messages → Rappel Slack**.

Avant l’envoi, les destinataires sont filtrés selon les préférences actuelles de leur compte dans la collection `users`. Un compte ou une préférence absent vaut refus. La prévisualisation dans l'administration affiche tous les speakers potentiels avant ce filtrage.

Pour Slack, le `slackUserId` est utilisé s'il est déjà connu, ou résolu à la volée via l'API Slack `users.lookupByEmail` et persisté sur l'utilisateur.

Si au moins un des deux canaux aboutit avec succès, le champ `feedbackReminderSent` est mis à `true`. Les prochaines exécutions ignorent le talk, même s’il est ensuite édité. Les anciens documents sans ce champ sont considérés comme non relancés. Un échec d’envoi sur tous les canaux laisse le talk éligible pour le lendemain et n’arrête pas les suivants.

## SMTP local avec Docker Compose

`docker compose up -d --build` démarre aussi Mailpit. Les emails sont capturés
dans son interface : [http://localhost:8025](http://localhost:8025).
Le compte SMTP de test est `thezaurus` / `thezaurus-local` ; il est défini dans
`.env` et `.env-template` par `SMTP_USERNAME` et `SMTP_PASSWORD`.
Mailpit et l’API reçoivent les mêmes identifiants depuis ce fichier.

Dans Docker, l’API se connecte à `SMTP_HOST=mailpit`, `SMTP_PORT=1025`.
Depuis la machine hôte, le serveur est accessible sur `localhost:1025`.
`MAILPIT_UI_PORT` et `MAILPIT_SMTP_PORT` changent les ports publiés sur la machine,
sans changer le port SMTP interne. Les ports Mailpit sont limités à `127.0.0.1`.
L’authentification SMTP locale est sans TLS ; aucun relais externe n’est configuré.

L’override transmet explicitement les variables du `.env` à l’API. Les properties
du profil `dev` les lisent, notamment `SMTP_MOCK=false`, qui active la livraison
à Mailpit au lieu de la simulation Quarkus. `FEEDBACK_REMINDER_ENABLED=true`
active les rappels ; le batch se déclenche à la main (voir ci-dessous).
`FEEDBACK_REMINDER_TIME_ZONE` sert à déterminer les talks passés. Le modèle administrable et les préférences email
restent nécessaires. Les messages Mailpit sont éphémères lors d’une recréation.

### Déclencher le batch en local

En production, le batch est déclenché par Cloud Scheduler, qui appelle
`POST /api/internal/feedback-reminders` avec un token OIDC de son compte de service.
En local, il n’y a pas de déclenchement automatique : le profil `dev` active
`thezaurus.feedback-reminder.local-trigger`, qui accepte l’appel sans token.

```bash
curl -i -X POST http://localhost:8080/api/internal/feedback-reminders
```

L’appel se fait directement sur l’API (`:8080`) : la route du front exige un token.
Une réponse `204` signifie que le batch a tourné ; les emails arrivent dans Mailpit
(aucun log s’il n’y a aucun talk éligible).

Après modification de `.env`, lancer `docker compose up -d api mailpit` pour
recréer les conteneurs concernés ; un simple `restart` ne recharge pas leurs variables.

### Données nécessaires dans l’émulateur

Le batch sort sans rien envoyer (et souvent sans log) si l’une de ces conditions manque :

1. **Modèle d’email** : **Administration → Templates de messages → Email de rappel**,
   sujet et corps non vides (document `dev_app_configuration/reminder_template`).
2. **Préférence du speaker** : notification email activée sur sa page profil
   (`emailNotificationsEnabled = true` dans `users/<email>`). Une préférence jamais
   exprimée (`null`) vaut refus.
3. **Talk éligible** : speaker = ce compte, `status` `ACCEPTED` ou `DONE`, `visibility`
   `PUBLIC`, `feedbackReminderSent = false`, `date` **strictement passée** (`AAAA-MM-JJ`),
   et pas à la fois un replay et une audience renseignés.

Les données sont consultables et modifiables dans l’Emulator UI : http://localhost:4000
(projet `local-dev`, base `(default)`, collections préfixées par `dev_` sauf `users`).

Un talk ne reçoit qu’un rappel : après un envoi, remettre `feedbackReminderSent` à
`false` (booléen) pour rejouer le test.

**L’émulateur ne persiste rien** : à chaque redémarrage de son conteneur, modèle,
utilisateurs et talks sont perdus. Pour reconstruire l’API sans le relancer :

```bash
docker compose up -d --build --no-deps api
```

### Tester l’envoi réel via Mailgun en local

Par défaut l’envoi local passe par Mailpit. Pour recevoir un vrai email, renseigner dans
`.env` (jamais commité) :

```bash
MAILGUN_SENDING_KEY=<Domain Sending Key>
MAILGUN_DOMAIN=m.zenika.com
SMTP_FROM=thezaurus-noreply@m.zenika.com
```

puis `docker compose up -d --no-deps api` et le `curl` ci-dessus. Le log doit indiquer
`Email de rappel envoyé avec succès via mailgun (m.zenika.com)` ; `via smtp` signifie que
les variables Mailgun ne sont pas arrivées dans le conteneur. Vider ces variables pour
revenir à Mailpit. Le profil `dev` reste actif : c’est lui qui autorise le `curl` sans token.

### Diagnostic rapide

```bash
docker logs thezaurus-api --since 5m 2>&1 | grep -i -E "rappel|reminder"
```

| Symptôme | Cause probable |
|---|---|
| `204`, aucun log | rappels désactivés, modèle absent, aucun talk éligible ou préférence email non activée |
| `Modèle d'email de rappel invalide.` | syntaxe Mustache ou lien autre que `{{talksUrl}}` |
| `Échec de l'envoi d'email … Connection refused` | Mailpit arrêté, ou Mailgun non configuré alors qu’on l’attendait |
| `Mailgun a répondu 401` | clé Mailgun ou domaine incorrect |
| Lien `localhost:3000` dans l’email | `THEZAURUS_PUBLIC_URL` non défini (normal en local) |

## Configuration en production et hors Docker Compose

Les rappels sont désactivés par défaut. Vous pouvez renseigner la configuration soit via les variables d'environnement (`.env` ou variables système dans votre orchestration), soit via un fichier `config/application.properties` non versionné relatif au répertoire de lancement de l'API.

### Via variables d'environnement (recommandé en conteneur / Cloud Run)

```bash
FEEDBACK_REMINDER_ENABLED=true
FEEDBACK_REMINDER_TIME_ZONE=Europe/Paris
THEZAURUS_PUBLIC_URL=https://thezaurus.zenika.com

# Prod : API HTTP Mailgun (région EU), authentifiée par une Domain Sending Key
SMTP_FROM=thezaurus-noreply@m.zenika.com
MAILGUN_DOMAIN=m.zenika.com
MAILGUN_BASE_URL=https://api.eu.mailgun.net
MAILGUN_SENDING_KEY=votre-sending-key
```

Si `MAILGUN_SENDING_KEY` ou `MAILGUN_DOMAIN` est vide, l'envoi passe par le SMTP
`quarkus-mailer` (`SMTP_*`) : c'est le cas en local avec Mailpit. Une Domain Sending Key
n'est acceptée que par l'API HTTP Mailgun, pas en SMTP. Le transport utilisé apparaît dans
le log `Email de rappel envoyé avec succès via mailgun (m.zenika.com) …`.

Sur Cloud Run (`.github/workflows/_deploy.yml`), `MAILGUN_DOMAIN` et `SMTP_FROM` viennent des
vars de l'environnement GitHub, la clé du secret Secret Manager `thezaurus-mailgun-sending-key`.

### Via properties Quarkus (`config/application.properties`)

```properties
thezaurus.feedback-reminder.enabled=true
thezaurus.feedback-reminder.time-zone=Europe/Paris
thezaurus.public-url=https://thezaurus.zenika.com

quarkus.mailer.host=smtp.sendgrid.net
quarkus.mailer.port=587
quarkus.mailer.from=thezaurus@zenika.com
quarkus.mailer.username=apikey
quarkus.mailer.password=votre-token-secret
quarkus.mailer.login=REQUIRED
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.mock=false
```

Hors Compose, le profil `dev` simule l’envoi par défaut (`SMTP_MOCK=true`).
Le profil `test` conserve toujours `quarkus.mailer.mock=true` et n'utilise jamais Mailgun ;
les tests déclenchent le service explicitement. Une simulation réussie marque le talk
dans la base de développement : utiliser une base isolée.

## Exécution en production

Le batch est déclenché par **Cloud Scheduler** (job `talk-feedback-reminder` en prod,
`talk-feedback-reminder-staging` en staging), géré par Terraform dans
`infra/modules/environment/scheduler.tf`. Le job appelle
`POST <NEXTAUTH_URL>/api/internal/feedback-reminders` avec un token OIDC signé pour
`SCHEDULER_SERVICE_ACCOUNT` ; la route du front le relaie à l'API, qui vérifie la signature,
l'audience (`FEEDBACK_REMINDER_AUDIENCE`) et l'email du compte (`FEEDBACK_REMINDER_INVOKER`).
Le job n'a pas de retry : un batch interrompu est rattrapé au passage suivant.

L'envoi est synchrone dans la requête (limite `--attempt-deadline=300s`). Les appels
concurrents sont ignorés dans l'instance ; cette protection suppose une seule instance
d'API (`maxScale: 1` dans `_deploy.yml`).

### Checklist de mise en production

1. **Secret Manager** : secret `thezaurus-mailgun-sending-key` (Domain Sending Key Mailgun),
   lisible par `RUNTIME_SERVICE_ACCOUNT` (`roles/secretmanager.secretAccessor`).
2. **Vars de l'environnement GitHub `production`** :
   - `FEEDBACK_REMINDER_ENABLED=true` (sinon le batch ne fait rien) ;
   - `MAILGUN_DOMAIN` (ex. `m.zenika.com`) et `SMTP_FROM` (ex. `thezaurus-noreply@m.zenika.com`) ;
   - `SCHEDULER_SERVICE_ACCOUNT`, `FEEDBACK_REMINDER_AUDIENCE` et `SCHEDULER_CRON`
     (ex. `0 9 * * 1-5`, fuseau `Europe/Paris`).
3. **Index composite Firestore** sur `<prefix>_talks`, tous ascendants :
   `feedbackReminderSent`, `status`, `visibility`, `date`. Sans lui, le batch échoue avec
   `FAILED_PRECONDITION: The query requires an index` (le lien de création est dans le log).
   L'émulateur n'exige pas d'index : l'erreur n'apparaît qu'en prod.
4. **Modèle d'email** : à créer dans **Administration → Templates de messages → Email de
   rappel** (document `<prefix>_app_configuration/reminder_template`). S'il est créé à la
   main dans la console, `revision` doit être un entier ou absent.
5. **Release depuis `main`** avec un commit `feat:`/`fix:` touchant `api/` : sans nouvelle
   version, l'image API existante est redéployée et le nouveau code n'est pas livré.

Un talk ne reçoit qu'un rappel (`feedbackReminderSent`). Pour rejouer un test, remettre ce
champ à `false` (booléen) sur le talk.

SMTP et Firestore ne partagent pas de transaction : si l’email est accepté puis
que l’écriture du marqueur échoue (ou que le processus s’arrête), le prochain
passage peut renvoyer l’email. Les erreurs sont journalisées avec l’identifiant
du talk ; le marqueur n’est jamais enregistré avant l’envoi.

La configuration du transport suit la [référence du mailer Quarkus](https://quarkus.io/guides/mailer-reference/).
