# Rappels après un talk

Le job vérifie chaque jour à 09:00 (`Europe/Paris`) les talks `ACCEPTED` ou `DONE`
dont le replay ou l’audience manque. Une audience de zéro est renseignée.
Il utilise la date du talk ou, à défaut, la fin de la période de conférence.
La date doit être strictement antérieure au jour courant ; une date absente,
invalide ou une période inversée est ignorée.

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

## Configuration dans les properties

Les rappels sont désactivés par défaut. Renseigner les propriétés SMTP dans
`api/src/main/resources/application.properties`, ou dans un fichier externe
`config/application.properties` relatif au répertoire de lancement de l’API.
Conserver les identifiants réels dans le fichier externe non versionné.

```properties
thezaurus.feedback-reminder.enabled=true
thezaurus.feedback-reminder.cron=0 0 9 * * ?
thezaurus.feedback-reminder.time-zone=Europe/Paris
thezaurus.public-url=https://thezaurus.example.com

quarkus.mailer.host=smtp.example.com
quarkus.mailer.port=587
quarkus.mailer.from=thezaurus@example.com
quarkus.mailer.username=thezaurus@example.com
quarkus.mailer.password=REPLACE_LOCALLY
quarkus.mailer.login=REQUIRED
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
```

En profils `dev` et `test`, `quarkus.mailer.mock=true` simule l’envoi. Aucun
serveur SMTP n’est contacté. Le scheduler est désactivé en test ; les tests
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
