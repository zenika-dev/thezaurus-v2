import { apiFetch, type BackendNotificationPreferences } from "@/shared/api";
import type { NotificationPreferences, UserProfile } from "./model";

/**
 * `ProfileResource.getProfile()` retourne un `Response` brut (branche 404), donc SmallRye ne
 * génère aucun schéma pour cette réponse : ce type reste à écrire à la main. Seul le nid
 * `notificationPreferences` est dérivé du contrat, via le type généré pour le PUT correspondant.
 */
interface BackendProfile {
  name?: string | null;
  email?: string | null;
  notificationPreferences?: BackendNotificationPreferences | null;
  slackLinked?: boolean;
}

function mapBackendToFrontend(profile: BackendProfile): UserProfile {
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
