import { z } from "zod";
import { Visibility } from "@/shared/api";
import { contributorSchema, type ContributorFormData } from "@/entities/user";

export const speakerSchema = contributorSchema;
export type SpeakerFormData = ContributorFormData;

export const talkFormSchema = z.object({
  title:       z.string().trim().min(1, "Le titre est requis"),
  speakers:    z.array(speakerSchema)
    .min(1, "Au moins un intervenant est requis")
    .refine(
      (speakers) => {
        const seen = new Set<string>();
        for (const speaker of speakers) {
          const key =
            speaker.email && speaker.email.trim()
              ? speaker.email.trim().toLowerCase()
              : speaker.name.trim().toLowerCase();
          if (seen.has(key)) return false;
          seen.add(key);
        }
        return true;
      },
      { message: "Un intervenant ne peut pas être ajouté en double" }
    ),
  office:      z.string().trim().min(1, "L'agence est requise"),
  description: z.string().trim().min(1, "L'abstract est requis"),
  format:      z.string().trim().min(1, "Le format est requis"),
  visibility:  z.enum(Visibility),
  language:    z.string(),
  conference:  z.string(),
  notes:       z.string(),
});

export type TalkFormData = z.infer<typeof talkFormSchema>;
