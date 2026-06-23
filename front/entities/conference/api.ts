import { ConferenceCFPStatus, ConferenceData } from "./model";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const API_URL = `${API_BASE}/conferences`;

interface BackendConference {
    id: string;
    name: string;
    date: string;
    cfpLink: string;
    location: string;
    cfpStatus: string;
    submittedTalksAmount: number;
}

export const mapBackendToFrontend = (c: BackendConference): ConferenceData => ({
    id: c.id,
    title: c.name,
    date: c.date,
    cfpLink: c.cfpLink,
    location: c.location,
    cfpStatus: c.cfpStatus as ConferenceCFPStatus,
    submittedTalksAmount: c.submittedTalksAmount,
});

export const conferenceApi = {
  getConferences: async (): Promise<ConferenceData[]> => {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error("Failed to fetch conferences");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
};
