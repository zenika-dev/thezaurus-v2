export type ConferenceCFPStatus = "Open" | "Closed";

export interface ConferenceData {
    id: string;
    title: string;
    location: string;
    date: string;
    cfpLink: string;
    cfpStatus: ConferenceCFPStatus;
    submittedTalksAmount: number;
}

export const conferenceCFPStatusConfig: Record<ConferenceCFPStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Open: { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  Closed: { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
};