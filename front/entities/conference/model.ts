import { enumValues } from "@/shared/api";
import type {
  BackendConferenceReach,
  BackendConferenceType,
  BackendDatePrecision,
  BackendLocation,
} from "@/shared/api";

/**
 * Types *et* valeurs viennent du contrat OpenAPI : `ConferenceType` et `ConferenceReach` sont de
 * vraies enums Java, que le contrat décrit exactement. Aucune valeur n'est recopiée ici — ajouter
 * une valeur côté Java et régénérer suffit à la faire apparaître dans les formulaires.
 */
export type ConferenceType = BackendConferenceType;

export const CONFERENCE_TYPES = enumValues.ConferenceType;

export type ConferenceReach = BackendConferenceReach;

export const CONFERENCE_REACHES = enumValues.ConferenceReach;

/**
 * `cfpStatus` est un `String` libre côté back : le contrat ne peut pas le contraindre. Cette union
 * reste donc une convention purement frontend, validée à l'exécution par `toFrontendCfpStatus`.
 */
export const CONFERENCE_CFP_STATUSES = ["Open", "Closed", "None"] as const;

export type ConferenceCFPStatus = (typeof CONFERENCE_CFP_STATUSES)[number];

export type ConferenceLocation = BackendLocation;

export type DatePrecision = BackendDatePrecision;

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
  location: ConferenceLocation;
  date: ConferencePeriod;
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

/**
 * Le `Record` sur l'union du contrat vaut contrôle d'exhaustivité : ajouter une valeur à
 * `ConferenceType` côté Java fait échouer la compilation ici tant qu'elle n'a pas de couleur.
 */
export const conferenceTypeConfig: Record<ConferenceType, { text: string; bg: string; darkText: string; darkBg: string }> = {
  "Marketing / business": { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  "Technique stratégique": { text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  "Technique généraliste": { text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  "Technique": { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
  "Hors scope": { text: "#681AC1", bg: "#F3E8FF", darkText: "#FFB9FF", darkBg: "#59168B" },
};
