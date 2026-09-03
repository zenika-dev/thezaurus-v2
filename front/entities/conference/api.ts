import { apiFetch, type BackendConference } from "@/shared/api";
import {
  CONFERENCE_CFP_STATUSES,
  type ConferenceCFPStatus,
  type ConferenceData,
  type ConferencePeriod,
} from "./model";

/**
 * `cfpStatus` est un `String` libre côté back (pas une vraie enum Java) : le contrat ne peut pas
 * en garantir les valeurs, on les valide donc à l'exécution.
 */
function toFrontendCfpStatus(s: string | undefined): ConferenceCFPStatus {
  return CONFERENCE_CFP_STATUSES.includes(s as ConferenceCFPStatus)
    ? (s as ConferenceCFPStatus)
    : "None";
}

/**
 * Repli pour une conférence dont le back n'a pas de période exploitable (document antérieur à la
 * migration et non reconnu). Rend le modèle UI total sans inventer de date.
 */
const UNKNOWN_PERIOD: ConferencePeriod = { start: "", end: "", precision: "DAY" };

/**
 * Ne fait que totaliser le payload : le contrat déclare tous les champs optionnels, faute
 * d'annotations `@Schema(required = true)` sur les modèles Java.
 */
export const mapBackendToFrontend = (c: BackendConference): ConferenceData => ({
  id: c.id ?? "",
  name: c.name ?? "",
  date:
    c.date?.start && c.date.end && c.date.precision
      ? { start: c.date.start, end: c.date.end, precision: c.date.precision }
      : UNKNOWN_PERIOD,
  cfpLink: c.cfpLink,
  location: c.location ?? {},
  cfpStatus: toFrontendCfpStatus(c.cfpStatus),
  submittedTalksAmount: c.submittedTalksAmount ?? 0,
  cfpClosingDate: c.cfpClosingDate,
  type: c.type ?? "Hors scope",
  reach: c.reach ?? "Locale",
});

/**
 * Le modèle UI a exactement la forme du contrat. Le passage par `BackendConference` n'existe que
 * pour le vérifier au compilateur — si le back renomme un champ, ça casse ici.
 */
const toPayload = (c: ConferenceData): BackendConference => c;

export const conferenceApi = {
  getConferences: async (): Promise<ConferenceData[]> => {
    const res = await apiFetch("/conferences");
    if (!res.ok) throw new Error("Failed to fetch conferences");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createConference: async (conference: ConferenceData): Promise<ConferenceData> => {
    const res = await apiFetch("/conferences", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toPayload(conference)),
    });
    if (!res.ok) throw new Error("Failed to create conference");
    return mapBackendToFrontend(await res.json());
  },
  updateConference: async (conference: ConferenceData): Promise<ConferenceData> => {
    const res = await apiFetch(`/conferences/${conference.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(toPayload(conference)),
    });
    if (!res.ok) throw new Error("Failed to update conference");
    return conference;
  },
  deleteConference: async (id: string): Promise<void> => {
    const res = await apiFetch(`/conferences/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete conference");
  },
};
