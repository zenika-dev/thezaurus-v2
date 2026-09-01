import { useState } from "react";

import {
  Sparkles,
  MessageSquare,
  FileText,
  Check,
  Plus,
  Wand2,
  Loader2,
  AlertTriangle,
} from "lucide-react";

import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Input from "@mui/material/Input";
import TextareaAutosize from "@mui/material/TextareaAutosize";

import { TalkReviewResponse } from "@/entities/talk";

interface TalkAssistantDialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  abstract: string;
  loading: boolean;
  error: string | null;
  assistantReviewResult: TalkReviewResponse | null;
  onApply: (title: string, abstract: string) => void;
}

type Feedback = {
  label: string;
  text: string;
  tone: "title" | "abstract" | "coherence";
};

const toneClasses: Record<Feedback["tone"], string> = {
  title: "border-primary/30 bg-primary/5",
  abstract: "border-border bg-muted/40",
  coherence: "border-emerald-500/30 bg-emerald-500/5",
};

/**
 * Transforme le tableau `feedback` de chaînes de caractères (ex: "[Titre] ...")
 * renvoyé par l'API Quarkus en objets structurels Feedback pour la vue UI.
 */
function parseFeedbackItems(feedbackList: string[] = []): Feedback[] {
  return feedbackList.map((item) => {
    if (item.startsWith("[Titre]")) {
      return {
        label: "Titre",
        tone: "title",
        text: item.replace("[Titre]", "").trim(),
      };
    }
    if (item.startsWith("[Abstract]")) {
      return {
        label: "Abstract",
        tone: "abstract",
        text: item.replace("[Abstract]", "").trim(),
      };
    }
    if (item.startsWith("[Cohérence]")) {
      return {
        label: "Cohérence",
        tone: "coherence",
        text: item.replace("[Cohérence]", "").trim(),
      };
    }
    return {
      label: "Avis",
      tone: "abstract",
      text: item,
    };
  });
}

export function TalkAssistantDialog({
  open,
  onClose,
  title,
  abstract,
  loading,
  error,
  assistantReviewResult,
  onApply,
}: TalkAssistantDialogProps) {
  const [prevOpen, setPrevOpen] = useState(open);
  const [draftTitle, setDraftTitle] = useState(title);
  const [draftAbstract, setDraftAbstract] = useState(abstract);

  if (open !== prevOpen) {
    setPrevOpen(open);
    if (open) {
      setDraftTitle(title);
      setDraftAbstract(abstract);
    }
  }

  const handleApplyUpdate = () => {
    onApply(draftTitle, draftAbstract);
    onClose();
  };

  const feedbackList = parseFeedbackItems(assistantReviewResult?.feedback);
  const titleSuggestions = assistantReviewResult?.suggestedTitles || [];
  const abstractSuggestions = assistantReviewResult?.suggestedAbstracts || [];

  return (
    <Dialog
      open={open}
      maxWidth="md"
      fullWidth
      onClose={loading ? undefined : onClose}
    >
      <DialogTitle className="flex flex-col space-y-1.5 text-left font-display text-lg">
        <div className="flex items-center gap-2 text-lg font-semibold leading-none tracking-tight">
          <Sparkles className="h-4 w-4 text-primary" aria-hidden="true" />
          Assistant talk — relecture
        </div>
        <p className="text-sm font-normal text-muted-foreground">
          Suggestions facultatives : gardez votre texte ou piochez ce qui vous aide.
        </p>
      </DialogTitle>

      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto space-y-4 pt-2">
        {loading && (
          <div className="flex flex-col items-center justify-center py-12 space-y-3">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-sm font-medium text-muted-foreground">
              L&#39;Assistant révise votre talk en direct...
            </p>
          </div>
        )}

        {error && !loading && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
            <div className="flex-1 text-sm">
              <p className="font-semibold text-destructive">Échec de l&#39;analyse</p>
              <p className="text-muted-foreground mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {!loading && !error && (
          <>

            {feedbackList.length > 0 && (
              <div className="grid gap-2 sm:grid-cols-3">
                {feedbackList.map((f) => (
                  <div
                    key={f.label}
                    className={`rounded-lg border p-2.5 ${toneClasses[f.tone]}`}
                  >
                    <p className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
                      <MessageSquare className="h-3 w-3" aria-hidden="true" />
                      {f.label}
                    </p>
                    <p className="mt-1 text-xs leading-snug text-muted-foreground">
                      {f.text}
                    </p>
                  </div>
                ))}
              </div>
            )}

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="assistant-title"
                  className="flex items-center gap-1.5 text-sm font-semibold"
                >
                  <FileText className="h-3.5 w-3.5 text-primary" aria-hidden="true" />{" "}
                  Titre du talk
                </label>
                <span className="text-xs text-muted-foreground">
                  {draftTitle.length} caractères
                </span>
              </div>
              <Input
                id="assistant-title"
                fullWidth
                value={draftTitle}
                onChange={(e) => setDraftTitle(e.target.value)}
                className="w-full"
              />
              <div className="flex flex-wrap gap-1.5">
                {titleSuggestions.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setDraftTitle(s)}
                    className="max-w-full truncate rounded-full border border-border bg-background px-2.5 py-1 text-xs text-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
                    title={s}
                  >
                    <Wand2 className="mr-1 inline h-3 w-3" aria-hidden="true" />
                    {s}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="assistant-abstract"
                  className="flex items-center gap-1.5 text-sm font-semibold"
                >
                  <FileText className="h-3.5 w-3.5 text-primary" aria-hidden="true" />{" "}
                  Abstract / Description
                </label>
                <span className="text-xs text-muted-foreground">
                  {draftAbstract.length} caractères
                </span>
              </div>
              <TextareaAutosize
                id="assistant-abstract"
                minRows={7}
                value={draftAbstract}
                onChange={(e) => setDraftAbstract(e.target.value)}
                className="w-full rounded-md border border-input bg-background p-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              <div className="space-y-1.5">
                {abstractSuggestions.map((s) => (
                  <div
                    key={s}
                    className="flex items-start gap-2 rounded-lg border border-border bg-muted/30 p-2"
                  >
                    <p className="flex-1 text-xs leading-snug text-muted-foreground">
                      {s}
                    </p>
                    <button
                      type="button"
                      onClick={() =>
                        setDraftAbstract((prev) =>
                          `${prev.trim()} ${s.split("« ")[1]?.replace(" »", "") ?? s}`.trim()
                        )
                      }
                      className="inline-flex shrink-0 items-center gap-1 rounded-full border border-border bg-background px-2 py-0.5 text-xs font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
                    >
                      <Plus className="h-3 w-3" aria-hidden="true" /> Ajouter
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </DialogContent>

      <DialogActions className="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2 p-6 pt-0">
        <Button
          type="button"
          variant="outlined"
          onClick={onClose}
        >
          Fermer sans appliquer
        </Button>
        <Button
          type="button"
          variant="contained"
          disabled={loading}
          onClick={handleApplyUpdate}
        >
          <Check className="mr-1.5 h-4 w-4" aria-hidden="true" />
          Appliquer au formulaire
        </Button>
      </DialogActions>
    </Dialog>
  );
}