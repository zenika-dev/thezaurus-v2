import { ConferenceCFPStatus, ConferenceData, ConferenceDate, ConferenceReach, ConferenceType } from "./model";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const API_URL = `${API_BASE}/conferences`;

interface BackendConference {
    id: string;
    name: string;
    date: string;
    cfpLink: string;
    location: {
        city?: string;
        country?: string;
        address?: string;
        postalCode?: string;
    };
    cfpStatus: ConferenceCFPStatus;
    submittedTalksAmount: number;
    cfpClosingDate?: string;
    type: ConferenceType;
    reach: ConferenceReach;
}

export function parseConferenceDate(raw: string): ConferenceDate {
    if (!raw) return { type: "single", date: "" };
    
    if (raw.includes("/")) {
        const [start, end] = raw.split("/");
        return { type: "range", start, end };
    }
    
    if (/^\d{4}-\d{2}$/.test(raw)) {
        const [year, month] = raw.split("-");
        return { type: "month", year: parseInt(year, 10), month: parseInt(month, 10) };
    }
    
    return { type: "single", date: raw };
}

export function serializeConferenceDate(d: ConferenceDate): string {
    switch (d.type) {
        case "single":
            return d.date;
        case "range":
            return `${d.start}/${d.end}`;
        case "month":
            return `${d.year}-${String(d.month).padStart(2, "0")}`;
    }
}

export const mapBackendToFrontend = (c: BackendConference): ConferenceData => ({
    id: c.id,
    title: c.name,
    date: parseConferenceDate(c.date),
    cfpLink: c.cfpLink,
    location: c.location,
    cfpStatus: c.cfpStatus,
    submittedTalksAmount: c.submittedTalksAmount,
    cfpClosingDate: c.cfpClosingDate,
    type: c.type,
    reach: c.reach,
});

export const mapFrontendToBackend = (c: ConferenceData): BackendConference => ({
    id: c.id,
    name: c.title,
    date: serializeConferenceDate(c.date),
    cfpLink: c.cfpLink || "",
    location: c.location,
    cfpStatus: c.cfpStatus,
    submittedTalksAmount: c.submittedTalksAmount,
    cfpClosingDate: c.cfpClosingDate,
    type: c.type,
    reach: c.reach,
});

export const conferenceApi = {
  getConferences: async (): Promise<ConferenceData[]> => {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error("Failed to fetch conferences");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createConference:  async (conference: ConferenceData): Promise<ConferenceData> => {
      const res = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(mapFrontendToBackend(conference)),
      });
      if (!res.ok) throw new Error("Failed to create conference");
      return mapBackendToFrontend(await res.json());
    },
      updateConference: async (conference: ConferenceData): Promise<ConferenceData> => {
        const res = await fetch(`${API_URL}/${conference.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(mapFrontendToBackend(conference)),
        });
        if (!res.ok) throw new Error("Failed to update conference");
        return conference;
      },
    deleteConference: async (id: string): Promise<void> => {
    const res = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete conference");
  },
};
