import {
  apiFetch,
  type BackendPagedResultTalk,
  type BackendTalk,
  type BackendTalkReviewResponse,
  type PagedResult,
  type PaginationParams,
} from "@/shared/api";
import type { ApiErrorResponse, TalkData, TalkReviewRequest } from "./model";

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
    audience: t.audience ?? null,
  };
}

/**
 * Le modèle UI a exactement la forme du contrat. Le passage par `BackendTalk` n'existe que pour
 * le vérifier au compilateur — si le back renomme un champ, ça casse ici.
 */
const toPayload = (t: TalkData): BackendTalk => ({
  ...t,
  conference: t.conference ?? undefined,
  audience: t.audience ?? undefined,
});

export const talkApi = {
  getTalks: async (params?: PaginationParams): Promise<PagedResult<TalkData>> => {
    const searchParams = new URLSearchParams();
    if (params?.page !== undefined) searchParams.set("page", String(params.page));
    if (params?.pageSize !== undefined) searchParams.set("pageSize", String(params.pageSize));
    if (params?.status && params.status !== "All") searchParams.set("status", params.status);
    if (params?.personal) searchParams.set("personal", "true");
    const qs = searchParams.toString();
    const res = await apiFetch(`/talks${qs ? `?${qs}` : ""}`);
    if (!res.ok) throw new Error("Failed to fetch talks");
    const data: BackendPagedResultTalk | BackendTalk[] = await res.json();
    if (Array.isArray(data)) {
      return {
        items: data.map(mapBackendToFrontend),
        total: data.length,
        page: 0,
        pageSize: data.length,
        totalPages: 1,
      };
    }
    return {
      items: (data.items ?? []).map(mapBackendToFrontend),
      total: data.total ?? 0,
      page: data.page ?? 0,
      pageSize: data.pageSize ?? 25,
      totalPages: data.totalPages ?? 0,
    };
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
    return mapBackendToFrontend(await res.json());
  },
  deleteTalk: async (id: string): Promise<void> => {
    const res = await apiFetch(`/talks/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete talk");
  },
  reviewTalk: async (payload: TalkReviewRequest): Promise<BackendTalkReviewResponse> => {
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
