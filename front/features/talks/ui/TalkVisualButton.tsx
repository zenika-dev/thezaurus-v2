"use client";
import type { TalkData } from "@/entities/talk";
import { DownloadVisualButton } from "@/shared/ui/DownloadVisualButton";

export function TalkVisualButton({ talk, disabled = false, iconOnly = false }: { talk: TalkData; disabled?: boolean; iconOnly?: boolean }) {
  const disabledReason = talk.speakers.length > 3 ? "Le visuel est disponible pour 3 intervenants maximum."
    : talk.speakers.length === 0 ? "Ajoutez au moins un intervenant pour générer le visuel."
    : !talk.conference?.name ? "Renseignez la conférence pour générer le visuel." : undefined;
  return <DownloadVisualButton endpoint={`/api/backend/talks/${encodeURIComponent(talk.id)}/visual`}
    filename={`talk-${talk.id}.png`} disabled={disabled} disabledReason={disabledReason} iconOnly={iconOnly}
    label={iconOnly ? `Télécharger le visuel du talk : ${talk.title}` : undefined}
    forbiddenError="Vous n’avez pas accès à la génération de ce visuel."
    missingError="Le talk ou la photo d’un intervenant est introuvable." />;
}
