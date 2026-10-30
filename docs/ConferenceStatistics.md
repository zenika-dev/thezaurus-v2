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

Le code actuel minimise la complexité et les écritures, pas nécessairement la facture.
Pour une application davantage consultée que modifiée, **les compteurs transactionnels sur les
conférences sont le meilleur choix de coût récurrent**. Ils évitent également le N+1 : une seule
requête sur les conférences fournit les deux compteurs. Cette comparaison ne remplace pas encore
le calcul actuel par ces compteurs.

## Comparaison chiffrée des compteurs transactionnels

Sur une même période, noter :

- L : nombre réel de chargements backend après cache, tous utilisateurs confondus.
- C : nombre de conférences affichées.
- T : nombre de talks lus par la requête actuelle, y compris les talks sans conférence reconnue.
- RΔ et WΔ : lectures et écritures supplémentaires pour maintenir les compteurs.
- r et w : prix unitaires respectifs d'une lecture et d'une écriture.

À volumes stables, hors quota gratuit, stockage, réseau, reprise initiale et retries :

```text
Actuel    = L × (C + T) × r
Compteurs = L × C × r + RΔ × r + WΔ × w

Les compteurs coûtent moins si : L × T > RΔ + WΔ × (w / r)
```

Les opérations normales sur les talks sont communes aux deux options et sont donc exclues de
cette comparaison. Le cache de 60 secondes est local à chaque navigateur : il n'évite pas les
lectures des autres utilisateurs.

Exemple fictif, pas une mesure de production : C = 100, T = 1 000, L = 100 chargements/jour,
20 mutations/jour modifiant réellement les compteurs. Avec une provision de deux lectures et
deux écritures supplémentaires par mutation (déplacement ou suppression inclus, hors retries) :

| Opérations par jour | Calcul actuel | Compteurs |
| --- | ---: | ---: |
| Lectures de la page | 110 000 | 10 000 |
| Lectures supplémentaires de maintenance | 0 | au plus 40 |
| Écritures supplémentaires de maintenance | 0 | au plus 40 |

Pour un rapport illustratif w/r = 3, les compteurs représentent au plus 10 160 unités de coût
de lecture contre 110 000 : environ **91 % de coût d'opérations en moins sur ce périmètre**.
Ils sont déjà rentables dès un chargement/jour dans cet exemple : 1 000 lectures évitées contre
160 unités de maintenance. Cela ne signifie pas une baisse de 91 % de la facture globale.
Le rapport 3 correspond notamment aux tarifs standards affichés pour Iowa (0,03 USD/100 000
lectures, 0,09 USD/100 000 écritures), consultés le 30 octobre 2026 ; ce n'est pas une estimation
du tarif de la base déployée. La région Cloud Run ne prouve pas celle de Firestore.

Si T est faible et les mutations très nombreuses, l'option actuelle peut coûter moins. Le coût
monétaire marginal peut aussi être nul sous le quota gratuit, quand la base y est éligible.
Pour chiffrer en euros, il faut la localisation de la base, les tarifs contractuels et les
volumes réels. La migration ajoute un coût ponctuel à amortir.

## Ce que demanderait une implémentation fiable

Le dépôt dispose déjà d'une transaction dans `TalkRepository.save` : pas besoin de Cloud
Function, de tâche périodique ni de compteur distribué pour ce niveau d'usage.

1. Stocker les deux totaux sur la conférence, protégés contre les valeurs envoyées par les clients.
   `acceptedTalksAmount` est actuellement exclu de la persistance Firestore : il faudrait retirer
   cette exclusion. Le compteur `submittedTalksAmount` historique doit être recalculé.
2. Dans la transaction du talk, calculer la contribution avant/après par identifiant et statut.
   Appliquer uniquement les deltas non nuls, avec les deux champs dans la même écriture par
   conférence. `ACCEPTED → DONE` et une correction de titre ne nécessitent aucune écriture de compteur.
3. Pour un déplacement, décrémenter l'ancienne conférence et incrémenter la nouvelle dans la
   même transaction. Lire tous les documents nécessaires avant la première écriture ; conserver
   les contrôles d'autorisation et la marque `deleting`.
4. Remplacer la suppression directe du talk par une suppression transactionnelle avec décrément.
   La lecture préalable du talk doit se faire dans la transaction pour résister à un double appel
   et aux modifications concurrentes.
5. Modifier `ConferenceRepository.update` : son `transaction.set(reference, conference)` actuel
   peut écraser les compteurs. Conserver les valeurs du serveur ou mettre à jour les seuls champs
   éditables. Une création de conférence initialise les compteurs à zéro.
6. La suppression de conférence convertit déjà les rattachements par lots avant de supprimer le
   document. Ne pas recréer la conférence via une écriture de compteur concurrente. Auditer aussi
   `migrateLegacyConferences`, les imports et toute écriture directe hors repository.
7. Initialiser les compteurs existants une seule fois sous suspension des mutations, ou par un
   protocole de migration versionné. Un scan suivi de `set` pendant que les talks changent peut
   perdre des incréments. Un champ absent ne doit pas être assimilé silencieusement à un zéro fiable.
8. Tester créations, suppressions répétées, changements de statut, déplacements, modifications
   concurrentes, retries, reprise de migration et édition des métadonnées de conférence.

La conférence devient un document partagé entre les écritures des talks associés : les
transactions peuvent être relancées en cas de contention. Ne distribuer les compteurs que si
des mesures montrent ce besoin ; lire des fragments à chaque affichage annulerait une partie
du bénéfice recherché.

## Captures de l'interface

Captures du vrai front de la branche, le 30 octobre 2026, avec réponses API et session de
démonstration interceptées dans le navigateur. Docker étant arrêté, elles ne constituent pas
une validation de bout en bout contre Firestore. Les chiffres sont fictifs et ne viennent pas
de la production. Aucun code de l'interface n'a été modifié pour ces captures.

- [Bureau, 1440 × 1000](screenshots/issue-196/conferences-desktop.png).
- [Mobile, 390 × 1100](screenshots/issue-196/conferences-mobile.png).
- [Filtre 2027 et compteurs à zéro](screenshots/issue-196/conferences-zero-filter.png).

Vérifications automatisées de capture : compteurs pluriels et singuliers visibles, filtre de
l'année fonctionnel, zéro visible, absence de débordement horizontal en mobile et exactement
un appel groupé `/conferences?includeStatistics=true`, sans nouvel appel lors des filtres.

Références : [facturation Firestore](https://firebase.google.com/docs/firestore/pricing),
[agrégations Firestore](https://firebase.google.com/docs/firestore/query-data/aggregation-queries).
Voir aussi les [agrégations à l'écriture](https://firebase.google.com/docs/firestore/solutions/aggregation)
et la [contention transactionnelle](https://firebase.google.com/docs/firestore/transaction-data-contention).
