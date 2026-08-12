import type { TalkData } from "./model";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const API_URL = `${API_BASE}/talks`;

export interface BackendSpeaker {
  name?: string;
  email?: string;
  /** Renseigné par la commande Slack. Le front ne l'affiche pas mais doit le préserver. */
  slackUserId?: string;
}

interface BackendTalk {
  id?: string;
  title?: string;
  speakers?: BackendSpeaker[];
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
  speakers: BackendSpeaker[];
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
    speaker: speakers[0]?.name || "",
    cospeaker: speakers[1]?.name || "",
    email: speakers[0]?.email || "",
    speakersSource: speakers,
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
  // Le formulaire ne connaît que deux noms et un email : on repart des speakers reçus du backend
  // pour ne pas perdre les champs qu'il ne sait pas éditer (slackUserId, email du co-speaker).
  // Au-delà des deux premiers, les speakers (ex. ajoutés via la commande Slack) ne sont pas
  // éditables par ce formulaire mais doivent être réémis tels quels pour ne pas les perdre.
  const source = t.speakersSource ?? [];
  const speakers: BackendSpeaker[] = [];
  if (t.speaker.trim()) {
    speakers.push({ ...source[0], name: t.speaker.trim(), email: t.email.trim() || undefined });
  }
  if (t.cospeaker.trim()) {
    speakers.push({ ...source[1], name: t.cospeaker.trim() });
  }
  speakers.push(...source.slice(2));

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
};
