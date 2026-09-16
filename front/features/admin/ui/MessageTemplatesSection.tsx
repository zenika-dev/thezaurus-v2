"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Alert, Autocomplete, Button, CircularProgress, Paper, TextField } from "@mui/material";
import { talkApi } from "@/entities/talk";
import { queryKeys, type BackendReminderTemplateView, type BackendReminderTemplatePreview } from "@/shared/api";
import { reminderTemplateApi, ReminderTemplateError } from "../api/reminder-template";
import { useUnsavedTemplate } from "../model/useUnsavedTemplate";
import { ReminderBodyEditor, templateVariables } from "./ReminderBodyEditor";

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Une erreur est survenue. Veuillez réessayer.";
}

function PreviewBody({ html }: { html: string }) {
  return <iframe title="Corps de l’email" sandbox="" referrerPolicy="no-referrer" className="w-full min-h-72 border-0 bg-white rounded" srcDoc={`<!doctype html><html lang="fr"><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"><style>body{font:16px system-ui,sans-serif;color:#18181b;padding:16px;overflow-wrap:anywhere}a{color:#2563eb}</style></head><body>${html}</body></html>`} />;
}

export function MessageTemplatesSection({ onDirtyChange }: { onDirtyChange?: (dirty: boolean) => void }) {
  const template = useQuery({
    queryKey: ["admin", "reminder-template"], queryFn: reminderTemplateApi.get,
    refetchOnWindowFocus: false, staleTime: Infinity, gcTime: 0,
  });
  if (template.isPending) return <p role="status"><CircularProgress size={18} /> Chargement du modèle…</p>;
  if (template.isError) return <Alert severity="error" action={<Button onClick={() => template.refetch()}>Réessayer</Button>}>{errorMessage(template.error)}</Alert>;
  return <TemplateForm initial={template.data} onDirtyChange={onDirtyChange} />;
}

