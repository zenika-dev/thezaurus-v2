import type { BackendNotificationPreferences } from "@/shared/api";

/**
 * Canaux de notification par lesquels l'utilisateur·trice peut être joint·e. Version totale du
 * type généré (`email`/`slack` y sont optionnels, faute d'annotation `@Schema` sur le record Java).
 */
export type NotificationPreferences = Required<BackendNotificationPreferences>;

/**
 * Profil de la personne connectée. `name` et `email` viennent du SSO, en lecture seule.
 * `slackLinked` et non le `slackUserId` : le backend ne publie pas l'identifiant Slack.
 *
 * Pas dérivable du contrat : `ProfileResource.getProfile()` retourne un `Response` brut (branche
 * 404), donc SmallRye ne peut pas en inférer de schéma — même limitation que les autres endpoints
 * à statut conditionnel (`get`/`create`/`update`/`delete` sur les ressources REST).
 */
export interface UserProfile {
  name: string;
  email: string;
  notificationPreferences: NotificationPreferences;
  slackLinked: boolean;
}
