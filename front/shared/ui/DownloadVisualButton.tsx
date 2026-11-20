"use client";

import { useState } from "react";
import { Alert, Button, CircularProgress, IconButton, Snackbar, Tooltip } from "@mui/material";
import { Download } from "lucide-react";
import { apiFetch } from "@/shared/api/backend";


export function DownloadVisualButton({ endpoint, filename, disabled = false, disabledReason, iconOnly = false,
  label = "Télécharger le visuel", forbiddenError, missingError }: {
  endpoint: string; filename: string; disabled?: boolean; disabledReason?: string; iconOnly?: boolean;
  label?: string; forbiddenError: string; missingError: string;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const download = async () => {
    setPending(true);
    setError(null);
    try {
      const response = await apiFetch(endpoint, { method: "POST" });
      if (response.status === 401) return;
      if (!response.ok) {
        throw new Error(response.status === 403
          ? forbiddenError
          : response.status === 404
            ? missingError
            : "Impossible de générer le visuel. Vérifiez la configuration de l’agent et réessayez.");
      }
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      // Anchor downloads expose no completion event. Keep the URL alive until
      // the document is left, including when it enters the back/forward cache.
      window.addEventListener("pagehide", () => URL.revokeObjectURL(url), { once: true });
    } catch (error) {
      setError(error instanceof Error ? error.message : "Impossible de télécharger le visuel.");
    } finally {
      setPending(false);
    }
  };

  const unavailable = disabled || !!disabledReason || pending;
  const icon = pending ? <CircularProgress size={16} color="inherit" /> : <Download size={16} />;

  return <>
    <Tooltip title={disabledReason || (pending ? "Génération du visuel…" : label)} describeChild>
      <span className="inline-flex" tabIndex={unavailable && disabledReason ? 0 : undefined}>
        {iconOnly ? <IconButton size="small" aria-label={label} disabled={unavailable} onClick={download}>
          {icon}
        </IconButton> : <Button startIcon={icon} disabled={unavailable}
          onClick={download}>{pending ? "Génération du visuel…" : "Télécharger le visuel"}</Button>}
      </span>
    </Tooltip>
    <Snackbar open={!!error} onClose={() => setError(null)}>
      <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>
    </Snackbar>
  </>;
}
