# Statistiques par conférence

La page des conférences appelle `GET /conferences?includeStatistics=true` une seule fois.
Chaque carte affiche les talks soumis et acceptés, regroupés par identifiant de conférence.
Deux conférences homonymes restent distinctes. Les noms libres sans identifiant ne sont pas
rattachés automatiquement à une conférence.

- Soumissions : `SUBMITTED`, `ACCEPTED`, `REJECTED`, `DONE`.
- Acceptations : `ACCEPTED`, `DONE`.
- `DRAFT` et `PLANNED` sont exclus. Un talk compte une fois, quel que soit son nombre de speakers.

Il s'agit des statuts actuels, pas d'un historique : repasser un talk en brouillon le retire des
statistiques. Modifier son rattachement le déplace vers sa nouvelle conférence. Les compteurs
historiques saisis sur les conférences ne sont pas utilisés pour ce calcul.

## Requêtes et coût

Le serveur lit les conférences puis effectue **une requête groupée** sur les talks aux quatre
statuts concernés, projetée sur `conference.id` et `status`. Il ne désérialise pas les talks
complets et ne résout aucune conférence depuis ces talks : pas de N+1. Une liste de conférences
vide évite même cette seconde requête. Les formulaires utilisent la liste ordinaire, sans calcul.
Le cache React Query existant conserve les résultats pendant 60 secondes ; les mutations de
talks invalident les conférences, y compris en cas de changement de statut ou de rattachement.

Pour C conférences et T talks aux statuts concernés, un chargement sans cache lit environ C + T
documents (avec le minimum de facturation des requêtes vides). La projection réduit le transfert,
**pas le nombre de lectures facturées**. Aucune écriture, migration, tâche périodique ou nouvel
index composite n'est nécessaire.

Choix comparés :

| Option | Avantage | Coût / compromis |
| --- | --- | --- |
| Lecture groupée projetée (retenue) | Deux requêtes, données actuelles, maintenance simple | Lectures proportionnelles aux talks concernés |
| Deux `count()` par conférence | Les agrégations peuvent coûter moins de lectures | 2C requêtes supplémentaires : N+1 |
| Compteurs matérialisés et transactionnels | Lecture des seuls documents de conférence | Écritures supplémentaires, reprise des données, synchronisation de tous les chemins de modification et suppression |

Le choix retenu minimise la complexité et les écritures tout en supprimant le N+1. Il ne prétend
pas minimiser la facture pour tout volume possible : si les consultations deviennent très
fréquentes ou le nombre de talks très élevé, mesurer C, T et les taux de lecture/écriture avant
de passer à des compteurs transactionnels. Ne pas confondre nombre de requêtes et coût facturé.

Références : [facturation Firestore](https://firebase.google.com/docs/firestore/pricing),
[agrégations Firestore](https://firebase.google.com/docs/firestore/query-data/aggregation-queries).
