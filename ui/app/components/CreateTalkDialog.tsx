import * as React from "react";
import Button from "@mui/material/Button";
import DialogTitle from "@mui/material/DialogTitle";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Typography from "@mui/material/Typography";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select, { type SelectChangeEvent } from "@mui/material/Select";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import dayjs from "dayjs";
import type { Dayjs } from "dayjs";
import "dayjs/locale/fr";
import { useTheme } from "@mui/material/styles";

import {
  type TalkData,
  type TalkStatus,
  agencyLabels,
  visibilityLabels,
  formatLabels,
  languageLabels,
} from "../types/talk";

dayjs.locale("fr");

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => void;
}

export function CreateTalkDialog({
  open,
  onClose,
  onSubmit,
}: CreateTalkDialogProps) {
  const [title, setTitle] = React.useState("");
  const [speaker, setSpeaker] = React.useState("");
  const [cospeaker, setCospeaker] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [abstract, setAbstract] = React.useState("");
  const [format, setFormat] = React.useState("");
  const [visibility, setVisibility] = React.useState("");
  const [language, setLanguage] = React.useState("");
  const [agency, setAgency] = React.useState("");
  const [conference, setConference] = React.useState("");
  const [date, setDate] = React.useState<Dayjs | null>(null);
  const [notes, setNotes] = React.useState("");
  const [toastOpen, setToastOpen] = React.useState(false);

  const theme = useTheme();

  const resetForm = () => {
    setTitle("");
    setSpeaker("");
    setCospeaker("");
    setEmail("");
    setAbstract("");
    setFormat("");
    setVisibility("");
    setLanguage("");
    setAgency("");
    setConference("");
    setDate(null);
    setNotes("");
  };

  const isEmailValid = (emailStr: string) => {
    if (!emailStr.trim()) return true;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailStr.trim());
  };

  const handleSave = (status: TalkStatus) => {
    if (status !== "Draft") {
      const requiredFieldsMissing =
        !title.trim() ||
        !speaker.trim() ||
        !abstract.trim() ||
        !agency ||
        !format ||
        !visibility;
      const emailInvalid = !isEmailValid(email);

      if (requiredFieldsMissing || emailInvalid) {
        setToastOpen(true);
        return;
      }
    } else {
      if (!title.trim()) {
        setToastOpen(true);
        return;
      }
    }

    onSubmit({
      id: crypto.randomUUID(),
      title: title.trim(),
      speaker: speaker.trim(),
      cospeaker: cospeaker.trim(),
      email: email.trim(),
      agency,
      abstract: abstract.trim(),
      format,
      visibility,
      language,
      conference: conference.trim(),
      date: date ? date.format("DD-MM-YYYY") : "",
      notes: notes.trim(),
      status,
      slides: "",
      replay: "",
    });
    resetForm();
    onClose();
  };

  const handleSubmit = () => handleSave("Idea");
  const handleDraft = () => handleSave("Draft");

  const handleCancel = () => {
    resetForm();
    onClose();
  };

  const handleToastClose = (
    _event?: React.SyntheticEvent | Event,
    reason?: string,
  ) => {
    if (reason === "clickaway") return;
    setToastOpen(false);
  };

  const handleFormatChange = (event: SelectChangeEvent) => {
    setFormat(event.target.value as string);
  };

  const handleVisibilityChange = (event: SelectChangeEvent) => {
    setVisibility(event.target.value as string);
  };

  const handleLanguageChange = (event: SelectChangeEvent) => {
    setLanguage(event.target.value as string);
  };

  const handleAgencyChange = (event: SelectChangeEvent) => {
    setAgency(event.target.value as string);
  };

  return (
    <Dialog onClose={handleCancel} open={open} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pb: 0 }}>Nouveau Talk</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Déclarez une nouvelle idée de talk ou soumission à une conférence.
        </Typography>

        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField
              label="Titre du talk"
              id="talk-title"
              required
              fullWidth
              placeholder="Ex: Building Resilient Microservices"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Speaker"
              id="speaker"
              required
              fullWidth
              placeholder="Prénom Nom"
              value={speaker}
              onChange={(e) => setSpeaker(e.target.value)}
            />
          </Grid>
          <Grid size={6}>
            <TextField
              label="Co-speaker"
              id="cospeaker"
              fullWidth
              placeholder="Prénom Nom"
              value={cospeaker}
              onChange={(e) => setCospeaker(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Email"
              id="email"
              fullWidth
              placeholder="speaker@zenika.com"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={email.trim() !== "" && !isEmailValid(email)}
              helperText={
                email.trim() !== "" && !isEmailValid(email)
                  ? "Format d'email invalide"
                  : ""
              }
            />
          </Grid>
          <Grid size={6}>
            <FormControl fullWidth required>
              <InputLabel id="select-agency-label">Agence</InputLabel>
              <Select
                labelId="select-agency-label"
                id="select-agency"
                value={agency}
                label="Agence"
                onChange={handleAgencyChange}
              >
                {Object.entries(agencyLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid size={12}>
            <TextField
              label="Abstract / Description"
              id="abstract"
              multiline
              rows={4}
              fullWidth
              placeholder="Décrivez le contenu de votre talk..."
              required
              value={abstract}
              onChange={(e) => setAbstract(e.target.value)}
              slotProps={{
                input: { sx: { "& textarea": { resize: "vertical" } } },
              }}
            />
          </Grid>

          <Grid size={4}>
            <FormControl fullWidth required>
              <InputLabel id="select-format-label">Format</InputLabel>
              <Select
                labelId="select-format-label"
                id="select-format"
                value={format}
                label="Format"
                onChange={handleFormatChange}
              >
                {Object.entries(formatLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={4}>
            <FormControl fullWidth required>
              <InputLabel id="select-visibility-label">Visibilité</InputLabel>
              <Select
                labelId="select-visibility-label"
                id="select-visibility"
                value={visibility}
                label="Visibilité"
                onChange={handleVisibilityChange}
              >
                {Object.entries(visibilityLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={4}>
            <FormControl fullWidth>
              <InputLabel id="select-language-label">Langue</InputLabel>
              <Select
                labelId="select-language-label"
                id="select-language"
                value={language}
                label="Langue"
                onChange={handleLanguageChange}
              >
                {Object.entries(languageLabels).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid size={12}>
            <TextField
              label="Conférence cible"
              id="conference"
              fullWidth
              placeholder="Ex: Devoxx France, Sunny Tech..."
              value={conference}
              onChange={(e) => setConference(e.target.value)}
            />
          </Grid>

          <Grid size={12}>
            <DatePicker
              label="Date"
              value={date}
              onChange={(newValue) => setDate(newValue)}
              views={["year", "month", "day"]}
              format="DD/MM/YYYY"
              slotProps={{ textField: { fullWidth: true } }}
            />
          </Grid>

          <Grid size={12}>
            <TextField
              label="Notes / Commentaires"
              id="notes"
              multiline
              rows={4}
              fullWidth
              placeholder="Informations complémentaires..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              slotProps={{
                input: { sx: { "& textarea": { resize: "vertical" } } },
              }}
            />
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button variant="outlined" onClick={handleCancel}>
          Annuler
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Button
          variant="outlined"
          onClick={handleDraft}
          sx={{ color: "#bbbbbbff" }}
        >
          Sauvegarder en brouillon
        </Button>
        <Button variant="contained" onClick={handleSubmit}>
          Créer le talk
        </Button>
      </DialogActions>

      <Snackbar
        open={toastOpen}
        autoHideDuration={4000}
        onClose={handleToastClose}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert onClose={handleToastClose} severity="error" variant="filled">
          {"Merci de remplir tous les champs obligatoires."}
        </Alert>
      </Snackbar>
    </Dialog>
  );
}