function TemplateForm({ initial, onDirtyChange }: { initial: BackendReminderTemplateView; onDirtyChange?: (dirty: boolean) => void }) {
  const [saved, setSaved] = useState(initial);
  const [subject, setSubject] = useState(initial.subject ?? "");
  const [bodyHtml, setBodyHtml] = useState(initial.bodyHtml ?? "");
  const [talkId, setTalkId] = useState("");
  const [preview, setPreview] = useState<BackendReminderTemplatePreview | null>(null);
  const [previewSource, setPreviewSource] = useState("");
  const [pending, setPending] = useState<"save" | "preview" | "reload" | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [comparison, setComparison] = useState<BackendReminderTemplateView | null>(null);
  const subjectInput = useRef<HTMLInputElement>(null);
  const talks = useQuery({ queryKey: queryKeys.talks.lists(), queryFn: talkApi.getTalks });
  const dirty = subject !== (saved.subject ?? "") || bodyHtml !== (saved.bodyHtml ?? "");
  const source = JSON.stringify({ subject, bodyHtml, talkId });
  useUnsavedTemplate(dirty);
  useEffect(() => { onDirtyChange?.(dirty); }, [dirty, onDirtyChange]);

  async function save() {
    setPending("save"); setError(""); setSuccess(false);
    try {
      const updated = await reminderTemplateApi.save({ subject, bodyHtml, revision: saved.revision ?? 0 });
      // Keep the editor's serialisation as baseline: the server may normalise HTML.
      setSaved({ ...updated, subject, bodyHtml });
      setConflict(false); setComparison(null); setSuccess(true);
    } catch (error) {
      if (error instanceof ReminderTemplateError && error.status === 409) setConflict(true);
      setError(errorMessage(error));
    } finally { setPending(null); }
  }

  async function reloadForComparison() {
    setPending("reload"); setError("");
    try {
      const latest = await reminderTemplateApi.get();
      setComparison(latest); setSaved(latest); setConflict(false);
      // Local fields are intentionally untouched so no draft text is lost.
    } catch (error) { setError(errorMessage(error)); }
    finally { setPending(null); }
  }

  async function renderPreview() {
    setPending("preview"); setError(""); setPreview(null);
    try {
      const rendered = await reminderTemplateApi.preview({ subject, bodyHtml, talkId });
      setPreview(rendered); setPreviewSource(source);
    } catch (error) { setError(errorMessage(error)); }
    finally { setPending(null); }
  }

  return <Paper variant="outlined" className="border-border! rounded-2xl! p-5! sm:p-6! bg-surface!">
    <div className="mb-6">
      <h2 className="text-xl font-bold text-text">Modèle d’email de rappel</h2>
      <p className="text-sm text-text-muted mt-1">Un modèle commun à toute l’application. Le message s’adresse à tous les speakers du talk. Aucun email n’est envoyé depuis cette page.</p>
    </div>
    <div className="flex flex-col gap-5">
      {error && <Alert severity="error">{error}</Alert>}
      {success && !dirty && <Alert severity="success">Modèle enregistré.</Alert>}
      {conflict && <Alert severity="warning" action={<Button disabled={!!pending} onClick={reloadForComparison}>Recharger pour comparer</Button>}>Un autre administrateur a modifié le modèle. Votre texte est conservé. Rechargez la version enregistrée avant de sauvegarder.</Alert>}
      {comparison && <div className="rounded-xl border border-border p-4">
        <h3 className="font-semibold">Version enregistrée par l’autre administrateur</h3>
        <p className="text-sm my-2">Comparez-la à votre texte conservé ci-dessous. L’enregistrement remplacera cette version.</p>
        <p className="font-semibold">{comparison.subject}</p>
        <PreviewBody html={comparison.bodyHtml ?? ""} />
      </div>}
      <TextField inputRef={subjectInput} label="Sujet" value={subject} required disabled={!!pending} fullWidth onChange={(event) => { setSubject(event.target.value); setSuccess(false); }} />
      <select aria-label="Insérer une variable dans le sujet" value="" disabled={!!pending} className="self-start rounded border border-border bg-surface p-2 text-sm" onChange={(event) => {
        const input = subjectInput.current;
        const start = input?.selectionStart ?? subject.length;
        const end = input?.selectionEnd ?? start;
        const variable = `{${event.target.value}}`;
        setSubject(subject.slice(0, start) + variable + subject.slice(end));
        requestAnimationFrame(() => { input?.focus(); input?.setSelectionRange(start + variable.length, start + variable.length); });
      }}>
        <option value="">Insérer une variable dans le sujet…</option>
        {templateVariables.map(([variable, label]) => <option key={variable} value={variable}>{label}</option>)}
      </select>
      <ReminderBodyEditor initialHtml={initial.bodyHtml ?? ""} disabled={!!pending} onChange={(html) => { setBodyHtml(html); setSuccess(false); }} />
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="contained" disabled={!!pending || conflict || !dirty || !subject.trim() || !bodyHtml.trim()} onClick={save}>{pending === "save" ? "Enregistrement…" : comparison ? "Enregistrer ma version après comparaison" : "Enregistrer"}</Button>
        <span role="status" className="text-sm text-text-muted">{dirty ? "Modifications non enregistrées" : "Aucune modification en attente"}</span>
      </div>
      <section className="border-t border-border pt-5 flex flex-col gap-4" aria-labelledby="preview-heading">
        <h3 id="preview-heading" className="text-lg font-semibold">Aperçu sur un talk</h3>
        {talks.isError && <Alert severity="error" action={<Button onClick={() => talks.refetch()}>Réessayer</Button>}>Impossible de charger les talks.</Alert>}
        <Autocomplete options={talks.data ?? []} getOptionLabel={(talk) => `${talk.title}${talk.date ? ` — ${talk.date}` : ""}`} getOptionKey={(talk) => talk.id} isOptionEqualToValue={(a, b) => a.id === b.id} value={talks.data?.find((talk) => talk.id === talkId) ?? null} onChange={(_, talk) => setTalkId(talk?.id ?? "")} loading={talks.isPending} disabled={!!pending} noOptionsText="Aucun talk disponible" loadingText="Chargement…" renderInput={(params) => <TextField {...params} label="Talk utilisé pour l’aperçu" />} />
        <Button className="self-start!" variant="outlined" disabled={!!pending || !talkId || !subject.trim() || !bodyHtml.trim()} onClick={renderPreview}>{pending === "preview" ? "Génération…" : "Générer l’aperçu"}</Button>
        {preview && <div className="rounded-xl border border-border p-4">
          {previewSource !== source && <Alert severity="info">Le contenu ou le talk a changé. Générez à nouveau l’aperçu.</Alert>}
          <p className="text-sm break-words"><strong>À :</strong> {preview.to?.join(", ") || "Aucun email de speaker renseigné"}</p>
          <p className="my-3"><strong>Sujet :</strong> {preview.subject}</p>
          <PreviewBody html={preview.bodyHtml ?? ""} />
        </div>}
      </section>
    </div>
  </Paper>;
}
