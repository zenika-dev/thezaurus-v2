"use client";

import { Alert, Button, Dialog, DialogTitle, DialogContent, DialogActions, Switch, MenuItem,
  FormControl, Divider, Select, TextField, InputAdornment, IconButton } from "@mui/material";
import { Lock, Globe, X, Users, MapPin, Mic, Calendar, Bot,
  Link as LinkIcon, Play as PlayIcon, ExternalLink as ExternalLinkIcon } from "lucide-react";
import { TalkStatus } from "@/shared/api";
import { type TalkData, agencyLabels, talkStatusConfig } from "@/entities/talk";
import { SpeakerChip } from "@/entities/user";
import { isValidUrl } from "@/shared/lib";
import { StatusTag } from "./TalkTags";

interface TalkReadOnlyDialogProps {
  talk: TalkData;
  onClose: () => void;
  success?: string;
}

/** Présentation historique de la fiche, sans action de modification. */
export function TalkReadOnlyDialog({ talk, onClose, success }: TalkReadOnlyDialogProps) {
  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle className="font-semibold! pb-2! flex! justify-between! items-center! tracking-[-0.75px]!">
        {talk.title}
        <div className="flex items-center gap-2 tracking-normal">
          <StatusTag status={talk.status} />
          <IconButton aria-label="Fermer" onClick={onClose} size="small"><X size={20} /></IconButton>
        </div>
      </DialogTitle>

      <DialogContent className="pb-2!">
        {success && <Alert severity="success">{success}</Alert>}
        <div className="flex justify-between items-center mb-4">
          <p className="text-sm text-text-muted">Détails du talk</p>
          <Button
            variant="outlined"
            size="small"
            disabled
            startIcon={<Bot size={16} className="text-purple-600 dark:text-purple-400" />}
            className="normal-case text-xs border-purple-300 dark:border-purple-800 text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-950/50"
          >
            Relire avec l’IA
          </Button>
        </div>

        <div className="flex flex-col gap-4 mt-1">
          {talk.description && (
            <div className="bg-slate-50 dark:bg-slate-900/50 p-3 rounded-lg border border-slate-200 dark:border-slate-800">
              <p className="text-xs font-semibold text-text-muted mb-1">Abstract actuel :</p>
              <p className="text-sm text-slate-800 dark:text-slate-200 whitespace-pre-wrap">{talk.description}</p>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-4 col-span-2">
              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Users size={14} className="text-text-muted shrink-0" />
                    <span className="text-xs font-semibold text-text-muted">Intervenants :</span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-1.5 pl-5">
                  {talk.speakers && talk.speakers.length > 0 ? (
                    talk.speakers.map((speaker, index) => (
                      <SpeakerChip
                        key={index}
                        name={speaker.name}
                        email={speaker.email}
                        size="small"
                      />
                    ))
                  ) : (
                    <span className="text-sm text-text-muted">—</span>
                  )}
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <Mic size={14} className="text-text-muted shrink-0" />
                <span className="text-sm">{talk.conference?.name || "—"}</span>
              </div>
            </div>
            <div className="flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <MapPin size={14} className="text-text-muted shrink-0" />
                <span className="text-sm">{agencyLabels[talk.office] || talk.office}</span>
              </div>
              <div className="flex items-center gap-2">
                <Calendar size={14} className="text-text-muted shrink-0" />
                <span className="text-sm">{talk.date || "—"}</span>
              </div>
            </div>
          </div>

          <Divider />

          <div>
            <p className="text-sm text-text-muted mb-2">Changer le statut</p>
            <FormControl fullWidth>
              <Select value={talk.status} disabled size="small">
                {TalkStatus.map((s) => (
                  <MenuItem key={s} value={s}>{talkStatusConfig[s].label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </div>

          <Divider />

          <div className="flex justify-between items-center">
            <div>
              <div className="flex gap-1.5 items-center">
                {talk.visibility === "PUBLIC" ? <Globe size={16} /> : <Lock size={16} />}
                <span className="text-sm font-medium text-text">
                  {talk.visibility === "PUBLIC" ? "Visibilité externe" : "Visibilité interne"}
                </span>
              </div>
              <span className="text-xs text-text-muted block mt-0.5">
                {talk.visibility === "PUBLIC" ? "Visible publiquement." : "Réservé en interne."}
              </span>
            </div>
            <Switch checked={talk.visibility === "PUBLIC"} disabled />
          </div>

          {(talk.status === "ACCEPTED" || talk.status === "DONE") && (
            <>
              <Divider />
              <div>
                <p className="text-sm text-text-muted mb-4">Liens et restitution</p>
                <div className="flex flex-col gap-4">
                  <div className="grid grid-cols-2 gap-4">
                    <TextField
                      label="Slides" placeholder="https://..." value={talk.slides ?? ""} fullWidth size="small"
                      slotProps={{
                        input: {
                          readOnly: true,
                          startAdornment: <InputAdornment position="start"><LinkIcon size={16} /></InputAdornment>,
                          endAdornment: talk.slides && isValidUrl(talk.slides) ? (
                            <InputAdornment position="end">
                              <IconButton size="small" component="a" href={talk.slides} target="_blank" rel="noopener noreferrer" className="text-primary! p-0.5!">
                                <ExternalLinkIcon size={14} />
                              </IconButton>
                            </InputAdornment>
                          ) : null,
                        },
                      }}
                    />
                    <TextField
                      label="Replay" placeholder="https://..." value={talk.replay ?? ""} fullWidth size="small"
                      slotProps={{
                        input: {
                          readOnly: true,
                          startAdornment: <InputAdornment position="start"><PlayIcon size={16} /></InputAdornment>,
                          endAdornment: talk.replay && isValidUrl(talk.replay) ? (
                            <InputAdornment position="end">
                              <IconButton size="small" component="a" href={talk.replay} target="_blank" rel="noopener noreferrer" className="text-primary! p-0.5!">
                                <ExternalLinkIcon size={14} />
                              </IconButton>
                            </InputAdornment>
                          ) : null,
                        },
                      }}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <TextField
                      label="Audience"
                      placeholder="Ex : 150"
                      type="number"
                      value={talk.audience ?? ""}
                      fullWidth
                      size="small"
                      slotProps={{
                        htmlInput: { min: 0, step: 1 },
                        input: {
                          readOnly: true,
                          startAdornment: (
                            <InputAdornment position="start">
                              <Users size={16} />
                            </InputAdornment>
                          ),
                        },
                      }}
                    />
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </DialogContent>

      <DialogActions className="px-6! pb-6! pt-5! justify-between!">
        <div className="flex-1" />
        <Button variant="outlined" onClick={onClose}>Fermer</Button>
      </DialogActions>
    </Dialog>
  );
}
