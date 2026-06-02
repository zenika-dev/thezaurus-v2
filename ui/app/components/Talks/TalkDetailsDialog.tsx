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
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Select, { type SelectChangeEvent } from "@mui/material/Select";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import Box from "@mui/material/Box";
import InputAdornment from "@mui/material/InputAdornment";
import { useTheme } from "@mui/material/styles";
import IconButton from "@mui/material/IconButton";
import {
  Lock,
  Globe,
  X,
  User,
  MapPin,
  Mic,
  Calendar,
  Link as LinkIcon,
  Play as PlayIcon,
  ExternalLink as ExternalLinkIcon,
} from "lucide-react";

import { type TalkData, type TalkStatus, agencyLabels } from "../../types/talk";
import { isValidUrl } from "~/lib/utils";
import { StatusTag, statusConfig } from "./TalkTags";

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
