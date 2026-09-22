"use client";

import { useCallback, useEffect, useState } from "react";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { Alert, Autocomplete, Button, CircularProgress, MenuItem, Paper, TextField } from "@mui/material";

import { type BackendMessageTemplateDefinition, type BackendReminderTemplateView, type BackendReminderTemplatePreview } from "@/shared/api";
import { reminderTemplateApi, ReminderTemplateError } from "../api/reminder-template";
import { useUnsavedTemplate, unsavedTemplateMessage } from "../model/useUnsavedTemplate";
import { ReminderBodyEditor } from "./ReminderBodyEditor";

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Une erreur est survenue. Veuillez réessayer.";
}

function PreviewBody({ html }: { html: string }) {
  return <div className="h-72 min-h-72 resize-y overflow-hidden"><iframe title="Corps de l’email" sandbox="" referrerPolicy="no-referrer" className="w-full h-full border-0 bg-white rounded" srcDoc={`<!doctype html><html lang="fr"><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"><style>body{font:16px system-ui,sans-serif;color:#18181b;padding:16px;overflow-wrap:anywhere}a{color:#2563eb}</style></head><body>${html}</body></html>`} /></div>;
}

export function MessageTemplatesSection({ onDirtyChange }: { onDirtyChange?: (dirty: boolean) => void }) {
  const catalog = useQuery({
    queryKey: ["admin", "message-templates"], queryFn: reminderTemplateApi.list,
    refetchOnWindowFocus: false, staleTime: Infinity,
  });
  const [selectedId, setSelectedId] = useState<string>();
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const selected = catalog.data?.find((model) => model.id === selectedId) ?? catalog.data?.[0];
  const reportDirty = useCallback((value: boolean) => {
    setDirty(value);
    onDirtyChange?.(value);
  }, [onDirtyChange]);
  if (catalog.isPending) return <p role="status">Chargement des modèles…</p>;
  if (catalog.isError) return <Alert severity="error" action={<Button onClick={() => catalog.refetch()}>Réessayer</Button>}>{errorMessage(catalog.error)}</Alert>;
  if (!selected) return <Alert severity="info">Aucun modèle disponible.</Alert>;
  return <Paper variant="outlined" className="border-border! rounded-2xl! p-5! sm:p-6! bg-surface!">
    <div className="mb-6">
      <TextField select label="Modèle" value={selected.id} disabled={busy} fullWidth onChange={(event) => {
        if (event.target.value === selected.id) return;
        if (dirty && !window.confirm(unsavedTemplateMessage)) return;
        reportDirty(false);
        setSelectedId(event.target.value);
      }}>
        {catalog.data.map((model) => <MenuItem key={model.id} value={model.id}>{model.label}</MenuItem>)}
      </TextField>
      <p className="text-sm text-text-muted mt-1">{selected.description}</p>
    </div>
    <SelectedTemplate key={selected.id} definition={selected} onDirtyChange={reportDirty} onBusyChange={setBusy} />
  </Paper>;
}

type TemplateProps = {
  definition: BackendMessageTemplateDefinition;
  onDirtyChange: (dirty: boolean) => void;
  onBusyChange: (busy: boolean) => void;
};

function SelectedTemplate({ definition, ...callbacks }: TemplateProps) {
  const template = useQuery({
    queryKey: ["admin", "message-template", definition.id, definition.apiPath],
    queryFn: () => reminderTemplateApi.get(definition.apiPath!),
    refetchOnWindowFocus: false, staleTime: Infinity, gcTime: 0,
  });
  if (template.isPending) return <p role="status"><CircularProgress size={18} /> Chargement du modèle…</p>;
  if (template.isError) return <Alert severity="error" action={<Button onClick={() => template.refetch()}>Réessayer</Button>}>{errorMessage(template.error)}</Alert>;
  return <TemplateForm initial={template.data} definition={definition} {...callbacks} />;
}

