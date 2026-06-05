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

export const talkStatusConfig: Record<TalkStatus, { text: string; bg: string }> = {
  Draft:     { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Idea:      { text: "#007fff", bg: "rgba(0, 127, 255, 0.12)" },
  Submitted: { text: "#f59f0a", bg: "rgba(245, 159, 10, 0.12)" },
  Accepted:  { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
  Replayed:  { text: "#d32f2f", bg: "rgba(211, 47, 47, 0.12)" },
};
