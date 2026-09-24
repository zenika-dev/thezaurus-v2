"use client";

import { Alert, MenuItem, TextField } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { agencyLabels } from "@/entities/talk";
import { updateOfficeAction } from "@/entities/user/actions";
import type { UserProfile } from "@/entities/user";
import { Office, queryKeys } from "@/shared/api";

export function GeneralPreferencesSection({ profile }: { profile: UserProfile }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: updateOfficeAction,
    onMutate: () => queryClient.cancelQueries({ queryKey: queryKeys.profile.me() }),
    onSuccess: (_, savedOffice) => {
      queryClient.setQueryData<UserProfile>(queryKeys.profile.me(), (current) =>
        current ? { ...current, office: savedOffice } : current,
      );
    },
  });
  return (
    <section
      aria-labelledby="profile-preferences-title"
      className="rounded border border-gray-200 dark:border-[#2d2d2d] p-6 flex flex-col gap-4"
    >
      <h2 id="profile-preferences-title" className="text-xl font-bold">
        Préférences générales
      </h2>
      <TextField
        select
        label="Agence"
        value={mutation.isPending ? mutation.variables : profile.office}
        disabled={mutation.isPending}
        onChange={(event) => {
          const office = Office.find((office) => office === event.target.value) ?? "";
          if (office !== profile.office) mutation.mutate(office);
        }}
        helperText="Cette agence sera proposée à la création de vos prochains talks."
      >
        <MenuItem value="">Non renseignée</MenuItem>
        {Object.entries(agencyLabels).map(([value, label]) => (
          <MenuItem key={value} value={value}>{label}</MenuItem>
        ))}
      </TextField>
      {mutation.isError && (
        <Alert severity="error">La modification a échoué. Vous pouvez réessayer.</Alert>
      )}
    </section>
  );
}
