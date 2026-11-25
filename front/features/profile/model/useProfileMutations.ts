import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateNotificationPreferencesAction } from "@/entities/user/actions";
import type { NotificationPreferences, UserProfile } from "@/entities/user";
import { queryKeys } from "@/shared/api";

/**
 * Sauvegarde automatique à chaque bascule. En cas d'échec, rollback + `isError` que l'appelant
 * affiche : sur un interrupteur, un rollback muet ressemble à un bug.
 */
export function useProfileMutations() {
  const queryClient = useQueryClient();
  const queryKey = queryKeys.profile.me();

  const updateNotificationPreferences = useMutation({
    mutationFn: (preferences: NotificationPreferences) =>
      updateNotificationPreferencesAction(preferences),
    onMutate: async (preferences) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<UserProfile>(queryKey)?.notificationPreferences;
      queryClient.setQueryData<UserProfile>(queryKey, (old) =>
        old ? { ...old, notificationPreferences: preferences } : old,
      );
      return { previous };
    },
    onError: (_err, _preferences, ctx) => {
      const previous = ctx?.previous;
      if (previous) {
        queryClient.setQueryData<UserProfile>(queryKey, (old) =>
          old ? { ...old, notificationPreferences: previous } : old,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return { updateNotificationPreferences };
}
