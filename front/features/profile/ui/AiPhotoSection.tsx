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
      description="Autorise les agents IA à utiliser ta photo de profil pour créer des visuels de talks et d’articles de blog."
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
        prochaines récupérations ; les visuels déjà créés sont conservés.
      </p>
      {mutation.isError && (
        <Alert severity="error">L’autorisation n’a pas pu être enregistrée. Réessaie.</Alert>
      )}
    </SectionCard>
  );
}
