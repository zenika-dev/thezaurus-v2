"use client";

import { useState } from "react";
import { Alert, Button, IconButton, Snackbar, Tooltip } from "@mui/material";
import { Link } from "lucide-react";

export function CopyDetailLinkButton({ path, parameter, id, iconOnly = false, label = "Copier le lien" }: {
  path: string; parameter: string; id: string; iconOnly?: boolean; label?: string;
}) {
  const [feedback, setFeedback] = useState<"success" | "error" | null>(null);
  const copy = async () => {
    const url = new URL(path, window.location.origin);
    url.searchParams.set(parameter, id);
    try {
      await navigator.clipboard.writeText(url.toString());
      setFeedback("success");
    } catch { setFeedback("error"); }
  };
  return <>
    {iconOnly ? <Tooltip title="Copier le lien direct dans le presse-papiers" describeChild
      slotProps={{ tooltip: { sx: { fontSize: "0.875rem", lineHeight: 1.5, color: "#fff", bgcolor: "#1f2937" } } }}>
      <IconButton type="button" size="small" aria-label={label} onClick={copy}>
        <Link size={16} aria-hidden="true" />
      </IconButton>
    </Tooltip> : <Button type="button" size="small" startIcon={<Link size={16} />} onClick={copy}>Copier le lien</Button>}
    <Snackbar open={!!feedback} autoHideDuration={4000} onClose={() => setFeedback(null)}>
      <Alert severity={feedback ?? "success"} onClose={() => setFeedback(null)}>
        {feedback === "error" ? "Impossible de copier le lien. Réessayez ou copiez l’URL dans la barre d’adresse." : "Lien copié"}
      </Alert>
    </Snackbar>
  </>;
}
