import { z } from "zod";
import { TALK_VISIBILITIES } from "./model";

/**
 * Formulaire plat (deux intervenants nommés, une conférence par son nom) reconstruit vers le
 * modèle à la soumission (`withEditedSpeakers`).
 */
export const talkFormSchema = z.object({
  title:      z.string().min(1, "Le titre est requis"),
  speaker:    z.string().min(1, "L'intervenant est requis"),
  cospeaker:  z.string(),
  email:      z.union([z.string().email("Email invalide"), z.literal("")]),
  office:     z.string().min(1, "L'agence est requise"),
  description: z.string().min(1, "L'abstract est requis"),
  format:     z.string().min(1, "Le format est requis"),
  visibility: z.enum(TALK_VISIBILITIES),
  language:   z.string(),
  conference: z.string(),
  notes:      z.string(),
});

export type TalkFormData = z.infer<typeof talkFormSchema>;
