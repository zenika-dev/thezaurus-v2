import type { BackendNotificationPreferences, BackendProfileView } from "@/shared/api";

/**
 * Canaux de notification par lesquels l'utilisateur·trice peut être joint·e. Version totale du
 * type généré (`email`/`slack` y sont optionnels, faute d'annotation `@Schema` sur le record Java).
 */
export type NotificationPreferences = Required<BackendNotificationPreferences>;

/**
 * Profil de la personne connectée. `name` et `email` viennent du SSO, en lecture seule.
 * `slackLinked` et non le `slackUserId` : le backend ne publie pas l'identifiant Slack.
 */
export type UserProfile = Omit<Required<BackendProfileView>, "notificationPreferences"> & {
  notificationPreferences: NotificationPreferences;
};
