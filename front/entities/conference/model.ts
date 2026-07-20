export type ConferenceCFPStatus = "Open" | "Closed" | "None";

export type ConferenceType =
  | "Marketing / business"
  | "Technique stratégique"
  | "Technique généraliste"
  | "Technique"
  | "Hors scope";

export type ConferenceReach = "Locale" | "Régionale" | "Nationale";

export type ConferenceDate =
    | { type: "single"; date: string }
    | { type: "range"; start: string; end: string }
    | { type: "month"; year: number; month: number };

export interface ConferenceLocation {
    city?: string;
    country?: string;
    address?: string;
    postalCode?: string;
}

export interface ConferenceData {
    id: string;
    title: string;
    location: ConferenceLocation;
    date: ConferenceDate;
    cfpLink?: string;
    cfpClosingDate?: string;
    cfpStatus: ConferenceCFPStatus;
    submittedTalksAmount: number;
    type: ConferenceType;
    reach: ConferenceReach;
}

export const conferenceCFPStatusConfig: Record<ConferenceCFPStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Open: { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  Closed: { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  None: { text: "#888888", bg: "transparent", darkText: "#888888", darkBg: "transparent" },
};

export const conferenceTypeConfig: Record<ConferenceType, { text: string; bg: string; darkText: string; darkBg: string }> = {
  "Marketing / business": { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  "Technique stratégique": { text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  "Technique généraliste": { text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  "Technique": { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  "Hors scope": { text: "#681AC1", bg: "#F3E8FF", darkText: "#FFB9FF", darkBg: "#59168B" },
};