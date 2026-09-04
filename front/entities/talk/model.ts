import { enumValues } from "@/shared/api";
import type {
  BackendConference,
  BackendTalkReviewRequest,
  BackendTalkStatus,
  BackendUser,
  BackendVisibility,
} from "@/shared/api";

/**
 * Un speaker est un `User` côté back. `slackUserId` est renseigné par la commande Slack : le
 * formulaire ne l'expose pas mais doit le préserver.
 */
export type TalkSpeaker = BackendUser;

/**
 * Statut et visibilité gardent la casse du back : une traduction vers un nom réduit (`Draft` /
 * `Idea` / `external`) risquerait de faire collapser deux valeurs distinctes sur le même libellé.
 */
export type TalkStatus = BackendTalkStatus;

export const TALK_STATUSES = enumValues.TalkStatus;

export type TalkVisibility = BackendVisibility;

export const TALK_VISIBILITIES = enumValues.Visibility;

export interface TalkData {
  id: string;
  title: string;
  description: string;
  speakers: TalkSpeaker[];
  office: string;
  conference: BackendConference | null;
  status: TalkStatus;
  visibility: TalkVisibility;
  format: string;
  /** Date de présentation au format ISO `YYYY-MM-DD`, telle que stockée par le back. */
  date?: string;
  language: string;
  notes: string;
  slides?: string;
  replay?: string;
}

/**
 * Le formulaire n'expose que deux noms et un email, alors que le back porte une liste complète.
 * Réécrit les deux premiers speakers en préservant leurs champs non éditables (`slackUserId`,
 * email du co-speaker) ainsi que les suivants, ajoutés via la commande Slack.
 */
export function withEditedSpeakers(
  current: TalkSpeaker[],
  speaker: string,
  cospeaker: string,
  email: string,
): TalkSpeaker[] {
  const speakers: TalkSpeaker[] = [];
  if (speaker.trim()) {
    speakers.push({ ...current[0], name: speaker.trim(), email: email.trim() || undefined });
  }
  if (cospeaker.trim()) {
    speakers.push({ ...current[1], name: cospeaker.trim() });
  }
  speakers.push(...current.slice(2));
  return speakers;
}

export type TalkReviewRequest = Required<BackendTalkReviewRequest>;

/**
 * Pas dérivable du contrat : `TalkResource.review()` retourne un `Response` brut, donc SmallRye
 * n'en génère aucun schéma — même limitation que `UserProfile` (cf. entities/user/model.ts).
 */
export interface TalkReviewResponse {
  suggestedTitles: string[];
  suggestedAbstracts: string[];
  feedback: string[];
  keyImprovements: string[];
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export const agencyLabels: Record<string, string> = {
  paris:     "Paris",
  nantes:    "Nantes",
  rennes:    "Rennes",
  bordeaux:  "Bordeaux",
  lyon:      "Lyon",
  lille:     "Lille",
  grenoble:  "Grenoble",
  singapour: "Singapour",
  montreal:  "Montréal",
};

export const visibilityLabels: Record<TalkVisibility, string> = {
  PRIVATE: "Interne",
  PUBLIC:  "Externe",
};

export const formatLabels: Record<string, string> = {
  video:    "Vidéo",
  training: "Formation",
  public:   "Public",
  other:    "Autre",
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
