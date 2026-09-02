import dayjs from "dayjs";
import "dayjs/locale/fr";
import type { ConferencePeriod } from "./model";

dayjs.locale("fr");

/**
 * Les trois formes d'affichage se déduisent de la période, sans champ discriminant : `MONTH` masque
 * les bornes (elles couvrent le mois entier faute de dates arrêtées), sinon `start === end` désigne
 * une journée et le reste un intervalle.
 */
export function formatConferenceDate(d: ConferencePeriod): string {
    if (!d.start) return "";

    if (d.precision === "MONTH") {
        return dayjs(d.start).locale("fr").format("MMMM YYYY");
    }

    const start = dayjs(d.start).locale("fr");
    if (!d.end || d.start === d.end) {
        return start.format("D MMMM YYYY");
    }

    const end = dayjs(d.end).locale("fr");
    if (start.year() !== end.year()) {
        return `${start.format("D MMMM YYYY")} - ${end.format("D MMMM YYYY")}`;
    }
    if (start.month() === end.month()) {
        return `${start.format("D")}-${end.format("D MMMM YYYY")}`;
    }
    return `${start.format("D MMMM")} - ${end.format("D MMMM YYYY")}`;
}

export function getConferenceYear(d: ConferencePeriod): number {
    return dayjs(d.start).year();
}

/**
 * L'ISO se trie lexicographiquement dans l'ordre chronologique : la borne de début suffit. Ce tri
 * pourrait désormais être délégué au back (`orderBy("date.start")`), ce que l'ancien format
 * « chaîne surchargée » interdisait.
 */
export function getConferenceSortKey(d: ConferencePeriod): string {
    return d.start;
}
