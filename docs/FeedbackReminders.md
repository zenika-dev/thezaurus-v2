# Rappels après un talk

Le job vérifie chaque jour à 09:00 (`Europe/Paris`) les talks `ACCEPTED` ou `DONE`
dont le replay ou l’audience manque. Une audience de zéro est renseignée.
Il utilise uniquement la date propre du talk (`date`).
La date doit être strictement antérieure au jour courant ; une date absente
ou invalide est ignorée.

L’email est collectif : son sujet, son corps HTML et ses destinataires potentiels
sont produits par le même moteur que la prévisualisation administrateur (PR 138).
Il faut enregistrer le modèle dans **Administration → Templates de messages**.
Un modèle vide ou invalide et un talk sans adresse email n’entraînent aucun envoi.

Avant l’envoi, les destinataires sont filtrés selon les préférences actuelles de
leur compte dans la collection `users`. Seuls ceux ayant activé les notifications
par email reçoivent le rappel ; un compte ou une préférence absent vaut refus.
La prévisualisation affiche tous les speakers potentiels avant ce filtrage.
Sans destinataire ayant accepté les emails, aucun envoi ni marquage n’a lieu.
La préférence Slack est enregistrée, mais aucun rappel Slack n’est envoyé.

Après acceptation par le serveur SMTP, seul le champ `feedbackReminderSent` est
mis à `true`. Les prochaines exécutions ignorent le talk, même s’il est ensuite
édité. Les anciens documents sans ce champ sont considérés comme non relancés.
Un échec d’envoi laisse le talk éligible pour le lendemain et n’arrête pas les suivants.

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
active les rappels, à 09:00 Paris selon `FEEDBACK_REMINDER_CRON` et
`FEEDBACK_REMINDER_TIME_ZONE`. Le modèle administrable et les préférences email
restent nécessaires. Les messages Mailpit sont éphémères lors d’une recréation.

Après modification de `.env`, lancer `docker compose up -d api mailpit` pour
recréer les conteneurs concernés ; un simple `restart` ne recharge pas leurs variables.

## Configuration en production et hors Docker Compose

Les rappels sont désactivés par défaut. Vous pouvez renseigner la configuration soit via les variables d'environnement (`.env` ou variables système dans votre orchestration), soit via un fichier `config/application.properties` non versionné relatif au répertoire de lancement de l'API.

### Via variables d'environnement (recommandé en conteneur / Cloud Run)

```bash
FEEDBACK_REMINDER_ENABLED=true
FEEDBACK_REMINDER_CRON="0 0 9 * * ?"
FEEDBACK_REMINDER_TIME_ZONE=Europe/Paris
THEZAURUS_PUBLIC_URL=https://thezaurus.zenika.com

SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_FROM=thezaurus@zenika.com
SMTP_USERNAME=apikey
SMTP_PASSWORD=votre-token-secret
SMTP_LOGIN=REQUIRED
SMTP_START_TLS=REQUIRED
SMTP_TLS=false
SMTP_MOCK=false
```

### Via properties Quarkus (`config/application.properties`)

```properties
thezaurus.feedback-reminder.enabled=true
thezaurus.feedback-reminder.cron=0 0 9 * * ?
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
Le profil `test` conserve toujours `quarkus.mailer.mock=true`. Le scheduler est désactivé en test ; les tests
déclenchent le service explicitement. Une simulation réussie marque le talk
dans la base de développement : utiliser une base isolée.

## Exécution en production

Activer le job sur une seule instance de l’API, maintenue en fonctionnement avec
du CPU disponible à l’heure prévue. En particulier, un service Cloud Run réduit
à zéro ou suspendu hors requêtes ne garantit pas ce déclenchement quotidien.
Les exécutions simultanées sont empêchées dans cette instance ; le scheduler
ne coordonne pas plusieurs instances. Voir la [référence du scheduler Quarkus](https://quarkus.io/guides/scheduler-reference/).

SMTP et Firestore ne partagent pas de transaction : si l’email est accepté puis
que l’écriture du marqueur échoue (ou que le processus s’arrête), le prochain
passage peut renvoyer l’email. Les erreurs sont journalisées avec l’identifiant
du talk ; le marqueur n’est jamais enregistré avant l’envoi.

La configuration du transport suit la [référence du mailer Quarkus](https://quarkus.io/guides/mailer-reference/).
