import type { EventVisibility } from "./schema";

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
