export type TalkStatus = "Draft" | "Idea" | "Submitted" | "Accepted" | "Replayed";

export interface TalkData {
  id: string;
  title: string;
  speaker: string;
  cospeaker: string;
  email: string;
  agency: string;
  abstract: string;
  format: string;
  visibility: string;
  language: string;
  conference: string;
  date?: string;
  notes: string;
  status: TalkStatus;
  slides?: string;
  replay?: string;
}

export interface TalkReviewRequest {
  title: string;
  abstract: string;
}

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

export const visibilityLabels: Record<string, string> = {
  internal: "Interne",
  external: "Externe",
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

export const talkStatusConfig: Record<TalkStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Draft:     { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  Idea:      { text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  Submitted: { text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  Accepted:  { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  Replayed:  { text: "#681AC1", bg: "#F3E8FF", darkText: "#FFB9FF", darkBg: "#59168B" },
};
