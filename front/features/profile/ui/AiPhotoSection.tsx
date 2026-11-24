"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import FormControlLabel from "@mui/material/FormControlLabel";
import Switch from "@mui/material/Switch";
import Alert from "@mui/material/Alert";
import type { UserProfile } from "@/entities/user";
import { apiFetch, queryKeys } from "@/shared/api";
import { SectionCard } from "@/shared/ui";

export function AiPhotoSection({ profile }: { profile: UserProfile }) {
  const client = useQueryClient();
  const mutation = useMutation({
    mutationFn: async (allowed: boolean) => {
      const response = await apiFetch("/api/me/profile/ai-photo", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ allowed }),
      });
      if (!response.ok) throw new Error();
      return allowed;
    },
    onSuccess: (allowed) => {
      client.setQueryData<UserProfile>(queryKeys.profile.me(), (old) =>
        old ? { ...old, aiPhotoAllowed: allowed } : old,
      );
    },
  });
  return (
    <SectionCard
      titleId="ai-photo-title"
      title="Utilisation de ma photo par l’IA"
      description="Autorise l’envoi de ta photo de profil à l’IA pour ajuster son cadrage dans les visuels de talks et d’articles de blog."
    >
      <FormControlLabel
        label="Autoriser l’utilisation de ma photo par l’IA"
        control={
          <Switch
            checked={profile.aiPhotoAllowed}
            disabled={mutation.isPending}
            onChange={(event) => mutation.mutate(event.target.checked)}
          />
        }
      />
      <p className="text-sm text-text-muted">
        Ce choix reste valable si toi ou un admin changez ta photo. Le retrait bloque les
        prochains envois à l’IA ; les visuels déjà créés sont conservés.
        Sans ton accord, ta photo reste utilisable avec un cadrage simple, centré
        horizontalement et aligné en haut, sans envoi à l’IA.
        Le logo Zenika est utilisé si aucune photo n’est disponible.
      </p>
      {mutation.isError && (
        <Alert severity="error">L’autorisation n’a pas pu être enregistrée. Réessaie.</Alert>
      )}
    </SectionCard>
  );
}
