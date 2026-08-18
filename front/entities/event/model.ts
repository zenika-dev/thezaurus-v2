import type { EventVisibility } from "./schema";

/** Libellés des mois pour l'axe du graphique d'activité mensuelle. */
export const MONTH_LABELS = [
  "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
  "Juil", "Août", "Sep", "Oct", "Nov", "Déc",
] as const;

export const visibilityLabels: Record<EventVisibility, string> = {
  internal: "Interne",
  external: "Externe",
};


export const visibilityColors: Record<
  EventVisibility,
  { color: string; bg: string; darkColor: string; darkBg: string; chart: string }
> = {
  external: { color: "var(--color-primary)", bg: "#ed213c1a", darkColor: "var(--color-primary)", darkBg: "#ed213c1a" , chart: "var(--color-primary)"}, 
  internal: { color: "var(--color-primary)", bg: "#FBACB366", darkColor: "#D22B42", darkBg: "#FBACB322" , chart: "#FBACB3"}, 
};
