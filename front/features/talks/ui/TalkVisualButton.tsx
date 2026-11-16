"use client";

import { useState } from "react";
import { Alert, Button, Snackbar } from "@mui/material";
import { Download } from "lucide-react";
import type { TalkData } from "@/entities/talk";

export function TalkVisualButton({ talk, disabled = false }: { talk: TalkData; disabled?: boolean }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (talk.speakers.length !== 1) return null;

  const download = async () => {
    setPending(true);
    setError(null);
    try {
      const response = await fetch(`/api/backend/talks/${encodeURIComponent(talk.id)}/visual`, { method: "POST" });
      if (!response.ok) {
        throw new Error(response.status === 403
          ? "L’intervenant doit autoriser l’utilisation de sa photo par l’IA depuis son profil."
          : response.status === 404
            ? "Le talk ou la photo de l’intervenant est introuvable."
            : "Impossible de générer le visuel. Vérifiez la configuration de l’agent et réessayez.");
      }
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = `talk-${talk.id}.png`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Impossible de télécharger le visuel.");
    } finally {
      setPending(false);
    }
  };

  return <>
    <Button startIcon={<Download size={16} />} disabled={disabled || pending || !talk.conference?.name}
      onClick={download}>{pending ? "Génération du visuel…" : "Télécharger le visuel"}</Button>
    <Snackbar open={!!error} onClose={() => setError(null)}>
      <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>
    </Snackbar>
  </>;
}
