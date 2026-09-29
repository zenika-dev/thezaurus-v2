"use client";

import { useId, type ReactNode } from "react";
import { Box, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, FormHelperText, InputLabel, MenuItem, Select, TextField } from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import dayjs from "dayjs";
import "dayjs/locale/fr";
import { Bot } from "lucide-react";
import { DatePickerProvider } from "@/shared/ui";
import { TalkStatus } from "@/shared/api";
import { type TalkData, SpeakerAutocomplete, agencyLabels, formatLabels, languageLabels,
  visibilityLabels, talkStatusConfig } from "@/entities/talk";
import { legacyFormatLabels } from "@/entities/talk/model";
import { ConferenceAutocomplete } from "@/entities/conference/ui/ConferenceAutocomplete";

dayjs.locale("fr");

interface TalkFormDialogProps {
  open: boolean;
  title: string;
  description: string;
  value: TalkData;
  onChange: <K extends keyof TalkData>(field: K, value: TalkData[K]) => void;
  onClose: () => void;
  onSubmit: () => void;
  onReview: () => void;
  submitLabel: string;
  disabled?: boolean;
  pending?: boolean;
  assistantLoading: boolean;
  errors: Record<string, string | undefined>;
  feedback?: ReactNode;
  speakerWarning?: ReactNode;
  secondaryActions?: ReactNode;
  existingTalk?: boolean;
}

/** Même présentation pour la création, la modification et la consultation d’un talk. */
export function TalkFormDialog({ open, title, description, value, onChange, onClose, onSubmit,
  onReview, submitLabel, disabled = false, pending = false, assistantLoading, errors,
  feedback, speakerWarning, secondaryActions, existingTalk = false }: TalkFormDialogProps) {
  const id = useId();
  const formId = `${id}-form`;
  const renderSelectField = (field: "office" | "format" | "language" | "visibility", label: string,
    choices: Record<string, string>, required = false) => (
    <FormControl fullWidth required={required} disabled={disabled} error={!!errors[field]}>
      <InputLabel id={`${id}-${field}-label`}>{label}</InputLabel>
      <Select value={value[field]} labelId={`${id}-${field}-label`} label={label}
        onChange={event => {
          if (field === "visibility") onChange(field, event.target.value === "PUBLIC" ? "PUBLIC" : "PRIVATE");
          else onChange(field, event.target.value);
        }}>
        {value[field] && !choices[value[field]] && <MenuItem value={value[field]}>
          {legacyFormatLabels[value[field]] ?? value[field]} (valeur existante)
        </MenuItem>}
        {Object.entries(choices).map(([key, label]) => <MenuItem key={key} value={key}>{label}</MenuItem>)}
      </Select>
      {errors[field] && <FormHelperText>{errors[field]}</FormHelperText>}
    </FormControl>
  );
  return <DatePickerProvider>
    <Dialog open={open} onClose={pending ? undefined : onClose} maxWidth="md" fullWidth
      aria-labelledby={`${id}-title`} slotProps={{ paper: { className: "dark:bg-slate-900 dark:bg-none" } }}>
      <DialogTitle id={`${id}-title`} className="pb-0! flex justify-between items-center">
        <span>{title}</span>
        <Button variant="outlined" size="small" onClick={onReview} disabled={disabled || assistantLoading}
          startIcon={assistantLoading ? <CircularProgress size={16} className="text-purple-600" />
            : <Bot size={18} className="text-purple-600 dark:text-purple-400" />}
          className="normal-case border-purple-300 dark:border-purple-800 text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-950/50">
          {assistantLoading ? "Traitement IA en cours..." : "Relire avec l’Assistant talk"}
        </Button>
      </DialogTitle>
      <DialogContent>
        {feedback}
        <p className="text-sm text-text-muted mb-6">{description}</p>
        <Box component="form" id={formId} noValidate onSubmit={event => {
          event.preventDefault();
          if (!disabled) onSubmit();
        }} sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
          <TextField label="Titre du talk" value={value.title} onChange={event => onChange("title", event.target.value)}
            required fullWidth disabled={disabled} placeholder="Ex: Building Resilient Microservices"
            error={!!errors.title} helperText={errors.title} />
          <div className="grid grid-cols-2 gap-4">
            <SpeakerAutocomplete label="Speaker" required disabled={disabled}
              value={value.speakers.map(speaker => ({ name: speaker.name, email: speaker.email ?? "" }))}
              onChange={speakers => onChange("speakers", speakers)} error={!!errors.speakers} helperText={errors.speakers} />
            {renderSelectField("office", "Agence", agencyLabels, true)}
          </div>
          {speakerWarning}
          <TextField label="Abstract / Description" value={value.description} onChange={event => onChange("description", event.target.value)}
            multiline rows={4} fullWidth required disabled={disabled} placeholder="Décrivez le contenu de votre talk..."
            error={!!errors.description} helperText={errors.description} />
          <Box component="fieldset" sx={{ border: "none", p: 0, m: 0 }}>
            <Box component="legend" sx={{ display: "none" }}>Paramètres du talk</Box>
            <div className="grid grid-cols-3 gap-4">
              {renderSelectField("format", "Format", formatLabels, true)}
              {renderSelectField("visibility", "Visibilité", visibilityLabels, true)}
              {renderSelectField("language", "Langue", languageLabels)}
            </div>
          </Box>
          <ConferenceAutocomplete value={value.conference} onChange={conference => onChange("conference", conference)} disabled={disabled} />
          <DatePicker label="Date" value={value.date ? dayjs(value.date) : null} disabled={disabled}
            onChange={date => onChange("date", date ? date.format("YYYY-MM-DD") : "")}
            views={["year", "month", "day"]} format="DD/MM/YYYY"
            slotProps={{ textField: { fullWidth: true, error: !!errors.date || Boolean(value.date && !dayjs(value.date).isValid()), helperText: errors.date } }} />
          <TextField label="Notes / Commentaires" value={value.notes} onChange={event => onChange("notes", event.target.value)}
            multiline rows={4} fullWidth disabled={disabled} placeholder="Informations complémentaires..." />
          {existingTalk && <>
            <TextField select label="Statut" value={value.status} disabled={disabled} onChange={event => {
              const status = TalkStatus.find(status => status === event.target.value);
              if (status) onChange("status", status);
            }}>{TalkStatus.map(status => <MenuItem key={status} value={status}>{talkStatusConfig[status].label}</MenuItem>)}</TextField>
            <div className="grid grid-cols-2 gap-4">
              <TextField label="Slides" value={value.slides ?? ""} disabled={disabled} fullWidth
                onChange={event => onChange("slides", event.target.value)} error={!!errors.slides} helperText={errors.slides} />
              <TextField label="Replay" value={value.replay ?? ""} disabled={disabled} fullWidth
                onChange={event => onChange("replay", event.target.value)} error={!!errors.replay} helperText={errors.replay} />
            </div>
            <TextField label="Audience" type="number" value={value.audience ?? ""} disabled={disabled}
              onChange={event => onChange("audience", event.target.value === "" ? null : Number(event.target.value))}
              error={!!errors.audience} helperText={errors.audience} slotProps={{ htmlInput: { min: 0, step: 1 } }} />
          </>}
        </Box>
      </DialogContent>
      <DialogActions className="px-6! pb-6! pt-5!">
        <Button variant="outlined" disabled={pending} onClick={onClose}>Annuler</Button>
        <div className="flex-1" />
        {secondaryActions}
        <Button variant="contained" disabled={disabled} type="submit" form={formId}>{submitLabel}</Button>
      </DialogActions>
    </Dialog>
  </DatePickerProvider>;
}
