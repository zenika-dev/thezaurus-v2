import { apiFetch } from "@/shared/api";
import { eventsDashboardSchema, type EventsDashboard } from "./schema";

export const eventApi = {
  getEventsDashboard: async (year: number): Promise<EventsDashboard> => {
    const response = await apiFetch(`/events/dashboard?year=${year}`, { cache: "no-store" });
    if (!response.ok) throw new Error("Impossible de charger les événements.");
    return eventsDashboardSchema.parse(await response.json());
  },
};
