import { eventsDashboardSchema, type EventsDashboard } from "./schema";


// const API_URL = `${API_BASE}/events/dashboard`;
const MOCK_DASHBOARD_2026: EventsDashboard = {
  year: 2026,
  totals: { internal: 30, external: 19 },
  monthly: [
    { month: "Jan",  external: 0, internal: 12 },
    { month: "Fév",  external: 4, internal: 4 },
    { month: "Mar",  external: 2, internal: 7 },
    { month: "Avr",  external: 5, internal: 2 },
    { month: "Mai",  external: 4, internal: 5 },
    { month: "Juin", external: 4, internal: 2 },
    { month: "Juil", external: 0, internal: 0 },
    { month: "Août", external: 0, internal: 0 },
    { month: "Sep",  external: 0, internal: 0 },
    { month: "Oct",  external: 0, internal: 0 },
    { month: "Nov",  external: 0, internal: 0 },
    { month: "Déc",  external: 0, internal: 0 },
  ],
  eventTypes: [
    {
      name: "Évènements Hébergés",
      visibility: "internal",
      total: 26,
      cities: [
        { city: "Nantes", count: 8 },
        { city: "Paris", count: 6 },
        { city: "Lille", count: 6 },
        { city: "Brest", count: 3 },
        { city: "Lyon", count: 2 },
        { city: "Montréal", count: 1 },
      ],
    },
    {
      name: "NightClazz",
      visibility: "internal",
      total: 2,
      cities: [
        { city: "Nantes", count: 1 },
        { city: "Lille", count: 1 },
      ],
    },
    {
      name: "Matinale",
      visibility: "internal",
      total: 1,
      cities: [{ city: "Bordeaux", count: 1 }],
    },
  ],
};

export const eventApi = {
  getEventsDashboard: async (year = 2026): Promise<EventsDashboard> => {

    // Mock response api.
    return eventsDashboardSchema.parse({ ...MOCK_DASHBOARD_2026, year });
  },
};
