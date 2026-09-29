"use client";

import Autocomplete from "@mui/material/Autocomplete";
import TextField from "@mui/material/TextField";
import { useQuery } from "@tanstack/react-query";
import { conferenceApi } from "../api";
import { queryKeys, type BackendConference } from "@/shared/api";

export function ConferenceAutocomplete({ value, onChange, disabled = false }: {
  value: BackendConference | null;
  onChange: (value: BackendConference | null) => void;
  disabled?: boolean;
}) {
  const { data = [], isFetching, isError } = useQuery({
    queryKey: queryKeys.conferences.lists(),
    queryFn: conferenceApi.getConferences,
    enabled: !disabled,
  });
  return (
    <Autocomplete<BackendConference, false, false, true>
      freeSolo autoSelect disabled={disabled} fullWidth loading={isFetching}
      options={data} value={value}
      getOptionKey={(option) => typeof option === "string" ? option : option.id ?? option.name ?? ""}
      getOptionLabel={(option) => typeof option === "string" ? option : option.name ?? ""}
      isOptionEqualToValue={(option, selected) => typeof selected !== "string" && Boolean(option.id && option.id === selected.id)}
      onChange={(_, selected) => onChange(typeof selected === "string"
        ? (selected.trim() ? { name: selected.trim() } : null) : selected)}
      renderOption={(props, option) => {
        const { key, ...rest } = props;
        return <li key={key} {...rest}>{option.name}{option.date?.start ? ` — ${option.date.start}` : ""}</li>;
      }}
      renderInput={(params) => <TextField {...params} label="Conférence ciblée" size="small"
        helperText={isError ? "Recherche indisponible. La saisie libre reste possible."
          : "Sélectionnez une conférence ou saisissez un nom libre (Entrée pour valider)."} />}
    />
  );
}
