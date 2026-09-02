package com.zenika.thezaurus.model;

/**
 * Précision avec laquelle la période d'une conférence est connue.
 *
 * <p>La distinction n'est pas déductible des bornes : une conférence annoncée « mars 2026 » couvre
 * le mois entier faute de dates arrêtées, alors qu'une conférence du 1er au 31 mars dure réellement
 * un mois. Sans ce champ, les deux cas seraient indiscernables et affichés de la même façon.
 */
public enum DatePrecision {
    /** Les bornes sont les jours réels de la conférence. */
    DAY,
    /** Seul le mois est connu : les bornes couvrent le mois et ne doivent pas être affichées. */
    MONTH
}
