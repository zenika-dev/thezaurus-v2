import { z } from "zod";

export const eventVisibilitySchema = z.enum(["internal", "external"]);

export const monthlyActivitySchema = z.object({
  month: z.string(),
  internal: z.number().int().nonnegative(),
  external: z.number().int().nonnegative(),
});

export const cityCountSchema = z.object({
  city: z.string(),
  count: z.number().int().nonnegative(),
});

export const eventTypeSummarySchema = z.object({
  name: z.string(),
  visibility: eventVisibilitySchema,
  total: z.number().int().nonnegative(),
  cities: z.array(cityCountSchema),
});

export const eventsDashboardSchema = z.object({
  year: z.number().int(),
  totals: z.object({
    internal: z.number().int().nonnegative(),
    external: z.number().int().nonnegative(),
  }),
  monthly: z.array(monthlyActivitySchema),
  eventTypes: z.array(eventTypeSummarySchema),
});

export type EventVisibility = z.infer<typeof eventVisibilitySchema>;
export type MonthlyActivity = z.infer<typeof monthlyActivitySchema>;
export type CityCount = z.infer<typeof cityCountSchema>;
export type EventTypeSummary = z.infer<typeof eventTypeSummarySchema>;
export type EventsDashboard = z.infer<typeof eventsDashboardSchema>;
