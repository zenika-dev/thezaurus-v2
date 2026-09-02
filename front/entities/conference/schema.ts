import { z } from "zod";
import { CONFERENCE_CFP_STATUSES, CONFERENCE_REACHES, CONFERENCE_TYPES } from "./model";

export const conferenceFormSchema = z.object({
  name:               z.string().min(1, "Le titre est requis"),
  location:           z.string().min(1, "La localisation est requise"),
  cfpLink:            z.string().optional(),
  cfpClosingDate:     z.string().optional(),
  cfpStatus:          z.enum(CONFERENCE_CFP_STATUSES),
  submittedTalksAmount: z.number(),
  type: z.enum(CONFERENCE_TYPES),
  reach: z.enum(CONFERENCE_REACHES),
});

export type ConferenceFormData = z.infer<typeof conferenceFormSchema>;
