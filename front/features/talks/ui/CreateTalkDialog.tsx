"use client";

import { useState, useEffect } from "react";
import { useSession } from "next-auth/react";
import { DatePickerProvider } from "@/shared/ui";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import FormHelperText from "@mui/material/FormHelperText";
import Select from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import dayjs, { type Dayjs } from "dayjs";
import "dayjs/locale/fr";
import { Bot } from "lucide-react";
import type { BackendTalkReviewResponse, TalkStatus } from "@/shared/api";
import type { TalkData } from "@/entities/talk";
import {
  agencyLabels,
  visibilityLabels,
  formatLabels,
  languageLabels,
  SpeakerAutocomplete,
  talkFormSchema,
  type TalkFormData,
  type SpeakerFormData,
  reviewTalkAction,
} from "@/entities/talk";
import { TalkAssistantDialog } from "@/features/talks/ui/TalkAssistantDialog";

dayjs.locale("fr");

const DIALOG_TITLE_ID = "create-talk-dialog-title";

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => void;
}

export function CreateTalkDialog({ open, onClose, onSubmit }: CreateTalkDialogProps) {
  const { data: session } = useSession();
  const [date, setDate] = useState<Dayjs | null>(null);

  const [assistantDialogOpen, setAssistantDialogOpen] = useState(false);
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [assistantResult, setAssistantResult] = useState<BackendTalkReviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const defaultSpeakers: SpeakerFormData[] = session?.user?.name
    ? [{ name: session.user.name, email: session.user.email ?? "" }]
    : [];

  const {
    register,
    handleSubmit,
    control,
    trigger,
    getValues,
    setValue,
    reset,
    formState: { errors },
  } = useForm<TalkFormData>({
    resolver: zodResolver(talkFormSchema),
    defaultValues: {
      title: "",
      speakers: defaultSpeakers,
      office: "",
      description: "",
      format: "",
      visibility: "PRIVATE",
      language: "francais",
      conference: "",
      notes: "",
    },
  });

  useEffect(() => {
    if (open && session?.user?.name) {
      const current = getValues("speakers");
      if (!current || current.length === 0) {
        setValue("speakers", [{ name: session.user.name, email: session.user.email ?? "" }]);
      }
    }
  }, [open, session?.user?.name, session?.user?.email, setValue, getValues]);

  const buildTalkData = (
    { speakers, conference, ...data }: TalkFormData,
    status: TalkStatus,
  ): TalkData => ({
    id: crypto.randomUUID(),
    ...data,
    speakers: speakers.map((speaker) => ({
      name: speaker.name,
      email: speaker.email?.trim() || undefined,
    })),
    conference: conference.trim() ? { name: conference.trim() } : null,
    date: date ? date.format("YYYY-MM-DD") : "",
    status,
    slides: "",
    replay: "",
  });

  const resetFormState = () => {
    reset({
      title: "",
      speakers: session?.user?.name
        ? [{ name: session.user.name, email: session.user.email ?? "" }]
        : [],
      office: "",
      description: "",
      format: "",
      visibility: "PRIVATE",
      language: "francais",
      conference: "",
      notes: "",
    });
    setDate(null);
    setAssistantResult(null);
    setError(null);
  };

  const handleCancel = () => {
    resetFormState();
    onClose();
  };

  const handleSaveDraft = async () => {
    const titleValid = await trigger("title");
    if (!titleValid) return;
    onSubmit(buildTalkData(getValues(), "DRAFT"));
    resetFormState();
    onClose();
  };

  const onCreateTalk = (data: TalkFormData) => {
    onSubmit(buildTalkData(data, "PLANNED"));
    resetFormState();
    onClose();
  };

  const handleTriggerAssistantReview = async () => {
    const values = getValues();
    setAssistantDialogOpen(true);
    if (!values.title.trim()) {
      setError("Veuillez saisir un titre avant de demander une relecture.");
      return;
    }
    setAssistantLoading(true);
    try {
      const res = await reviewTalkAction({
        title: values.title,
        abstract: values.description,
      });
      setAssistantResult(res);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Erreur de communication avec l'assistant IA.";
      setError(message);
    } finally {
      setAssistantLoading(false);
    }
  };

  const handleApplyAssistantSuggestions = (
    suggestedTitle: string,
    suggestedDescription: string,
  ) => {
    setValue("title", suggestedTitle, { shouldValidate: true, shouldDirty: true });
    setValue("description", suggestedDescription, { shouldValidate: true, shouldDirty: true });
  };

  return (
    <DatePickerProvider>
    <Dialog
      onClose={handleCancel}
      open={open}
      maxWidth="md"
      fullWidth
      aria-labelledby={DIALOG_TITLE_ID}
      slotProps={{ paper: { className: "dark:bg-slate-900 dark:bg-none" } }}
    >

      <DialogTitle id={DIALOG_TITLE_ID} className="pb-0! flex justify-between items-center">
        <span>Nouveau Talk</span>
        <Button
          variant="outlined"
          size="small"
          onClick={handleTriggerAssistantReview}
          disabled={assistantLoading}
          startIcon={
            assistantLoading ? (
              <CircularProgress size={16} className="text-purple-600" />
            ) : (
              <Bot size={18} className="text-purple-600 dark:text-purple-400" />
            )
          }
          className="normal-case border-purple-300 dark:border-purple-800 text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-950/50"
        >
          {assistantLoading ? "Traitement IA en cours..." : "Relire avec l'Assistant talk"}
        </Button>
      </DialogTitle>


      <DialogContent>
        <p className="text-sm text-text-muted mb-6">
          Déclarez une nouvelle idée de talk ou soumission à une conférence.
        </p>

        <Box
          component="form"
          id="create-talk-form"
          onSubmit={handleSubmit(onCreateTalk)}
          noValidate
          sx={{ display: "flex", flexDirection: "column", gap: 2 }}
        >
          <TextField
            {...register("title")}
            id="talk-title"
            label="Titre du talk"
            required
            fullWidth
            placeholder="Ex: Building Resilient Microservices"
            error={!!errors.title}
            helperText={errors.title?.message}
          />

            <div className="grid grid-cols-2 gap-4">
              <Controller
                name="speakers"
                control={control}
                render={({ field }) => (
                  <SpeakerAutocomplete
                    id="talk-speakers"
                    label="Speaker"
                    required
                    value={field.value}
                    onChange={field.onChange}
                    error={!!errors.speakers}
                    helperText={
                      errors.speakers?.message ||
                      (errors.speakers as { root?: { message?: string } })?.root?.message
                    }
                  />
                )}
              />

              <Controller
                name="office"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth required error={!!errors.office}>
                    <InputLabel id="create-agency-label">Agence</InputLabel>
                    <Select
                      {...field}
                      labelId="create-agency-label"
                      id="talk-agency"
                      label="Agence"
                    >
                      {Object.entries(agencyLabels).map(([agencyKey, agencyLabel]) => (
                        <MenuItem key={agencyKey} value={agencyKey}>
                          {agencyLabel}
                        </MenuItem>
                      ))}
                    </Select>
                    {errors.office && (
                      <FormHelperText>{errors.office.message}</FormHelperText>
                    )}
                  </FormControl>
                )}
              />
            </div>

          <TextField
            {...register("description")}
            id="talk-abstract"
            label="Abstract / Description"
            multiline
            rows={4}
            fullWidth
            required
            placeholder="Décrivez le contenu de votre talk..."
            error={!!errors.description}
            helperText={errors.description?.message}
          />

          <Box component="fieldset" sx={{ border: "none", p: 0, m: 0 }}>
            <Box component="legend" sx={{ display: "none" }}>Paramètres du talk</Box>
            <div className="grid grid-cols-3 gap-4">
              <Controller
                name="format"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth required error={!!errors.format}>
                    <InputLabel id="create-format-label">Format</InputLabel>
                    <Select {...field} labelId="create-format-label" id="talk-format" label="Format">
                      {Object.entries(formatLabels).map(([formatKey, formatLabel]) => (
                        <MenuItem key={formatKey} value={formatKey}>{formatLabel}</MenuItem>
                      ))}
                    </Select>
                    {errors.format && (
                      <FormHelperText>{errors.format.message}</FormHelperText>
                    )}
                  </FormControl>
                )}
              />
              <Controller
                name="visibility"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth required error={!!errors.visibility}>
                    <InputLabel id="create-visibility-label">Visibilité</InputLabel>
                    <Select {...field} labelId="create-visibility-label" id="talk-visibility" label="Visibilité">
                      {Object.entries(visibilityLabels).map(([visibilityKey, visibilityLabel]) => (
                        <MenuItem key={visibilityKey} value={visibilityKey}>{visibilityLabel}</MenuItem>
                      ))}
                    </Select>
                    {errors.visibility && (
                      <FormHelperText>{errors.visibility.message}</FormHelperText>
                    )}
                  </FormControl>
                )}
              />
              <Controller
                name="language"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel id="create-lang-label">Langue</InputLabel>
                    <Select {...field} labelId="create-lang-label" id="talk-language" label="Langue">
                      {Object.entries(languageLabels).map(([languageKey, languageLabel]) => (
                        <MenuItem key={languageKey} value={languageKey}>{languageLabel}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              />
            </div>
          </Box>

          <TextField
            {...register("conference")}
            id="talk-conference"
            label="Conférence cible"
            fullWidth
            placeholder="Ex: Devoxx France, Sunny Tech..."
          />

          <DatePicker
            label="Date"
            value={date}
            onChange={setDate}
            views={["year", "month", "day"]}
            format="DD/MM/YYYY"
            slotProps={{ textField: { fullWidth: true, id: "talk-date" } }}
          />

          <TextField
            {...register("notes")}
            id="talk-notes"
            label="Notes / Commentaires"
            multiline
            rows={4}
            fullWidth
            placeholder="Informations complémentaires..."
          />
        </Box>
      </DialogContent>

      <DialogActions className="px-6! pb-6! pt-5!">
        <Button variant="outlined" onClick={handleCancel}>Annuler</Button>
        <div className="flex-1" />
        <Button
          variant="outlined"
          onClick={handleSaveDraft}
          className="text-[#bbb]! border-[#ddd]!"
        >
          Sauvegarder en brouillon
        </Button>
        <Button variant="contained" type="submit" form="create-talk-form">
          Créer le talk
        </Button>
      </DialogActions>

      <TalkAssistantDialog
          open={assistantDialogOpen}
          loading={assistantLoading}
          error={error}
          title={getValues().title}
          abstract={getValues().description}
          assistantReviewResult={assistantResult}
          onClose={() => setAssistantDialogOpen(false)}
          onApply={handleApplyAssistantSuggestions}
      />
    </Dialog>
    </DatePickerProvider>
  );
}
