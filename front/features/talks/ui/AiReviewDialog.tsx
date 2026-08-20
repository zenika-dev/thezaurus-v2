"use client";

import { useState, useEffect } from "react";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import Divider from "@mui/material/Divider";
import {
  Bot, Save, X, MessageSquare, CheckCircle2,
  FileText, ShieldCheck, ArrowRight,
} from "lucide-react";
import type { TalkReviewResponse } from "@/entities/talk";
import { AiReviewDiffView } from "./AiReviewDiffView";

const DIALOG_TITLE_ID = "ai-review-dialog-title";

interface AiReviewDialogProps {
  open: boolean;
  loading: boolean;
  originalTitle: string;
  originalAbstract: string;
  reviewResult: TalkReviewResponse | null;
  onClose: () => void;
  onApply: (updatedTitle: string, updatedAbstract: string) => void;
}

export function AiReviewDialog({
  open,
  loading,
  originalTitle,
  originalAbstract,
  reviewResult,
  onClose,
  onApply,
}: AiReviewDialogProps) {
  const [editableTitle, setEditableTitle] = useState(originalTitle);
  const [editableAbstract, setEditableAbstract] = useState(originalAbstract);

  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    setEditableTitle(originalTitle);
    setEditableAbstract(originalAbstract);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [originalTitle, originalAbstract, open]);

  const handleSaveModifications = () => {
    onApply(editableTitle, editableAbstract);
    onClose();
  };

  // Helper to extract category icon and theme for feedback
  const getCategoryTheme = (text: string) => {
    if (text.startsWith("[Titre]")) {
      return {
        label: "Titre",
        cleanText: text.replace("[Titre]", "").trim(),
        bg: "bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-900/50 text-blue-900 dark:text-blue-200",
        badge: "bg-blue-100 dark:bg-blue-900/60 text-blue-800 dark:text-blue-200",
        icon: FileText,
      };
    }
    if (text.startsWith("[Abstract]")) {
      return {
        label: "Abstract",
        cleanText: text.replace("[Abstract]", "").trim(),
        bg: "bg-purple-50 dark:bg-purple-950/40 border-purple-200 dark:border-purple-900/50 text-purple-900 dark:text-purple-200",
        badge: "bg-purple-100 dark:bg-purple-900/60 text-purple-800 dark:text-purple-200",
        icon: MessageSquare,
      };
    }
    if (text.startsWith("[Cohérence]")) {
      return {
        label: "Cohérence",
        cleanText: text.replace("[Cohérence]", "").trim(),
        bg: "bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-900/50 text-emerald-900 dark:text-emerald-200",
        badge: "bg-emerald-100 dark:bg-emerald-900/60 text-emerald-800 dark:text-emerald-200",
        icon: ShieldCheck,
      };
    }
    return {
      label: "Analyse",
      cleanText: text,
      bg: "bg-slate-50 dark:bg-slate-900/50 border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-200",
      badge: "bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300",
      icon: CheckCircle2,
    };
  };

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onClose}
      maxWidth="lg"
      fullWidth
      aria-labelledby={DIALOG_TITLE_ID}
      slotProps={{ paper: { className: "dark:bg-slate-900 dark:bg-none" } }}
    >
      <DialogTitle id={DIALOG_TITLE_ID} className="pb-2! border-b border-slate-200 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20">
              <Bot size={22} aria-hidden="true" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-slate-900 dark:text-slate-100">
                  Relecture et Optimisation
                </span>
              </div>
              <p className="text-xs text-text-muted mt-0.5">
                Consultez les suggestions et modifiez directement le contenu original de votre talk
              </p>
            </div>
          </div>
          {!loading && (
            <IconButton onClick={onClose} size="small" aria-label="Fermer la relecture">
              <X size={20} />
            </IconButton>
          )}
        </div>
      </DialogTitle>

      <DialogContent className="pt-6! pb-6! flex flex-col gap-6">
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-4 text-center">
            <div className="relative flex items-center justify-center">
              <CircularProgress size={56} className="text-purple-600 dark:text-purple-400" />
              <div className="absolute">
                <Bot size={24} className="text-purple-600 dark:text-purple-400 animate-pulse" />
              </div>
            </div>
            <div className="flex flex-col gap-1">
              <span className="font-semibold text-base text-slate-800 dark:text-slate-200">
                Traitement en cours par l&apos;Assistant talk...
              </span>
              <p className="text-sm text-text-muted max-w-sm">
                Analyse de la pertinence du titre, la clarté de l&apos;abstract et génèration des propositions ciblées.
              </p>
            </div>
          </div>
        ) : reviewResult ? (
          <>
            {/* Actionable Recommendations */}
            {reviewResult.keyImprovements && reviewResult.keyImprovements.length > 0 && (
                <div className="bg-slate-50 dark:bg-slate-900/50 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex flex-col gap-2.5">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                  <CheckCircle2 size={16} className="text-emerald-500" />
                  Recommandations globales
                </span>
                  <ul className="space-y-1.5 text-xs text-slate-700 dark:text-slate-300">
                    {reviewResult.keyImprovements.map((imp, idx) => (
                        <li key={`imp-${idx}`} className="flex items-start gap-2 leading-relaxed">
                          <ArrowRight size={14} className="text-emerald-500 shrink-0 mt-0.5" />
                          <span>{imp}</span>
                        </li>
                    ))}
                  </ul>
                </div>
            )}

            {/* Editable Title View with Suggestions */}
            <AiReviewDiffView
              label="Titre du talk"
              value={editableTitle}
              onChange={setEditableTitle}
              suggestions={reviewResult.suggestedTitles || []}
              placeholder="Saisissez le titre de votre talk..."
            />

            <Divider className="my-1 border-slate-200 dark:border-slate-800" />

            {/* Editable Abstract View with Suggestions */}
            <AiReviewDiffView
              label="Abstract / Description"
              value={editableAbstract}
              onChange={setEditableAbstract}
              suggestions={reviewResult.suggestedAbstracts || []}
              multiline
              rows={5}
              placeholder="Saisissez l'abstract de votre talk..."
            />

            {/* Categorized Feedback Cards */}
            {reviewResult.feedback && reviewResult.feedback.length > 0 && (
              <>
                <Divider className="my-1 border-slate-200 dark:border-slate-800" />
                <div className="flex flex-col gap-3">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                    <MessageSquare size={16} className="text-purple-600" />
                    Feedbacks et remarques
                  </span>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    {reviewResult.feedback.map((fb, idx) => {
                      const theme = getCategoryTheme(fb);
                      const Icon = theme.icon;
                      return (
                        <div key={`fb-${idx}`} className={`p-3.5 rounded-xl border flex flex-col gap-1.5 ${theme.bg}`}>
                          <div className="flex items-center gap-2">
                            <span className={`text-[11px] font-bold px-2 py-0.5 rounded-md ${theme.badge}`}>
                              {theme.label}
                            </span>
                            <Icon size={14} className="shrink-0 opacity-80" />
                          </div>
                          <p className="text-xs leading-relaxed font-normal">
                            {theme.cleanText}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </>
            )}


          </>
        ) : (
          <div className="py-8 text-center text-text-muted">
            Aucune donnée de relecture disponible.
          </div>
        )}
      </DialogContent>

      <DialogActions className="px-6! pb-6! pt-4! border-t border-slate-200 dark:border-slate-800">
        <Button variant="outlined" onClick={onClose} disabled={loading}>
          Fermer sans enregistrer
        </Button>
        <div className="flex-1" />
        <Button
          variant="contained"
          onClick={handleSaveModifications}
          disabled={loading || !reviewResult}
          startIcon={<Save size={18} />}
          className="bg-purple-600 hover:bg-purple-700 text-white font-medium px-5"
        >
          Enregistrer
        </Button>
      </DialogActions>
    </Dialog>
  );
}
