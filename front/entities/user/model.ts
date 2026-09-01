/** Canaux de notification par lesquels l'utilisateur·trice peut être joint·e. */
export interface NotificationPreferences {
  email: boolean;
  slack: boolean;
}

/**
 * Profil de la personne connectée. `name` et `email` viennent du SSO, en lecture seule.
 * `slackLinked` et non le `slackUserId` : le backend ne publie pas l'identifiant Slack.
 */
export interface UserProfile {
  name: string;
  email: string;
  notificationPreferences: NotificationPreferences;
  slackLinked: boolean;
}
