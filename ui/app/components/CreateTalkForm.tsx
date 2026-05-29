import * as React from "react";
import Button from "@mui/material/Button";
import DialogTitle from "@mui/material/DialogTitle";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Switch from "@mui/material/Switch";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select, { type SelectChangeEvent } from "@mui/material/Select";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Box from "@mui/material/Box";
import InputAdornment from "@mui/material/InputAdornment";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import dayjs from "dayjs";
import type { Dayjs } from "dayjs";
import "dayjs/locale/fr";
import { useTheme } from "@mui/material/styles";
import IconButton from "@mui/material/IconButton";
import {
  Lock,
  Globe,
  Eye,
  X,
  User,
  MapPin,
  Mic,
  Calendar,
  Link as LinkIcon,
  Play as PlayIcon,
  ExternalLink as ExternalLinkIcon,
  Plus,
} from "lucide-react";

import {
  type TalkData,
  type TalkStatus,
  agencyLabels,
  visibilityLabels,
  formatLabels,
  languageLabels,
} from "../types/talk";
import { useTalks } from "../hooks/useTalks";
import { isValidUrl } from "~/lib/utils";

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

const statusConfig: Record<TalkStatus, { text: string; bg: string }> = {
  Draft: { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Idea: { text: "#007fff", bg: "rgba(0, 127, 255, 0.12)" },
  Submitted: { text: "#f59f0a", bg: "rgba(245, 159, 10, 0.12)" },
  Accepted: { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
  Replayed: { text: "#d32f2f", bg: "rgba(211, 47, 47, 0.12)" },
};

function StatusTag({ status }: { status: TalkStatus }) {
  const config = statusConfig[status];
  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.5,
        borderRadius: 1,
        display: "inline-block",
        fontSize: "0.75rem",
        fontWeight: "bold",
        border: `1px solid ${config.bg}`,
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      {status}
    </Box>
  );
}

function VisibilityTag({ visibility }: { visibility: string }) {
  const isExternal = visibility === "external";
  const label = visibilityLabels[visibility] || visibility;
  const config = isExternal
    ? { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)", Icon: Globe }
    : { text: "#757575", bg: "rgba(117, 117, 117, 0.12)", Icon: Lock };

  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.8,
        borderRadius: 1,
        border: `1px solid ${config.bg}`,
        display: "inline-flex",
        alignItems: "center",
        gap: 0.5,
        fontSize: "0.75rem",
        lineHeight: 1,
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      <config.Icon size={12} />
      <span>{label}</span>
    </Box>
  );
}

interface TalkDetailsDialogProps {
  talk: TalkData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (talk: TalkData) => void;
  onDelete: (id: string) => void;
}

