import { z } from "zod";
import { MonthLabel } from "@/shared/api";

export const eventVisibilitySchema = z.enum(["internal", "external"]);

export const monthlyActivitySchema = z.object({
  month: z.enum(MonthLabel),
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

export const eventsTotalsSchema = z.object({
  internal: z.number().int().nonnegative(),
  external: z.number().int().nonnegative(),
});

export const eventsDashboardSchema = z.object({
  year: z.number().int(),
  totals: eventsTotalsSchema,
  monthly: z.array(monthlyActivitySchema),
  eventTypes: z.array(eventTypeSummarySchema),
});

export type EventVisibility = z.infer<typeof eventVisibilitySchema>;
export type MonthlyActivity = z.infer<typeof monthlyActivitySchema>;
export type CityCount = z.infer<typeof cityCountSchema>;
export type EventTypeSummary = z.infer<typeof eventTypeSummarySchema>;
export type EventsTotals = z.infer<typeof eventsTotalsSchema>;
export type EventsDashboard = z.infer<typeof eventsDashboardSchema>;
