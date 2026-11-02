import { statusTones } from "@/shared/lib/status-tones";
import type {
  BackendLocation,
  ConferenceReach,
  ConferenceType,
  DatePrecision,
} from "@/shared/api";

/**
 * `cfpStatus` est un `String` libre côté back : le contrat ne peut pas le contraindre. Cette union
 * reste donc une convention purement frontend, validée à l'exécution par `toFrontendCfpStatus`.
 */
export const CONFERENCE_CFP_STATUSES = ["Open", "Closed", "None"] as const;

export type ConferenceCFPStatus = (typeof CONFERENCE_CFP_STATUSES)[number];

/**
 * Version totale du type généré (tous les champs y sont optionnels). Pas de champ discriminant :
 * `precision === "MONTH"` pour un mois, sinon `start === end` pour une date unique.
 */
export interface ConferencePeriod {
  start: string;
  end: string;
  precision: DatePrecision;
}

export interface ConferenceData {
  id: string;
  name: string;
  location: BackendLocation;
  date: ConferencePeriod;
  cfpLink?: string;
  cfpOpeningDate?: string;
  cfpClosingDate?: string;
  cfpStatus: ConferenceCFPStatus;
  submittedTalksAmount: number;
  type: ConferenceType;
  reach: ConferenceReach;
}

export const conferenceCFPStatusConfig: Record<ConferenceCFPStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Open: { ...statusTones.success },
  Closed: { ...statusTones.neutral },
  None: { ...statusTones.neutral },
};

/**
 * Le `Record` sur l'union du contrat vaut contrôle d'exhaustivité : ajouter une valeur à
 * `ConferenceType` côté Java fait échouer la compilation ici tant qu'elle n'a pas de couleur.
 */
export const conferenceTypeConfig: Record<ConferenceType, { text: string; bg: string; darkText: string; darkBg: string }> = {
  "Marketing / business": { ...statusTones.neutral },
  "Technique stratégique": { ...statusTones.idea },
  "Technique généraliste": { ...statusTones.info },
  "Technique": { ...statusTones.success },
  "Hors scope": { ...statusTones.replayed },
};
