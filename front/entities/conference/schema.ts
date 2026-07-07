import { z } from "zod";

export const conferenceFormSchema = z.object({
  title:              z.string().min(1, "Le titre est requis"),
  location:           z.string().min(1, "La localisation est requise"),
  cfpLink:            z.string(), // required?
  cfpClosingDate:     z.string().optional(),
  cfpStatus:          z.enum(["Open", "Closed", "None"]),
  submittedTalksAmount: z.number(),
});

export type ConferenceFormData = z.infer<typeof conferenceFormSchema>;
