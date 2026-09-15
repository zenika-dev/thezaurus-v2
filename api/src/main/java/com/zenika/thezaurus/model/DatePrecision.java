package com.zenika.thezaurus.model;

/**
 * Précision de la période d'une conférence. Non déductible des bornes : « mars 2026 » (dates non
 * arrêtées) et un événement du 1er au 31 mars ont les mêmes bornes mais un affichage différent.
 */
public enum DatePrecision {
    /** Les bornes sont les jours réels de la conférence. */
    DAY,
    /** Seul le mois est connu : les bornes couvrent le mois et ne doivent pas être affichées. */
    MONTH
}