function TemplateForm({ initial, definition, onDirtyChange, onBusyChange }: TemplateProps & { initial: BackendReminderTemplateView }) {
  const [saved, setSaved] = useState(initial);
  const [subject, setSubject] = useState(initial.subject ?? "");
  const [bodyHtml, setBodyHtml] = useState(initial.bodyHtml ?? "");
  const [contextId, setContextId] = useState("");
  const [preview, setPreview] = useState<BackendReminderTemplatePreview | null>(null);
  const [previewSource, setPreviewSource] = useState("");
  const [pending, setPending] = useState<"save" | "preview" | "reload" | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [comparison, setComparison] = useState<BackendReminderTemplateView | null>(null);
  const context = definition.previewContext;
  const contexts = useInfiniteQuery({
    queryKey: ["admin", "template-contexts", definition.id, context?.optionsPath],
    queryFn: ({ pageParam }) => reminderTemplateApi.contexts(context!.optionsPath!, pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor || undefined,
    enabled: !!context?.optionsPath,
  });
  const contextOptions = contexts.data?.pages.flatMap((page) => page.options ?? []).filter((option) => !!option.id) ?? [];
  const dirty = subject !== (saved.subject ?? "") || bodyHtml !== (saved.bodyHtml ?? "");
  const source = JSON.stringify({ subject, bodyHtml, contextId });
  useUnsavedTemplate(dirty);
  useEffect(() => { onDirtyChange?.(dirty); }, [dirty, onDirtyChange]);

  useEffect(() => { onBusyChange(!!pending); }, [pending, onBusyChange]);

  async function save() {
    setPending("save"); setError(""); setSuccess(false);
    try {
      const updated = await reminderTemplateApi.save(definition.apiPath!, { subject, bodyHtml, revision: saved.revision ?? 0 });
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
      const latest = await reminderTemplateApi.get(definition.apiPath!);
      setComparison(latest); setSaved(latest); setConflict(false);
      // Local fields are intentionally untouched so no draft text is lost.
    } catch (error) { setError(errorMessage(error)); }
    finally { setPending(null); }
  }

  async function renderPreview() {
    setPending("preview"); setError(""); setPreview(null);
    try {
      const rendered = await reminderTemplateApi.preview(definition.apiPath!, { subject, bodyHtml, contextId });
      setPreview(rendered); setPreviewSource(source);
    } catch (error) { setError(errorMessage(error)); }
    finally { setPending(null); }
  }

  return <div className="flex flex-col gap-5">
      {error && <Alert severity="error">{error}</Alert>}
      {success && !dirty && <Alert severity="success">Modèle enregistré.</Alert>}
      {conflict && <Alert severity="warning" action={<Button disabled={!!pending} onClick={reloadForComparison}>Recharger pour comparer</Button>}>Un autre administrateur a modifié le modèle. Votre texte est conservé. Rechargez la version enregistrée avant de sauvegarder.</Alert>}
      {comparison && <div className="rounded-xl border border-border p-4">
        <h3 className="font-semibold">Version enregistrée par l’autre administrateur</h3>
        <p className="text-sm my-2">Comparez-la à votre texte conservé ci-dessous. L’enregistrement remplacera cette version.</p>
        <p className="font-semibold">{comparison.subject}</p>
        <PreviewBody html={comparison.bodyHtml ?? ""} />
      </div>}
      <TextField label="Sujet" value={subject} required disabled={!!pending} fullWidth onChange={(event) => { setSubject(event.target.value); setSuccess(false); }} />
      <ReminderBodyEditor definition={definition} initialHtml={initial.bodyHtml ?? ""} disabled={!!pending} onChange={(html) => { setBodyHtml(html); setSuccess(false); }} />
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="contained" disabled={!!pending || conflict || !dirty || !subject.trim() || !bodyHtml.trim()} onClick={save}>{pending === "save" ? "Enregistrement…" : comparison ? "Enregistrer ma version après comparaison" : "Enregistrer"}</Button>
        <span role="status" className="text-sm text-text-muted">{dirty ? "Modifications non enregistrées" : "Aucune modification en attente"}</span>
      </div>
      <section className="border-t border-border pt-5 flex flex-col gap-4" aria-labelledby="preview-heading">
        <h3 id="preview-heading" className="text-lg font-semibold">Aperçu</h3>
        {contexts.isError && <Alert severity="error" action={<Button onClick={() => contexts.refetch()}>Réessayer</Button>}>Impossible de charger les éléments pour l’aperçu.</Alert>}
        {context && <Autocomplete options={contextOptions} getOptionLabel={(option) => option.label ?? ""} getOptionKey={(option) => option.id ?? option.label ?? ""} isOptionEqualToValue={(a, b) => a.id === b.id} value={contextOptions.find((option) => option.id === contextId) ?? null} onChange={(_, option) => setContextId(option?.id ?? "")} loading={contexts.isPending} disabled={!!pending} noOptionsText="Aucun élément dans les pages chargées" loadingText="Chargement…" renderInput={(params) => <TextField {...params} label={context.label} />} />}
        {context && contexts.hasNextPage && <Button className="self-start!" disabled={!!pending || contexts.isFetchingNextPage} onClick={() => contexts.fetchNextPage()}>{contexts.isFetchingNextPage ? "Chargement…" : "Charger plus d’éléments"}</Button>}
        <Button className="self-start!" variant="outlined" disabled={!!pending || (!!context && !contextId) || !subject.trim() || !bodyHtml.trim()} onClick={renderPreview}>{pending === "preview" ? "Génération…" : "Générer l’aperçu"}</Button>
        {preview && <div className="rounded-xl border border-border p-4">
          {previewSource !== source && <Alert severity="info">Le contenu ou le contexte a changé. Générez à nouveau l’aperçu.</Alert>}
          <p className="text-sm break-words"><strong>À :</strong> {preview.to?.join(", ") || "Aucun destinataire renseigné"}</p>
          <p className="my-3"><strong>Sujet :</strong> {preview.subject}</p>
          <PreviewBody html={preview.bodyHtml ?? ""} />
        </div>}
      </section>
    </div>;
}
