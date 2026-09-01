"use client";

import { useState, useEffect } from "react";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Switch from "@mui/material/Switch";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import Divider from "@mui/material/Divider";
import Select, { type SelectChangeEvent } from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import CircularProgress from "@mui/material/CircularProgress";
import {
  Lock, Globe, X, User, MapPin, Mic, Calendar, Bot,
  Link as LinkIcon, Play as PlayIcon, ExternalLink as ExternalLinkIcon,
} from "lucide-react";
import type { TalkData, TalkStatus, TalkReviewResponse } from "@/entities/talk";
import { agencyLabels, reviewTalkAction } from "@/entities/talk";
import { isValidUrl } from "@/shared/lib";
import { StatusTag, statusConfig } from "./TalkTags";

interface TalkDetailsDialogProps {
  talk: TalkData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (talk: TalkData) => void;
  onDelete: (id: string) => void;
}

export function TalkDetailsDialog({ talk, open, onClose, onUpdate, onDelete }: TalkDetailsDialogProps) {
  const [slides, setSlides] = useState(talk?.slides ?? "");
  const [replay, setReplay] = useState(talk?.replay ?? "");
  const [aiDialogOpen, setAiDialogOpen] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<TalkReviewResponse | null>(null);

  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    setSlides(talk?.slides ?? "");
    setReplay(talk?.replay ?? "");
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [talk?.id, talk?.slides, talk?.replay]);

  if (!talk) return null;

  const handleStatusChange = (e: SelectChangeEvent) =>
    onUpdate({ ...talk, status: e.target.value as TalkStatus });

  const handleVisibilityToggle = (e: React.ChangeEvent<HTMLInputElement>) =>
    onUpdate({ ...talk, visibility: e.target.checked ? "external" : "internal" });

  const handleDelete = () => {
    if (confirm("Êtes-vous sûr de vouloir supprimer ce talk ?")) {
      onDelete(talk.id);
      onClose();
    }
  };

  const handleTriggerAiReview = async () => {
    setAiDialogOpen(true);
    setAiLoading(true);
    try {
      const res = await reviewTalkAction({
        title: talk.title,
        abstract: talk.abstract,
      });
      setAiResult(res);
    } catch {
      // Error handling
    } finally {
      setAiLoading(false);
    }
  };

  const handleApplyAiSuggestions = (suggestedTitle: string, suggestedAbstract: string) => {
    onUpdate({
      ...talk,
      title: suggestedTitle,
      abstract: suggestedAbstract,
    });
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle className="font-semibold! pb-2! flex! justify-between! items-center! tracking-[-0.75px]!">
          {talk.title}
          <div className="flex items-center gap-2 tracking-normal">
            <StatusTag status={talk.status} />
            <IconButton onClick={onClose} size="small"><X size={20} /></IconButton>
          </div>
        </DialogTitle>

        <DialogContent className="pb-2!">
          <div className="flex justify-between items-center mb-4">
            <p className="text-sm text-text-muted">Détails du talk</p>
            <Button
              variant="outlined"
              size="small"
              onClick={handleTriggerAiReview}
              disabled={aiLoading}
              startIcon={
                aiLoading ? (
                  <CircularProgress size={16} className="text-purple-600" />
                ) : (
                  <Bot size={16} className="text-purple-600 dark:text-purple-400" />
                )
              }
              className="normal-case text-xs border-purple-300 dark:border-purple-800 text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-950/50"
            >
              {aiLoading ? "Relire avec l'IA..." : "Relire avec l'IA"}
            </Button>
          </div>

          <div className="flex flex-col gap-4 mt-1">
            {/* Abstract overview if present */}
            {talk.abstract && (
              <div className="bg-slate-50 dark:bg-slate-900/50 p-3 rounded-lg border border-slate-200 dark:border-slate-800">
                <p className="text-xs font-semibold text-text-muted mb-1">Abstract actuel :</p>
                <p className="text-sm text-slate-800 dark:text-slate-200 whitespace-pre-wrap">{talk.abstract}</p>
              </div>
            )}

            {/* Info grid */}
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-4">
                <div className="flex items-center gap-2">
                  <User size={14} className="text-text-muted shrink-0" />
                  <span className="text-sm">{talk.speaker}{talk.cospeaker ? ` & ${talk.cospeaker}` : ""}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Mic size={14} className="text-text-muted shrink-0" />
                  <span className="text-sm">{talk.conference || "—"}</span>
                </div>
              </div>
              <div className="flex flex-col gap-4">
                <div className="flex items-center gap-2">
                  <MapPin size={14} className="text-text-muted shrink-0" />
                  <span className="text-sm">{agencyLabels[talk.agency] || talk.agency}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Calendar size={14} className="text-text-muted shrink-0" />
                  <span className="text-sm">{talk.date || "—"}</span>
                </div>
              </div>
            </div>

            <Divider />

            {/* Status */}
            <div>
              <p className="text-sm text-text-muted mb-2">Changer le statut</p>
              <FormControl fullWidth>
                <Select value={talk.status} onChange={handleStatusChange} size="small">
                  {Object.keys(statusConfig).map((s) => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </div>

            <Divider />

            {/* Visibility toggle */}
            <div className="flex justify-between items-center">
              <div>
                <div className="flex gap-1.5 items-center">
                  {talk.visibility === "external" ? <Globe size={16} /> : <Lock size={16} />}
                  <span className="text-sm font-medium text-text">
                    {talk.visibility === "external" ? "Visibilité externe" : "Visibilité interne"}
                  </span>
                </div>
                <span className="text-xs text-text-muted block mt-0.5">
                  {talk.visibility === "external" ? "Visible publiquement." : "Réservé en interne."}
                </span>
              </div>
              <Switch checked={talk.visibility === "external"} onChange={handleVisibilityToggle} />
            </div>

            {/* Slides & Replay */}
            {(talk.status === "Accepted" || talk.status === "Replayed") && (
              <>
                <Divider />
                <div>
                  <p className="text-sm text-text-muted mb-4">Liens du talk</p>
                  <div className="grid grid-cols-2 gap-4">
                    <TextField
                      label="Slides" placeholder="https://..." value={slides} fullWidth size="small"
                      onChange={(e) => setSlides(e.target.value)}
                      onBlur={() => { if (slides !== (talk.slides ?? "")) onUpdate({ ...talk, slides }); }}
                      onKeyDown={(e) => { if (e.key === "Enter") { onUpdate({ ...talk, slides }); (e.target as HTMLInputElement).blur(); } }}
                      slotProps={{
                        input: {
                          startAdornment: <InputAdornment position="start"><LinkIcon size={16} /></InputAdornment>,
                          endAdornment: slides && isValidUrl(slides) ? (
                            <InputAdornment position="end">
                              <IconButton size="small" component="a" href={slides} target="_blank" rel="noopener noreferrer" className="text-primary! p-0.5!">
                                <ExternalLinkIcon size={14} />
                              </IconButton>
                            </InputAdornment>
                          ) : null,
                        },
                      }}
                    />
                    <TextField
                      label="Replay" placeholder="https://..." value={replay} fullWidth size="small"
                      onChange={(e) => setReplay(e.target.value)}
                      onBlur={() => { if (replay !== (talk.replay ?? "")) onUpdate({ ...talk, replay }); }}
                      onKeyDown={(e) => { if (e.key === "Enter") { onUpdate({ ...talk, replay }); (e.target as HTMLInputElement).blur(); } }}
                      slotProps={{
                        input: {
                          startAdornment: <InputAdornment position="start"><PlayIcon size={16} /></InputAdornment>,
                          endAdornment: replay && isValidUrl(replay) ? (
                            <InputAdornment position="end">
                              <IconButton size="small" component="a" href={replay} target="_blank" rel="noopener noreferrer" className="text-primary! p-0.5!">
                                <ExternalLinkIcon size={14} />
                              </IconButton>
                            </InputAdornment>
                          ) : null,
                        },
                      }}
                    />
                  </div>
                </div>
              </>
            )}
          </div>
        </DialogContent>

        <DialogActions className="px-6! pb-6! pt-5! justify-between!">
          <Button variant="contained" onClick={handleDelete}>Supprimer</Button>
          <Button variant="outlined" onClick={onClose}>Fermer</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
