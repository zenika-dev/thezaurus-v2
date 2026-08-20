"use client";

import { useState } from "react";
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
import type { TalkData, TalkStatus, TalkReviewResponse } from "@/entities/talk";
import { agencyLabels, visibilityLabels, formatLabels, languageLabels } from "@/entities/talk";
import { talkFormSchema, type TalkFormData } from "@/entities/talk";
import { reviewTalkAction } from "@/entities/talk";
import { AiReviewDialog } from "./AiReviewDialog";

dayjs.locale("fr");

const DIALOG_TITLE_ID = "create-talk-dialog-title";

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => void;
}

export function CreateTalkDialog({ open, onClose, onSubmit }: CreateTalkDialogProps) {
  const [date, setDate] = useState<Dayjs | null>(null);
  const [aiDialogOpen, setAiDialogOpen] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<TalkReviewResponse | null>(null);

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
      title: "", speaker: "", cospeaker: "", email: "",
      agency: "", abstract: "", format: "", visibility: "",
      language: "", conference: "", notes: "",
    },
  });

  const buildTalkData = (data: TalkFormData, status: TalkStatus): TalkData => ({
    id: crypto.randomUUID(),
    ...data,
    date: date ? date.format("DD-MM-YYYY") : "",
    status,
    slides: "",
    replay: "",
  });

  const handleCancel = () => {
    reset();
    setDate(null);
    setAiResult(null);
    setAiDialogOpen(false);
    onClose();
  };

  const handleSaveDraft = async () => {
    const titleValid = await trigger("title");
    if (!titleValid) return;
    onSubmit(buildTalkData(getValues(), "Draft"));
    reset();
    setDate(null);
    setAiResult(null);
    onClose();
  };

  const onCreateTalk = (data: TalkFormData) => {
    onSubmit(buildTalkData(data, "Idea"));
    reset();
    setDate(null);
    setAiResult(null);
    onClose();
  };

  const handleTriggerAiReview = async () => {
    const values = getValues();
    setAiDialogOpen(true);
    setAiLoading(true);
    try {
      const res = await reviewTalkAction({
        title: values.title,
        abstract: values.abstract,
        format: values.format,
        language: values.language,
      });
      setAiResult(res);
    } catch {
      // Error handling
    } finally {
      setAiLoading(false);
    }
  };

  const handleApplyAiSuggestions = (suggestedTitle: string, suggestedAbstract: string) => {
    setValue("title", suggestedTitle, { shouldValidate: true, shouldDirty: true });
    setValue("abstract", suggestedAbstract, { shouldValidate: true, shouldDirty: true });
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
          onClick={handleTriggerAiReview}
          disabled={aiLoading}
          startIcon={
            aiLoading ? (
              <CircularProgress size={16} className="text-purple-600" />
            ) : (
              <Bot size={18} className="text-purple-600 dark:text-purple-400" />
            )
          }
          className="normal-case border-purple-300 dark:border-purple-800 text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-950/50"
        >
          {aiLoading ? "Traitement IA en cours..." : "Relire avec l'Assistant talk"}
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

          <Box component="fieldset" sx={{ border: "none", p: 0, m: 0 }}>
            <Box component="legend" sx={{ display: "none" }}>Intervenant</Box>
            <div className="grid grid-cols-2 gap-4">
              <TextField
                {...register("speaker")}
                id="talk-speaker"
                label="Speaker"
                required
                fullWidth
                placeholder="Prénom Nom"
                error={!!errors.speaker}
                helperText={errors.speaker?.message}
              />
              <TextField
                {...register("cospeaker")}
                id="talk-cospeaker"
                label="Co-speaker"
                fullWidth
                placeholder="Prénom Nom"
              />
              <TextField
                {...register("email")}
                id="talk-email"
                label="Email"
                fullWidth
                placeholder="speaker@zenika.com"
                type="email"
                error={!!errors.email}
                helperText={errors.email?.message}
              />

              <Controller
                name="agency"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth required error={!!errors.agency}>
                    <InputLabel id="create-agency-label">Agence</InputLabel>
                    <Select
                      {...field}
                      labelId="create-agency-label"
                      id="talk-agency"
                      label="Agence"
                    >
                      {Object.entries(agencyLabels).map(([v, l]) => (
                        <MenuItem key={v} value={v}>{l}</MenuItem>
                      ))}
                    </Select>
                    {errors.agency && (
                      <FormHelperText>{errors.agency.message}</FormHelperText>
                    )}
                  </FormControl>
                )}
              />
            </div>
          </Box>

          <div className="flex flex-col gap-1">
            <div className="flex justify-between items-center mb-1">
              <span className="text-xs font-medium text-text-muted">Abstract & Description</span>
            </div>
            <TextField
              {...register("abstract")}
              id="talk-abstract"
              label="Abstract / Description"
              multiline
              rows={4}
              fullWidth
              required
              placeholder="Décrivez le contenu de votre talk..."
              error={!!errors.abstract}
              helperText={errors.abstract?.message}
            />
          </div>

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
                      {Object.entries(formatLabels).map(([v, l]) => (
                        <MenuItem key={v} value={v}>{l}</MenuItem>
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
                      {Object.entries(visibilityLabels).map(([v, l]) => (
                        <MenuItem key={v} value={v}>{l}</MenuItem>
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
                      {Object.entries(languageLabels).map(([v, l]) => (
                        <MenuItem key={v} value={v}>{l}</MenuItem>
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

      <AiReviewDialog
        open={aiDialogOpen}
        loading={aiLoading}
        originalTitle={getValues().title}
        originalAbstract={getValues().abstract}
        reviewResult={aiResult}
        onClose={() => setAiDialogOpen(false)}
        onApply={handleApplyAiSuggestions}
      />
    </Dialog>
    </DatePickerProvider>
  );
}
