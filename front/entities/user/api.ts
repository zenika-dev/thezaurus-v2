import {
  apiFetch,
  type BackendProfileView,
  type BackendUserSummary,
} from "@/shared/api";
import type { NotificationPreferences, UserProfile } from "./model";

function mapBackendToFrontend(profile: BackendProfileView): UserProfile {
  return {
    name: profile.name ?? "",
    email: profile.email ?? "",
    notificationPreferences: {
      // Un canal ne doit jamais s'activer tout seul.
      email: profile.notificationPreferences?.email ?? false,
      slack: profile.notificationPreferences?.slack ?? false,
    },
    slackLinked: profile.slackLinked ?? false,
  };
}

export const profileApi = {
  getProfile: async (): Promise<UserProfile> => {
    const res = await apiFetch("/api/me/profile");
    if (!res.ok) throw new Error("Failed to fetch profile");
    return mapBackendToFrontend(await res.json());
  },
  /** Le PUT porte l'objet complet, pas le seul champ modifié. */
  updateNotificationPreferences: async (
    preferences: NotificationPreferences,
  ): Promise<NotificationPreferences> => {
    const res = await apiFetch("/api/me/profile/notification-preferences", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(preferences),
    });
    if (!res.ok) throw new Error("Failed to update notification preferences");
    return preferences;
  },
};

export const userApi = {
  getUsers: async (query?: string): Promise<BackendUserSummary[]> => {
    const url =
      query && query.trim().length > 0
        ? `/api/users?query=${encodeURIComponent(query.trim())}`
        : "/api/users";
    const res = await apiFetch(url);
    if (!res.ok) throw new Error("Failed to fetch users");
    return await res.json();
  },
};
