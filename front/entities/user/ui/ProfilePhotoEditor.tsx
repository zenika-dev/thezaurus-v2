"use client";

import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import { apiFetch, queryKeys } from "@/shared/api";

export function ProfilePhotoEditor({ email, admin = false }: { email: string; admin?: boolean }) {
  const client = useQueryClient();
  const input = useRef<HTMLInputElement>(null);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<{ error: boolean; text: string } | null>(null);
  const endpoint = admin
    ? `/api/admin/users/${encodeURIComponent(email)}/photo`
    : "/api/me/profile/photo";

  async function save(file?: File) {
    setMessage(null);
    if (file && (!file.size || file.size > 5 * 1024 * 1024 ||
        !["image/jpeg", "image/png", "image/webp"].includes(file.type))) {
      setMessage({ error: true, text: "Choisis une photo JPEG, PNG ou WebP de 5 Mo maximum." });
      return;
    }
    setPending(true);
    try {
      const response = await apiFetch(endpoint, {
        method: file ? "PUT" : "DELETE",
        headers: file ? { "Content-Type": file.type } : undefined,
        body: file,
      });
      if (!response.ok) {
        const text = response.status === 413
          ? "La photo dépasse 5 Mo. Choisis un fichier plus petit."
          : response.status === 400 || response.status === 415
            ? "Photo invalide. Choisis une image JPEG, PNG ou WebP de 20 mégapixels maximum."
            : response.status === 403
              ? "Tu n’as pas les droits pour modifier cette photo."
              : "La photo n’a pas pu être enregistrée. Réessaie plus tard.";
        setMessage({ error: true, text });
        return;
      }
      await client.invalidateQueries({ queryKey: queryKeys.profile.me() });
      // Les avatars chargés indépendamment du profil se rafraîchissent aussi.
      window.dispatchEvent(new Event("profile-photo-changed"));
      setMessage({ error: false, text: file ? "Photo enregistrée." : "Photo Google rétablie." });
    } catch {
      setMessage({ error: true, text: "La photo n’a pas pu être enregistrée. Réessaie." });
    } finally { setPending(false); }
  }

  return <div className="flex flex-col gap-2">
    <div className="flex flex-wrap gap-2">
      <Button onClick={() => input.current?.click()} disabled={pending} variant="outlined">
        {admin ? "Changer la photo" : "Charger une photo"}
      </Button>
        <input ref={input} type="file" hidden accept="image/jpeg,image/png,image/webp"
          aria-label={`Charger la photo de ${email}`} disabled={pending}
          onChange={(event) => {
            const file = event.currentTarget.files?.[0];
            event.currentTarget.value = "";
            if (file) void save(file);
          }} />
      <Button disabled={pending} onClick={() => void save()}>
        {admin ? "Rétablir la photo Google" : "Utiliser ma photo Google"}
      </Button>
    </div>
    <p className="m-0 text-xs text-text-muted">JPEG, PNG ou WebP · 5 Mo et 20 mégapixels maximum · sans recadrage obligatoire.</p>
    {message && <Alert severity={message.error ? "error" : "success"}>{message.text}</Alert>}
  </div>;
}
