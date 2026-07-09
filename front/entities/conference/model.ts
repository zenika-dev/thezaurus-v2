export type ConferenceCFPStatus = "Open" | "Closed" | "None";

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
    cfpLink: string;
    cfpClosingDate?: string;
    cfpStatus: ConferenceCFPStatus;
    submittedTalksAmount: number;
}

export const conferenceCFPStatusConfig: Record<ConferenceCFPStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Open: { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  Closed: { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  None: { text: "#888888", bg: "transparent", darkText: "#888888", darkBg: "transparent" },
};