export function TalkDetailsDialog({
  talk,
  open,
  onClose,
  onUpdate,
  onDelete,
}: TalkDetailsDialogProps) {
  if (!talk) return null;

  const [slides, setSlides] = React.useState(talk.slides || "");
  const [replay, setReplay] = React.useState(talk.replay || "");

  React.useEffect(() => {
    setSlides(talk.slides || "");
    setReplay(talk.replay || "");
  }, [talk.id, talk.slides, talk.replay]);

  const handleStatusChange = (event: SelectChangeEvent) => {
    onUpdate({ ...talk, status: event.target.value as TalkStatus });
  };

  const handleVisibilityToggle = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    onUpdate({
      ...talk,
      visibility: event.target.checked ? "external" : "internal",
    });
  };

  const handleSlidesBlur = () => {
    if (slides !== (talk.slides || "")) {
      onUpdate({ ...talk, slides });
    }
  };

  const handleReplayBlur = () => {
    if (replay !== (talk.replay || "")) {
      onUpdate({ ...talk, replay });
    }
  };

  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLDivElement>,
    field: "slides" | "replay",
  ) => {
    if (e.key === "Enter") {
      e.preventDefault();
      if (field === "slides") {
        onUpdate({ ...talk, slides });
      } else {
        onUpdate({ ...talk, replay });
      }
      (e.target as HTMLInputElement).blur();
    }
  };

  const handleDelete = () => {
    if (confirm("Êtes-vous sûr de vouloir supprimer ce talk ?")) {
      onDelete(talk.id);
      onClose();
    }
  };

  const theme = useTheme();

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle
        sx={{
          fontWeight: 600,
          pb: 1,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          letterSpacing: "-0.75px",
        }}
      >
        {talk.title}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            letterSpacing: "normal",
          }}
        >
          <StatusTag status={talk.status} />
          <IconButton onClick={onClose} size="small" sx={{ ml: 1 }}>
            <X size={20} />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent sx={{ pb: 1 }}>
        <Typography
          variant="body2"
          sx={{ color: theme.palette.text.secondary, mb: 3 }}
        >
          Détails du talk
        </Typography>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Grid container spacing={2}>
            <Grid size={6}>
              <Stack spacing={2}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <User size={14} color={theme.palette.text.secondary} />
                  <Typography variant="body2">
                    {talk.speaker}
                    {talk.cospeaker ? ` & ${talk.cospeaker}` : ""}
                  </Typography>
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <Mic size={14} color={theme.palette.text.secondary} />
                  <Typography variant="body2">
                    {talk.conference || "—"}
                  </Typography>
                </Box>
              </Stack>
            </Grid>
            <Grid size={6}>
              <Stack spacing={2}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <MapPin size={14} color={theme.palette.text.secondary} />
                  <Typography variant="body2">
                    {agencyLabels[talk.agency] || talk.agency}
                  </Typography>
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <Calendar size={14} color={theme.palette.text.secondary} />
                  <Typography variant="body2">{talk.date || "—"}</Typography>
                </Box>
              </Stack>
            </Grid>
          </Grid>

          <Divider />

          <Box>
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ display: "block", mb: 1 }}
            >
              Changer le statut
            </Typography>
            <FormControl fullWidth>
              <Select
                value={talk.status}
                onChange={handleStatusChange}
                size="small"
              >
                {Object.keys(statusConfig).map((status) => (
                  <MenuItem key={status} value={status}>
                    {status}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          <Divider />

          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <Box>
              <Box sx={{ display: "flex", gap: 0.75, alignItems: "center" }}>
                {talk.visibility === "external" ? (
                  <Globe size={16} />
                ) : (
                  <Lock size={16} />
                )}
                <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                  {talk.visibility === "external"
                    ? "Visibilité externe"
                    : "Visibilité interne"}
                </Typography>
              </Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block" }}
              >
                {talk.visibility === "external"
                  ? "Visible publiquement."
                  : "Réservé en interne."}
              </Typography>
            </Box>
            <Switch
              checked={talk.visibility === "external"}
              onChange={handleVisibilityToggle}
              sx={{
                width: 50,
                height: 26,
                padding: 0,
                "& .MuiSwitch-switchBase": {
                  padding: 0,
                  margin: "2px",
                  transitionDuration: "300ms",
                  "&.Mui-checked": {
                    transform: "translateX(24px)",
                    color: "#fff",
                    "& + .MuiSwitch-track": {
                      backgroundColor: "primary.main",
                      opacity: 1,
                      border: 0,
                    },
                  },
                },
                "& .MuiSwitch-thumb": {
                  boxSizing: "border-box",
                  width: 22,
                  height: 22,
                },
                "& .MuiSwitch-track": {
                  borderRadius: 13,
                  backgroundColor: "#E9E9EA",
                  opacity: 1,
                },
              }}
            />
          </Box>

          {(talk.status === "Accepted" || talk.status === "Replayed") && (
            <>
              <Divider />
              <Box>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ display: "block", mb: 1.5 }}
                >
                  Liens du talk
                </Typography>
                <Grid container spacing={2}>
                  <Grid size={6}>
                    <TextField
                      label="Slides"
                      placeholder="https://..."
                      value={slides}
                      onChange={(e) => setSlides(e.target.value)}
                      onBlur={handleSlidesBlur}
                      onKeyDown={(e) => handleKeyDown(e, "slides")}
                      fullWidth
                      size="small"
                      slotProps={{
                        input: {
                          startAdornment: (
                            <InputAdornment position="start">
                              <LinkIcon size={16} />
                            </InputAdornment>
                          ),
                          endAdornment:
                            slides && isValidUrl(slides) ? (
                              <InputAdornment position="end">
                                <IconButton
                                  size="small"
                                  href={slides}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  sx={{
                                    color: "primary.main",
                                    p: 0.5,
                                    "&:hover": {
                                      backgroundColor:
                                        "rgba(237, 33, 60, 0.08)",
                                    },
                                  }}
                                >
                                  <ExternalLinkIcon size={14} />
                                </IconButton>
                              </InputAdornment>
                            ) : null,
                        },
                      }}
                    />
                  </Grid>
                  <Grid size={6}>
                    <TextField
                      label="Replay"
                      placeholder="https://..."
                      value={replay}
                      onChange={(e) => setReplay(e.target.value)}
                      onBlur={handleReplayBlur}
                      onKeyDown={(e) => handleKeyDown(e, "replay")}
                      fullWidth
                      size="small"
                      slotProps={{
                        input: {
                          startAdornment: (
                            <InputAdornment position="start">
                              <PlayIcon size={16} />
                            </InputAdornment>
                          ),
                          endAdornment:
                            replay && isValidUrl(replay) ? (
                              <InputAdornment position="end">
                                <IconButton
                                  size="small"
                                  href={replay}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  sx={{
                                    color: "primary.main",
                                    p: 0.5,
                                    "&:hover": {
                                      backgroundColor:
                                        "rgba(237, 33, 60, 0.08)",
                                    },
                                  }}
                                >
                                  <ExternalLinkIcon size={14} />
                                </IconButton>
                              </InputAdornment>
                            ) : null,
                        },
                      }}
                    />
                  </Grid>
                </Grid>
              </Box>
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions
        sx={{ px: 3, pb: 3, pt: 2.5, justifyContent: "space-between" }}
      >
        <Button
          variant="contained"
          onClick={handleDelete}
          sx={{
            fontWeight: 600,
            boxShadow: "none",
            borderRadius: "8px",
            backgroundColor: theme.palette.primary.main,
            "&:hover": {
              backgroundColor: theme.palette.primary.light,
              boxShadow: "none",
            },
          }}
        >
          Supprimer
        </Button>
        <Button
          onClick={onClose}
          variant="outlined"
          sx={{
            fontWeight: 600,
            borderRadius: "8px",
            color: "text.primary",
            borderColor: "divider",
            "&:hover": {
              borderColor: "divider",
              backgroundColor: "rgba(237, 33, 60, 0.12)",
              color: "primary.main",
            },
          }}
        >
          Fermer
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function TalkDashboard() {
  const [open, setOpen] = React.useState(false);
  const [selectedTalkId, setSelectedTalkId] = React.useState<string | null>(
    null,
  );

  const { talks, loading, createTalk, updateTalk, deleteTalk } = useTalks();

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const handleSubmit = async (talk: TalkData) => {
    try {
      await createTalk(talk);
    } catch (error) {
      alert("Erreur lors de la création du talk");
    }
  };

  const handleUpdateTalk = async (updatedTalk: TalkData) => {
    try {
      await updateTalk(updatedTalk);
    } catch (error) {
      alert("Erreur lors de la mise à jour du talk");
    }
  };

  const handleDeleteTalk = async (id: string) => {
    try {
      await deleteTalk(id);
    } catch (error) {
      alert("Erreur lors de la suppression du talk");
    }
  };

  const selectedTalk = talks.find((t) => t.id === selectedTalkId) || null;
  const theme = useTheme();

  return (
    <Box sx={{ p: 4 }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Box>
          <Typography variant="h4" sx={{ fontWeight: "bold" }}>
            Talks
          </Typography>
          <Typography
            variant="body2"
            sx={{ color: theme.palette.text.secondary }}
          >
            Manage the lifecycle of talks from idea to replay.
          </Typography>
        </Box>
        <Button
          variant="contained"
          onClick={handleOpen}
          sx={{
            fontWeight: "bold",
            py: 1,
            gap: 1,
            boxShadow: "none",
            borderRadius: "20px",
            background: "linear-gradient(135deg, #ed213c 0%, #BF1D67 100%)",
            transition:
              "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
            "&:hover": {
              boxShadow: "none",
              transform: "translateY(-2px)",
            },
          }}
        >
          <Plus size={16} />
          New Talk
        </Button>
      </Box>

      <TableContainer
        component={Paper}
        variant="outlined"
        sx={{
          border: "1px solid #ed213c",
          overflow: "hidden",
          borderRadius: 1,
        }}
      >
        <Table>
          <TableHead sx={{ backgroundColor: "#ececec" }}>
            <TableRow>
              <TableCell>
                <strong>Titre</strong>
              </TableCell>
              <TableCell>
                <strong>Speaker</strong>
              </TableCell>
              <TableCell>
                <strong>Agence</strong>
              </TableCell>
              <TableCell>
                <strong>Conférence</strong>
              </TableCell>
              <TableCell>
                <strong>Statut</strong>
              </TableCell>
              <TableCell align="center">
                <strong>Visibilité</strong>
              </TableCell>
              <TableCell align="center">
                <strong>Actions</strong>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody sx={{ "& tr:last-child td": { borderBottom: 0 } }}>
            {talks.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={9}
                  align="center"
                  sx={{ py: 4, color: "text.secondary" }}
                >
                  Aucun talk pour le moment. Créez-en-un avec "New Talk" !
                </TableCell>
              </TableRow>
            ) : (
              talks.map((talk) => (
                <TableRow key={talk.id} hover>
                  <TableCell>
                    <Typography
                      variant="body2"
                      sx={{
                        cursor: "pointer",
                        color: "primary.main",
                        textDecoration: "underline",
                        fontWeight: "medium",
                        "&:hover": { color: "primary.dark" },
                      }}
                      onClick={() => setSelectedTalkId(talk.id)}
                    >
                      {talk.title}
                    </Typography>
                  </TableCell>
                  <TableCell>{talk.speaker}</TableCell>
                  <TableCell>{agencyLabels[talk.agency] || "—"}</TableCell>
                  <TableCell>{talk.conference || "—"}</TableCell>
                  <TableCell>
                    <StatusTag status={talk.status} />
                  </TableCell>
                  <TableCell align="center">
                    <VisibilityTag visibility={talk.visibility} />
                  </TableCell>
                  <TableCell align="center">
                    {(talk.status === "Accepted" ||
                      talk.status === "Replayed") && (
                      <Box
                        onClick={() => setSelectedTalkId(talk.id)}
                        sx={{
                          px: 1.5,
                          py: 0.8,
                          borderRadius: 0.5,
                          display: "inline-flex",
                          alignItems: "center",
                          gap: 0.5,
                          fontSize: "0.75rem",
                          lineHeight: 1,
                          color: "text.primary",
                          backgroundColor: "common.white",
                          border: "1px solid",
                          borderColor: "divider",
                          cursor: "pointer",
                          "&:hover": {
                            backgroundColor: "rgba(237, 33, 60, 0.12)",
                            color: "primary.main",
                          },
                        }}
                      >
                        <Eye size={12} />
                        <span>Détail</span>
                      </Box>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <CreateTalkDialog
        open={open}
        onClose={handleClose}
        onSubmit={handleSubmit}
      />

      <TalkDetailsDialog
        talk={selectedTalk}
        open={!!selectedTalkId}
        onClose={() => setSelectedTalkId(null)}
        onUpdate={handleUpdateTalk}
        onDelete={handleDeleteTalk}
      />
    </Box>
  );
}
