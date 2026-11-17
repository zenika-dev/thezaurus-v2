"use client";
import type { BlogPostData } from "@/entities/post";
import { DownloadVisualButton } from "@/shared/ui/DownloadVisualButton";

export function BlogPostVisualButton({ post, disabled = false, iconOnly = false }: { post: BlogPostData; disabled?: boolean; iconOnly?: boolean }) {
  const disabledReason = post.writers.length > 2 ? "Le visuel est disponible pour 2 auteurs maximum."
    : post.writers.length === 0 ? "Ajoutez au moins un auteur pour générer le visuel." : undefined;
  return <DownloadVisualButton endpoint={`/blog-posts/${encodeURIComponent(post.id)}/visual`}
    filename={`blog-post-${post.id}.png`} disabled={disabled} disabledReason={disabledReason} iconOnly={iconOnly}
    label={iconOnly ? `Télécharger le visuel de l’article : ${post.title}` : undefined}
    forbiddenError="Vous n’avez pas accès à la génération de ce visuel."
    missingError="L’article ou la photo d’un auteur est introuvable." />;
}
