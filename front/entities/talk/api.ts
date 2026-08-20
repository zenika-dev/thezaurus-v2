import type { TalkData, TalkReviewRequest, TalkReviewResponse } from "./model";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const API_URL = `${API_BASE}/talks`;

interface BackendTalk {
  id?: string;
  title?: string;
  speakers?: string[];
  office?: string;
  description?: string;
  conference?: { name?: string } | null;
  status?: string;
  visibility?: string;
}

interface BackendTalkPayload {
  id: string;
  title: string;
  description: string;
  speakers: string[];
  office: string;
  conference: { name: string } | null;
  status: string;
  visibility: string;
}

export function mapBackendToFrontend(t: BackendTalk): TalkData {
  const speakers = t.speakers || [];
  return {
    id: t.id || "",
    title: t.title || "",
    speaker: speakers[0] || "",
    cospeaker: speakers[1] || "",
    email: "",
    agency: t.office || "",
    abstract: t.description || "",
    format: "public",
    visibility: t.visibility === "PUBLIC" ? "external" : "internal",
    language: "francais",
    conference: t.conference?.name || "",
    date: "",
    notes: "",
    status:
      t.status === "DONE"
        ? "Replayed"
        : t.status === "ACCEPTED"
          ? "Accepted"
          : t.status === "SUBMITTED"
            ? "Submitted"
            : t.status === "DRAFT"
              ? "Draft"
              : "Idea",
    slides: "",
    replay: "",
  };
}

export function mapFrontendToBackend(t: TalkData): BackendTalkPayload {
  const speakers: string[] = [];
  if (t.speaker.trim()) speakers.push(t.speaker.trim());
  if (t.cospeaker.trim()) speakers.push(t.cospeaker.trim());

  let backendStatus = "PLANNED";
  if (t.status === "Draft") backendStatus = "DRAFT";
  else if (t.status === "Submitted") backendStatus = "SUBMITTED";
  else if (t.status === "Accepted") backendStatus = "ACCEPTED";
  else if (t.status === "Replayed") backendStatus = "DONE";

  return {
    id: t.id,
    title: t.title,
    description: t.abstract,
    speakers,
    office: t.agency,
    conference: t.conference ? { name: t.conference } : null,
    status: backendStatus,
    visibility: t.visibility === "external" ? "PUBLIC" : "PRIVATE",
  };
}

export const talkApi = {
  getTalks: async (): Promise<TalkData[]> => {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error("Failed to fetch talks");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createTalk: async (talk: TalkData): Promise<TalkData> => {
    const res = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(talk)),
    });
    if (!res.ok) throw new Error("Failed to create talk");
    return mapBackendToFrontend(await res.json());
  },
  updateTalk: async (talk: TalkData): Promise<TalkData> => {
    const res = await fetch(`${API_URL}/${talk.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(talk)),
    });
    if (!res.ok) throw new Error("Failed to update talk");
    return talk;
  },
  deleteTalk: async (id: string): Promise<void> => {
    const res = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete talk");
  },
  reviewTalk: async (payload: TalkReviewRequest): Promise<TalkReviewResponse> => {
    const res = await fetch(`${API_URL}/review`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error("Failed to review talk with AI agent");
    return res.json();
  },
};
