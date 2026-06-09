import { z } from "zod";

export const talkFormSchema = z.object({
  title:      z.string().min(1, "Le titre est requis"),
  speaker:    z.string().min(1, "L'intervenant est requis"),
  cospeaker:  z.string(),
  email:      z.union([z.string().email("Email invalide"), z.literal("")]),
  agency:     z.string().min(1, "L'agence est requise"),
  abstract:   z.string().min(1, "L'abstract est requis"),
  format:     z.string().min(1, "Le format est requis"),
  visibility: z.string().min(1, "La visibilité est requise"),
  language:   z.string(),
  conference: z.string(),
  notes:      z.string(),
});

export type TalkFormData = z.infer<typeof talkFormSchema>;
