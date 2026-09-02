import { z } from "zod";
import { TALK_VISIBILITIES } from "./model";

/**
 * Le formulaire reste plat — deux intervenants nommés, une conférence désignée par son nom — là où
 * le modèle porte des listes et des objets. La reconstruction se fait à la soumission
 * (`withEditedSpeakers`), sans perdre ce que le formulaire n'édite pas.
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
