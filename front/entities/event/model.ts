import { visibilityTones } from "@/shared/lib/status-tones";
import type { EventVisibility } from "./schema";

export const visibilityLabels: Record<EventVisibility, string> = {
  internal: "Interne",
  external: "Externe",
};


export const visibilityColors: Record<
  EventVisibility,
  { color: string; bg: string; darkColor: string; darkBg: string; chart: string }
> = {
  external: { color: visibilityTones.external.text, bg: visibilityTones.external.bg, darkColor: visibilityTones.external.darkText, darkBg: visibilityTones.external.darkBg, chart: "#278443" },
  internal: { color: visibilityTones.internal.text, bg: visibilityTones.internal.bg, darkColor: visibilityTones.internal.darkText, darkBg: visibilityTones.internal.darkBg, chart: "#ed213c" },
};
