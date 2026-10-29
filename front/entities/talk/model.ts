import { statusTones } from "@/shared/lib/status-tones";
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
  DRAFT:     { label: "Draft",     ...statusTones.neutral },
  PLANNED:   { label: "Idea",      ...statusTones.idea },
  SUBMITTED: { label: "Submitted", ...statusTones.info },
  ACCEPTED:  { label: "Accepted",  ...statusTones.success },
  REJECTED:  { label: "Rejected",  ...statusTones.danger },
  DONE:      { label: "Replayed",  ...statusTones.replayed },
};
