import { apiFetch, type BackendTalk } from "@/shared/api";
import type { ApiErrorResponse, TalkData, TalkReviewRequest, TalkReviewResponse } from "./model";

/**
 * Ne fait que totaliser le payload : le contrat déclare tous les champs optionnels, faute
 * d'annotations `@Schema(required = true)` sur les modèles Java.
 */
export function mapBackendToFrontend(t: BackendTalk): TalkData {
  return {
    id: t.id ?? "",
    title: t.title ?? "",
    description: t.description ?? "",
    speakers: t.speakers ?? [],
    office: t.office ?? "",
    conference: t.conference ?? null,
    status: t.status ?? "PLANNED",
    visibility: t.visibility ?? "PRIVATE",
    format: t.format ?? "",
    date: t.date ?? "",
    language: t.language ?? "",
    notes: t.notes ?? "",
    slides: t.slides ?? "",
    replay: t.replay ?? "",
  };
}

/**
 * Le modèle UI a exactement la forme du contrat. Le passage par `BackendTalk` n'existe que pour
 * le vérifier au compilateur — si le back renomme un champ, ça casse ici.
 */
const toPayload = (t: TalkData): BackendTalk => ({ ...t, conference: t.conference ?? undefined });

export const talkApi = {
  getTalks: async (): Promise<TalkData[]> => {
    const res = await apiFetch("/talks");
    if (!res.ok) throw new Error("Failed to fetch talks");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createTalk: async (talk: TalkData): Promise<TalkData> => {
    const res = await apiFetch("/talks", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toPayload(talk)),
    });
    if (!res.ok) throw new Error("Failed to create talk");
    return mapBackendToFrontend(await res.json());
  },
  updateTalk: async (talk: TalkData): Promise<TalkData> => {
    const res = await apiFetch(`/talks/${talk.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toPayload(talk)),
    });
    if (!res.ok) throw new Error("Failed to update talk");
    return talk;
  },
  deleteTalk: async (id: string): Promise<void> => {
    const res = await apiFetch(`/talks/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete talk");
  },
  reviewTalk: async (payload: TalkReviewRequest): Promise<TalkReviewResponse> => {
    const response = await apiFetch("/talks/review", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      let errorMessage = "Une erreur est survenue lors de l'analyse du talk.";
      const errorData: ApiErrorResponse = await response.json();
      if (errorData.message) {
        errorMessage = errorData.message;
      }
      throw new Error(errorMessage);
    }
    return await response.json();
  },
};
