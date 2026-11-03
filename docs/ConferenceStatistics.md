# Statistiques par conférence

Les compteurs `submittedTalksAmount` et `acceptedTalksAmount` sont stockés sur les conférences.
La page ne lit plus les talks : une seule requête sur les conférences fournit les deux totaux,
sans N+1. Les compteurs font toujours partie de la réponse de `GET /conferences` ; le paramètre
inutilisé `includeStatistics` a été retiré du backend et du contrat OpenAPI.
Le cache navigateur et son invalidation restent inchangés.

La liste propose un tri décroissant par soumissions ou acceptations, combinable aux filtres.
Les égalités sont départagées par nom puis identifiant. Le tri travaille sur une copie filtrée
du catalogue en mémoire, sans requête supplémentaire ni modification du cache ; Réinitialiser
rétablit aussi l'ordre initial.

## Cumuls annuels

Le tableau « Cumuls par année » additionne les compteurs de toutes les conférences du catalogue,
par année de début, sans requête ni compteur persisté supplémentaire. Une conférence chevauchant
deux années n'est comptée qu'une fois. Les conférences sans année connue sont regroupées sur une
ligne séparée ; les années présentes avec zéro talk restent affichées. Un total général inclut
aussi les conférences sans date. Le tableau n'est pas affecté par les filtres ou le tri de la liste.

Le filtre de la liste propose les années disponibles, y compris les années passées, en plus de
l'année courante et de la suivante. Il permet de consulter les conférences d'une année du bilan.
Il s'agit de l'état actuel des conférences par année d'événement, pas d'un historique par date de
soumission : une modification de statut ou une suppression se répercute sur les cumuls.

- Soumissions : `SUBMITTED`, `ACCEPTED`, `REJECTED`, `DONE`.
- Acceptations : `ACCEPTED` et `DONE`.
- Brouillons et talks planifiés exclus. Un talk compte une fois, indépendamment des speakers.
- Rattachement uniquement par identifiant ; les noms libres ne sont pas rapprochés automatiquement.

## Maintenance transactionnelle

La création, la modification et la suppression d'un talk écrivent le talk et les deltas des
conférences dans la même transaction Firestore. Un déplacement peut modifier deux conférences.
Les lectures précèdent toutes les écritures. Les retries relisent les valeurs et ne doublent pas
les incréments. Supprimer un talk déjà supprimé ne décrémente rien.

`ConferenceCounters.applyDelta` centralise la préparation des changements (lectures et calculs) ;
ses mises à jour préparées sont appliquées seulement après toutes les lectures de la transaction.
Les noms des collections sont définis indépendamment dans `FirestoreCollections`, sans réécriture
du chemin d'une autre collection.

Une correction de titre ou `ACCEPTED → DONE` ne modifie pas les totaux et n'écrit aucun compteur.
Un retour à `DRAFT` retire la contribution précédente. Une conférence supprimée n'est jamais
recréée par le mécanisme de compteur. La suppression d'une conférence décrémente les contributions
converties en noms libres dans chaque lot avant de supprimer le document.

Les compteurs envoyés par un client sont ignorés à la création d'une conférence et remplacés par
les valeurs transactionnelles du serveur à l'édition. Le marqueur de migration est conservé.
Aucune Cloud Function, tâche périodique ou infrastructure supplémentaire n'est nécessaire.

## Migration des données existantes

`LegacyDataMigration` exécute `ConferenceRepository.migrateStatistics()` au démarrage, après la
normalisation des rattachements historiques. La migration est activée par défaut via
`thezaurus.migrations.startup=true` et désactivée dans les tests Quarkus sans Firestore.

Chaque conférence sans `conferenceStatisticsVersion=1` est initialisée dans sa propre transaction :
lecture de la conférence et de ses talks (projection du seul statut), remplacement des anciens
compteurs et pose du marqueur. Une conférence vide reçoit deux zéros. Les conférences en cours de
suppression sont ignorées. Les marqueurs sont projetés au scan initial pour éviter les transactions
sur les conférences déjà migrées lors des démarrages suivants.

Le marqueur ne suffit pas à valider une migration : les deux compteurs doivent être présents,
positifs ou nuls, représentables par le modèle et les acceptations ne peuvent dépasser les
soumissions. Des valeurs incomplètes ou invalides sont recalculées transactionnellement.
La normalisation des rattachements historiques conserve les identifiants des conférences en
cours de suppression ; seul le worker de suppression retire ces rattachements et leurs contributions.

