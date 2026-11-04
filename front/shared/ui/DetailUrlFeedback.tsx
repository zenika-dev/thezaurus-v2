"use client";

import { Alert, Button } from "@mui/material";
import { FormDialog } from "./FormDialog";

export function DetailUrlFeedback({ loading, error, onClose, onRetry }: {
  loading: boolean; error: boolean; onClose: () => void; onRetry: () => void;
}) {
  if (!loading && !error) return null;
  return <FormDialog open onClose={onClose} title="Détails" cancelLabel="Fermer">
    {loading ? <p role="status">Chargement…</p> : <Alert severity="error"
      action={<Button onClick={onRetry}>Réessayer</Button>}>
      Cet élément est introuvable, inaccessible ou n’a pas pu être chargé.
    </Alert>}
  </FormDialog>;
}
