# ADR 0001 — Pub/Sub pour les rappels post-talk en cas de fort volume

- **Statut** : proposé
- **Date** : 2026-11-10

## Contexte

Les rappels post-talk sont envoyés par un batch que Cloud Scheduler déclenche chaque jour
ouvré à 9h. L'API fait tout dans la même requête : sélection des talks, envoi email
(Mailgun) et Slack, puis marquage `feedbackReminderSent`.

C'est suffisant aujourd'hui, mais avec plus de volume :

- le batch peut dépasser la limite de 300 s du job ;
- un envoi en échec n'est rejoué que le lendemain ;
- chaque nouveau canal s'ajoute dans le même batch.

## Décision

On garde le batch actuel tant que le volume est faible.

Si le volume devient important, on envisagera **Google Cloud Pub/Sub** pour traiter les
envois de façon asynchrone plutôt que dans un seul batch. Les détails de mise en œuvre
seront définis à ce moment-là.

## Conséquences

- ✅ Meilleure tenue en charge, gestion des échecs facilitée, canaux plus faciles à ajouter.
- ✅ Service managé GCP, sans serveur à maintenir.
- ⚠️ Une brique d'infrastructure supplémentaire à configurer et à suivre.
- ⚠️ Les envois devront rester sans doublon.
