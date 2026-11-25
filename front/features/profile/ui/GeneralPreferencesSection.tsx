"use client";

import { Alert, FormControl, FormControlLabel, FormLabel, MenuItem, Radio, RadioGroup, TextField } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { agencyLabels } from "@/shared/lib/offices";
import { updateCalendarLocaleAction, updateOfficeAction } from "@/entities/user/actions";
import type { CalendarLocale, UserProfile } from "@/entities/user";
import { Office, queryKeys } from "@/shared/api";
import { SectionCard } from "@/shared/ui";

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
  const calendarLocaleMutation = useMutation({
    mutationFn: (locale: CalendarLocale) => updateCalendarLocaleAction(locale),
    onMutate: () => queryClient.cancelQueries({ queryKey: queryKeys.profile.me() }),
    onSuccess: (_, calendarLocale) => {
      queryClient.setQueryData<UserProfile>(queryKeys.profile.me(), (current) =>
        current ? { ...current, calendarLocale } : current,
      );
    },
  });
  return (
    <SectionCard
      titleId="profile-preferences-title"
      title="Préférences générales"
    >
      <TextField
        select
        fullWidth
        label="Agence"
        value={mutation.isPending ? mutation.variables : profile.office}
        disabled={mutation.isPending}
        onChange={(event) => {
          const office = Office.find((office) => office === event.target.value) ?? "";
          if (office !== profile.office) mutation.mutate(office);
        }}
        helperText="Cette agence sera proposée à la création de vos prochains talks et articles."
      >
        <MenuItem value="">Non renseignée</MenuItem>
        {Object.entries(agencyLabels).map(([value, label]) => (
          <MenuItem key={value} value={value}>{label}</MenuItem>
        ))}
      </TextField>
      {mutation.isError && (
        <Alert severity="error">La modification a échoué. Vous pouvez réessayer.</Alert>
      )}
      <div className="mt-4 flex flex-col items-start gap-2">
        <FormControl component="fieldset" disabled={calendarLocaleMutation.isPending}>
          <FormLabel component="legend" id="calendar-locale-label">Langue des calendriers</FormLabel>
          <RadioGroup
            row
            aria-labelledby="calendar-locale-label"
            name="calendarLocale"
            value={calendarLocaleMutation.isPending ? calendarLocaleMutation.variables : profile.calendarLocale}
            onChange={(_, locale) => {
              if ((locale === "fr" || locale === "en") && locale !== profile.calendarLocale) {
                calendarLocaleMutation.mutate(locale);
              }
            }}
          >
            <FormControlLabel value="fr" control={<Radio />} label="Français" />
            <FormControlLabel value="en" control={<Radio />} label="English" />
          </RadioGroup>
        </FormControl>
        {calendarLocaleMutation.isError && (
          <Alert severity="error">La langue des calendriers n’a pas été enregistrée. Vous pouvez réessayer.</Alert>
        )}
      </div>
    </SectionCard>
  );
}
