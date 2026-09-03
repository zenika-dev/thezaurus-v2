package com.zenika.thezaurus.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Période d'une conférence : deux bornes ISO {@code YYYY-MM-DD} inclusives, plus la précision
 * connue. Sans champ discriminant : date unique si {@code start == end}, intervalle sinon, mois
 * entier si précision {@link DatePrecision#MONTH}.
 *
 * <p>Les bornes sont des {@code String} et non des {@link LocalDate} : le mapper POJO Firestore ne
 * connaît pas {@code java.time}, et l'ordre lexicographique ISO coïncide avec l'ordre
 * chronologique, ce qui permet un {@code orderBy} Firestore direct sur {@code start} — impossible
 * avec l'ancien format « chaîne surchargée ».
 */
public class ConferencePeriod {

    private String start;
    private String end;
    private DatePrecision precision;

    public ConferencePeriod() {}

    public ConferencePeriod(String start, String end, DatePrecision precision) {
        this.start = start;
        this.end = end;
        this.precision = precision;
    }

    /**
     * Période d'une seule journée (formulaire Slack, qui n'expose qu'un sélecteur de date).
     *
     * @return la période correspondante, ou {@code null} si la date est absente ou invalide
     */
    public static ConferencePeriod singleDay(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            LocalDate day = LocalDate.parse(isoDate.trim());
            return new ConferencePeriod(day.toString(), day.toString(), DatePrecision.DAY);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Construit une période depuis l'ancien format « chaîne surchargée » utilisé avant ce type :
     * {@code "YYYY-MM-DD"} pour une date unique, {@code "YYYY-MM-DD/YYYY-MM-DD"} pour un
     * intervalle, {@code "YYYY-MM"} pour un mois.
     *
     * @return la période équivalente, ou {@code null} si la chaîne est vide ou non reconnue — la
     *     donnée est alors laissée en l'état plutôt que réécrite en une valeur inventée
     */
    public static ConferencePeriod fromLegacyString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            if (value.contains("/")) {
                String[] bounds = value.split("/", 2);
                LocalDate start = LocalDate.parse(bounds[0].trim());
                LocalDate end = LocalDate.parse(bounds[1].trim());
                return new ConferencePeriod(start.toString(), end.toString(), DatePrecision.DAY);
            }
            if (value.matches("\\d{4}-\\d{2}")) {
                LocalDate first = LocalDate.parse(value + "-01");
                return new ConferencePeriod(
                        first.toString(),
                        first.withDayOfMonth(first.lengthOfMonth()).toString(),
                        DatePrecision.MONTH);
            }
            return singleDay(value);
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public DatePrecision getPrecision() {
        return precision;
    }

    public void setPrecision(DatePrecision precision) {
        this.precision = precision;
    }
}
