"use server";

import { revalidatePath } from "next/cache";
import { profileApi } from "./api";
import type { Office } from "@/shared/api";
import type { CalendarLocale, NotificationPreferences } from "./model";

export async function updateOfficeAction(office: Office | ""): Promise<void> {
  await profileApi.updateOffice(office);
  revalidatePath("/profile");
}

export async function updateCalendarLocaleAction(calendarLocale: CalendarLocale): Promise<void> {
  await profileApi.updateCalendarLocale(calendarLocale);
  revalidatePath("/profile");
}

export async function updateNotificationPreferencesAction(
  preferences: NotificationPreferences,
): Promise<void> {
  await profileApi.updateNotificationPreferences(preferences);
  revalidatePath("/profile");
}