Une interruption laisse les conférences déjà migrées utilisables ; le démarrage suivant reprend
les autres. Un conflit relance la transaction avec un nouveau snapshot. Si une mutation touche une
conférence non migrée, elle initialise ses compteurs avant d'appliquer le delta, dans la même
transaction : aucune fenêtre entre le scan et l'incrément. L'application refuse de démarrer si une
migration échoue. Le log indique le nombre de conférences initialisées.

**Déploiement : arrêter/drainer les anciennes révisions et tout import écrivant directement les
talks avant d'activer cette version et sa migration.** Les anciens writers ne maintiennent pas les
compteurs : une transaction de migration ne peut pas corriger leurs écritures postérieures au
marquage. Les writers de cette nouvelle version peuvent fonctionner concurremment avec la migration.
Laisser les migrations activées jusqu'à un démarrage avec zéro conférence migrée. Les imports
futurs doivent emprunter le repository transactionnel.

La migration est livrée dans le code ; aucune base distante n'a été migrée depuis ce chat.

## Validation

Tests des six statuts à la création, transitions de statut, déplacements, retrait/ajout de
rattachement, suppression répétée ou orpheline, absence d'écriture sur deltas nuls, protection des
compteurs contre les payloads clients, rejet des conférences indisponibles, retries, initialisation
pendant une mutation, migration idempotente, interruption/reprise et conflit pendant migration.
Le banc transactionnel de test applique les écritures seulement au commit et refuse toute lecture
après la première écriture. Il simule les retries ; il ne remplace pas un test de charge Firestore.

## Comparaison chiffrée des compteurs transactionnels

Sur une même période, noter :

- L : nombre réel de chargements backend après cache, tous utilisateurs confondus.
- C : nombre de conférences affichées.
- T : nombre de talks lus par l’ancienne requête, y compris les talks sans conférence reconnue.
- RΔ et WΔ : lectures et écritures supplémentaires pour maintenir les compteurs.
- r et w : prix unitaires respectifs d'une lecture et d'une écriture.

À volumes stables, hors quota gratuit, stockage, réseau, reprise initiale et retries :

```text
Ancien calcul = L × (C + T) × r
Compteurs     = L × C × r + RΔ × r + WΔ × w

Les compteurs coûtent moins si : L × T > RΔ + WΔ × (w / r)
```

Les opérations normales sur les talks sont communes aux deux options et sont donc exclues de
cette comparaison. Le cache de 60 secondes est local à chaque navigateur : il n'évite pas les
lectures des autres utilisateurs.

Exemple fictif, pas une mesure de production : C = 100, T = 1 000, L = 100 chargements/jour,
20 mutations/jour modifiant réellement les compteurs. Avec une provision de deux lectures et
deux écritures supplémentaires par mutation (déplacement ou suppression inclus, hors retries) :

| Opérations par jour | Ancien calcul | Compteurs retenus |
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

Si T est faible et les mutations très nombreuses, l’ancienne option peut coûter moins. Le coût
monétaire marginal peut aussi être nul sous le quota gratuit, quand la base y est éligible.
Pour chiffrer en euros, il faut la localisation de la base, les tarifs contractuels et les
volumes réels. La migration ajoute un coût ponctuel à amortir.

## Captures de l'interface

Captures du vrai front de la branche, le 3 novembre 2026, avec réponses API et session de
démonstration interceptées dans le navigateur. Docker étant arrêté, elles ne constituent pas
une validation de bout en bout contre Firestore. Les chiffres sont fictifs et ne viennent pas
de la production. Les compteurs sont dérivés des talks du [scénario de démonstration](screenshots/issue-196/scenario.json),
avec 24 soumissions dont 9 acceptations, 12 dont 5, une soumission en attente et une conférence vide.
Aucun code de l'interface n'a été modifié pour ces captures.

- [Bureau, 1440 × 1000](screenshots/issue-196/conferences-desktop.png).
- [Mobile, 390 × 1100](screenshots/issue-196/conferences-mobile.png).
- [Filtre 2027 et compteurs à zéro](screenshots/issue-196/conferences-zero-filter.png).

Vérifications automatisées de capture : compteurs pluriels et singuliers visibles, filtre de
l'année fonctionnel, zéro visible, absence de débordement horizontal en mobile et exactement
un appel `/conferences`, sans nouvel appel lors des filtres. La liste et les sélecteurs partagent
désormais la même entrée de cache ; les deux totaux sont toujours présents dans le catalogue.

Références : [facturation Firestore](https://firebase.google.com/docs/firestore/pricing),
[agrégations Firestore](https://firebase.google.com/docs/firestore/query-data/aggregation-queries).
Voir aussi les [agrégations à l'écriture](https://firebase.google.com/docs/firestore/solutions/aggregation)
et la [contention transactionnelle](https://firebase.google.com/docs/firestore/transaction-data-contention).
