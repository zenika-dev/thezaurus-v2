import { TalkFormat, TalkStatus, Visibility } from "@/shared/api";
export { agencyLabels } from "@/shared/lib/offices";
import type {
  BackendConference,
  BackendTalkReviewRequest,
  BackendTalkReviewResponse,
  BackendUser,
} from "@/shared/api";

export interface TalkData {
  id: string;
  title: string;
  description: string;
  speakers: BackendUser[];
  office: string;
  conference: BackendConference | null;
  status: TalkStatus;
  visibility: Visibility;
  format: string;
  /** Date de présentation au format ISO `YYYY-MM-DD`, telle que stockée par le back. */
  date?: string;
  language: string;
  notes: string;
  slides?: string;
  replay?: string;
  audience?: number | null;
}

export type TalkReviewRequest = Required<BackendTalkReviewRequest>;

/** Message affiché quand le back ne fournit pas de message fonctionnel (réseau, réponse illisible…). */
export const TALK_REVIEW_FALLBACK_ERROR =
  "L'assistant IA rencontre une difficulté temporaire. Merci de réessayer dans quelques instants.";

/**
 * Résultat de la relecture IA : une server action ne peut pas lever d'erreur avec un message
 * lisible côté client (Next.js le masque en production), on le retourne donc explicitement.
 */
export type TalkReviewResult =
  | { ok: true; data: BackendTalkReviewResponse }
  | { ok: false; message: string };

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// Les valeurs viennent du contrat généré ; seule la présentation du libellé est locale.
export const visibilityLabels: Record<Visibility, string> = {
  PRIVATE: "Interne",
  PUBLIC:  "Externe",
};

export const formatLabels: Record<string, string> = Object.fromEntries(
  TalkFormat.map(format => [format, format === "conference" ? "Conférence" : format.charAt(0).toUpperCase() + format.slice(1)]),
);

export const legacyFormatLabels: Record<string, string> = {
  video: "Vidéo", training: "Formation", public: "Public", other: "Autre",
};

export const languageLabels: Record<string, string> = {
  francais: "Français",
  english:  "English",
};

/**
 * Le `Record` sur l'union du contrat vaut contrôle d'exhaustivité : ajouter une valeur à
 * `TalkStatus` côté Java fait échouer la compilation ici tant qu'elle n'a pas de libellé.
 */
export const talkStatusConfig: Record<TalkStatus, { label: string; text: string; bg: string; darkText: string; darkBg: string }> = {
  DRAFT:     { label: "Draft",     text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  PLANNED:   { label: "Idea",      text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  SUBMITTED: { label: "Submitted", text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  ACCEPTED:  { label: "Accepted",  text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  REJECTED:  { label: "Rejected",  text: "#9A0530", bg: "#FFE4E6", darkText: "#FFB3BA", darkBg: "#7E1A2C" },
  DONE:      { label: "Replayed",  text: "#681AC1", bg: "#F3E8FF", darkText: "#FFB9FF", darkBg: "#59168B" },
};
