"use client";

import FormControlLabel from "@mui/material/FormControlLabel";
import Switch from "@mui/material/Switch";
import type { NotificationPreferences, UserProfile } from "@/entities/user";
import { useProfileMutations } from "../model/useProfileMutations";

interface NotificationPreferencesSectionProps {
  profile: UserProfile;
}

const SLACK_DISABLED = "L'intégration Slack est désactivée sur cet environnement.";

/** Sauvegarde automatique des préférences, appliquées aux rappels de talks par email et Slack. */
export function NotificationPreferencesSection({ profile }: NotificationPreferencesSectionProps) {
  const { updateNotificationPreferences } = useProfileMutations();
  const preferences = profile.notificationPreferences;

  const toggle = (channel: keyof NotificationPreferences, enabled: boolean) => {
    updateNotificationPreferences.mutate({ ...preferences, [channel]: enabled });
  };

  return (
    <section
      aria-labelledby="profile-notifications-title"
      className="rounded border border-gray-200 dark:border-[#2d2d2d] p-6"
    >
      <h2
        id="profile-notifications-title"
        className="text-[20px] leading-[1.6] font-bold text-text m-0 mb-1"
      >
        Notifications
      </h2>
      <p className="text-[14px] leading-[1.43] text-text-muted m-0 mb-4">
        Active les notifications par mail ou sur Slack pour recevoir les rappels lorsque le replay ou
        l&apos;audience de tes talks manque.
      </p>

      <div className="flex flex-col gap-2">
        <FormControlLabel
          control={
            <Switch
              checked={preferences.email}
              onChange={(e) => toggle("email", e.target.checked)}
              slotProps={{ input: { "aria-describedby": "profile-notifications-error" } }}
            />
          }
          label="Me notifier par mail"
        />

        <div>
          <FormControlLabel
            control={
              <Switch
                checked={preferences.slack && profile.slackAvailable}
                disabled={!profile.slackAvailable}
                onChange={(e) => toggle("slack", e.target.checked)}
                slotProps={{ input: { "aria-describedby": "profile-notifications-error" } }}
              />
            }
            label="Me notifier sur Slack"
          />
          {!profile.slackAvailable && (
            <p className="text-[13px] leading-[1.4] text-text-muted m-0 ml-[46px]">
              {SLACK_DISABLED}
            </p>
          )}
        </div>
      </div>

      <p
        id="profile-notifications-error"
        role="status"
        className="text-[13px] leading-[1.4] text-[var(--mui-palette-error-main,#d32f2f)] m-0 mt-3 min-h-[18px]"
      >
        {updateNotificationPreferences.isError
          ? "La préférence n'a pas pu être enregistrée. Réessaie dans un instant."
          : ""}
      </p>
    </section>
  );
}
