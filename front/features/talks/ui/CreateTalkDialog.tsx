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
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import dayjs, { type Dayjs } from "dayjs";
import "dayjs/locale/fr";
import type { TalkData, TalkStatus } from "@/entities/talk";
import { agencyLabels, visibilityLabels, formatLabels, languageLabels } from "@/entities/talk";
import { talkFormSchema, type TalkFormData } from "@/entities/talk";

dayjs.locale("fr");

const DIALOG_TITLE_ID = "create-talk-dialog-title";

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => void;
}

export function CreateTalkDialog({ open, onClose, onSubmit }: CreateTalkDialogProps) {
  const [date, setDate] = useState<Dayjs | null>(null);

  const {
    register,
    handleSubmit,
    control,
    trigger,
    getValues,
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
    onClose();
  };

  const handleSaveDraft = async () => {
    const titleValid = await trigger("title");
    if (!titleValid) return;
    onSubmit(buildTalkData(getValues(), "Draft"));
    reset();
    setDate(null);
    onClose();
  };

  const onCreateTalk = (data: TalkFormData) => {
    onSubmit(buildTalkData(data, "Idea"));
    reset();
    setDate(null);
    onClose();
  };

  return (
    <DatePickerProvider>
    <Dialog
      onClose={handleCancel}
      open={open}
      maxWidth="md"
      fullWidth
      aria-labelledby={DIALOG_TITLE_ID}
    >
      <DialogTitle id={DIALOG_TITLE_ID} className="pb-0!">
        Nouveau Talk
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

      <DialogActions>
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
    </Dialog>
    </DatePickerProvider>
  );
}
