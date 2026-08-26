"use server";

import { revalidatePath } from "next/cache";
import { profileApi } from "./api";
import type { NotificationPreferences } from "./model";

export async function updateNotificationPreferencesAction(
  preferences: NotificationPreferences,
): Promise<void> {
  await profileApi.updateNotificationPreferences(preferences);
  revalidatePath("/profile");
}
