import { z } from "zod";
import { BlogPostStatus } from "@/shared/api";
import { speakerSchema } from "@/entities/talk/schema";


export const blogPostFormSchema = z.object({
  title:              z.string().min(1, "Le titre est requis"),
  writers:            z.array(speakerSchema).min(1, "Au moins un auteur est requis"),
  tags:               z.array(z.string()).min(1, "Sélectionnez au moins un tag"),
  status:             z.enum(BlogPostStatus),
  link:               z.string(),
  googleDocDraftLink: z.string(),
});

export type BlogPostFormData = z.infer<typeof blogPostFormSchema>;
