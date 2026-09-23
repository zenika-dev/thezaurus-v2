"use client";

import { useState } from "react";
import { useSession } from "next-auth/react";
import { Alert, Button } from "@mui/material";
import dayjs from "dayjs";
import { type BackendTalkReviewResponse } from "@/shared/api";
import { type TalkData, talkFormSchema, reviewTalkAction } from "@/entities/talk";
import { TalkAssistantDialog } from "./TalkAssistantDialog";
import { TalkFormDialog } from "./TalkFormDialog";
import { TalkReadOnlyDialog } from "./TalkReadOnlyDialog";

interface TalkDetailsDialogProps {
  talk: TalkData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (talk: TalkData) => Promise<TalkData>;
  onDelete: (id: string) => Promise<void>;
}

function mayEdit(talk: TalkData, email?: string | null, roles: readonly string[] = []) {
  return roles.includes("ADMIN") || roles.includes("DT") ||
    (roles.includes("CONSULTANT") && Boolean(email?.trim()) && talk.speakers.some(
      speaker => speaker.email?.trim().toLowerCase() === email?.trim().toLowerCase()));
}

export function TalkDetailsDialog(props: TalkDetailsDialogProps) {
  if (!props.open || !props.talk) return null;
  return <TalkEditor key={props.talk.id} {...props} talk={props.talk} />;
}

function TalkEditor({ talk, onClose, onUpdate, onDelete }: Omit<TalkDetailsDialogProps, "talk"> & { talk: TalkData }) {
  const { data: session } = useSession();
  const [saved, setSaved] = useState(talk);
  const [draft, setDraft] = useState(talk);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState("");
  const [pending, setPending] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiResult, setAiResult] = useState<BackendTalkReviewResponse | null>(null);
  const roles = session?.user?.roles ?? [];
  const editable = mayEdit(saved, session?.user?.email, roles) && mayEdit(talk, session?.user?.email, roles);
  const losesAccess = editable && !mayEdit(draft, session?.user?.email, roles);
  const disabled = !editable || pending;
  const change = <K extends keyof TalkData>(field: K, value: TalkData[K]) => {
    if (disabled) return;
    setDraft(current => ({ ...current, [field]: value }));
    setSuccess("");
  };
  const save = async () => {
    if (disabled) return;
    const normalizeLink = (value?: string) => (value ?? "").trim().replace(/^https?:/i, scheme => scheme.toLowerCase());
    const payload = { ...draft, slides: normalizeLink(draft.slides), replay: normalizeLink(draft.replay) };
    const result = talkFormSchema.safeParse({ ...payload, conference: payload.conference?.name ?? "" });
    const issues: Record<string, string> = {};
    if (!result.success) for (const issue of result.error.issues) issues[String(issue.path[0])] = issue.message;
    for (const field of ["slides", "replay"] as const) {
      if (payload[field] && !/^https?:\/\/[^\s]+$/.test(payload[field])) issues[field] = "Une URL HTTP ou HTTPS est requise";
    }
    if (draft.audience != null && (!Number.isInteger(draft.audience) || draft.audience < 0)) issues.audience = "Indiquez un entier positif ou zéro";
    if (draft.date && !dayjs(draft.date).isValid()) issues.date = "Indiquez une date valide";
    setErrors(issues);
    if (Object.keys(issues).length) return;
    setPending(true); setError(null);
    try {
      const updated = await onUpdate(payload);
      setSaved(updated); setDraft(updated);
      setSuccess(losesAccess ? "Modifications enregistrées. Vous ne faites plus partie des speakers : ce talk est maintenant en lecture seule." : "Modifications enregistrées.");
      onClose();
    } catch { setError("Enregistrement impossible. Vos modifications sont conservées ; vous pouvez réessayer."); }
    finally { setPending(false); }
  };
  const remove = async () => {
    if (!confirm("Êtes-vous sûr de vouloir supprimer ce talk ?")) return;
    setPending(true); setError(null);
    try { await onDelete(saved.id); onClose(); }
    catch { setError("La suppression a échoué. Vous pouvez réessayer."); setPending(false); }
  };
  const review = async () => {
    setAiOpen(true); setAiLoading(true); setAiError(null);
    try { setAiResult(await reviewTalkAction({ title: draft.title, abstract: draft.description })); }
    catch { setAiError("L’assistant est indisponible. Réessayez plus tard."); }
    finally { setAiLoading(false); }
  };
  if (!editable) return <TalkReadOnlyDialog talk={saved} onClose={onClose} success={success} />;

  return <>
    <TalkFormDialog open title={saved.title} description="Consultez les informations du talk."
      value={draft} onChange={change} onClose={onClose} onSubmit={save} onReview={review}
      assistantLoading={aiLoading} pending={pending} disabled={disabled} errors={errors} existingTalk
      submitLabel={pending ? "Enregistrement…" : "Enregistrer"}
      feedback={<>
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
      </>}
      speakerWarning={losesAccess && <Alert severity="warning">Après enregistrement, vous ne pourrez plus modifier ce talk car vous vous êtes retiré des speakers.</Alert>}
      secondaryActions={roles.includes("ADMIN") && <Button color="error" disabled={pending} onClick={remove}>Supprimer</Button>} />
    {editable && <TalkAssistantDialog open={aiOpen} loading={aiLoading} error={aiError} title={draft.title} abstract={draft.description}
      assistantReviewResult={aiResult} onClose={() => setAiOpen(false)} onApply={(title, description) => {
        setDraft(current => ({ ...current, title, description })); setAiOpen(false); setSuccess("");
      }} />}
  </>;
}
