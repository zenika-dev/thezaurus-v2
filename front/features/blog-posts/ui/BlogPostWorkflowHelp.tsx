"use client";

import { useId, useState } from "react";
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Tooltip } from "@mui/material";
import { Info } from "lucide-react";
import { BlogPostStatus } from "@/shared/api";
import { StatusTag } from "./BlogPostTags";

const descriptions: Record<BlogPostStatus, string> = {
  IDEA: "Le sujet est identifié, l’écriture n’a pas encore commencé.",
  DRAFT: "Le contenu est en cours de construction ; il reste du travail de rédaction.",
  REVIEW: "Une version complète est proposée à des relecteurs ; des corrections peuvent suivre.",
  READY_TO_PUBLISH: "Le travail éditorial est terminé. L’article attend sa publication.",
  PUBLISHED: "L’article est effectivement accessible à son adresse de publication.",
};

export function BlogPostWorkflowHelp() {
  const [open, setOpen] = useState(false);
  const titleId = useId();
  return <>
    <Tooltip title="Comprendre les étapes">
      <IconButton aria-label="Comprendre les étapes des articles" aria-haspopup="dialog" onClick={() => setOpen(true)}>
        <Info size={20} aria-hidden="true" />
      </IconButton>
    </Tooltip>
    <Dialog open={open} onClose={() => setOpen(false)} aria-labelledby={titleId} maxWidth="sm" fullWidth>
      <DialogTitle id={titleId}>Les étapes d’un article</DialogTitle>
      <DialogContent>
        <ol className="flex flex-col gap-5 list-none p-0 m-0">
          {BlogPostStatus.map(status => <li key={status}>
            <StatusTag status={status} />
            <p className="text-sm text-text-muted mt-2 mb-0">{descriptions[status]}</p>
          </li>)}
        </ol>
      </DialogContent>
      <DialogActions><Button onClick={() => setOpen(false)}>Fermer</Button></DialogActions>
    </Dialog>
  </>;
}
