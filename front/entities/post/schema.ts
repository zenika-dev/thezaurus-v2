import { z } from "zod";

export const blogPostFormSchema = z.object({
  title:              z.string().min(1, "Le titre est requis"),
  author:             z.string().min(1, "L'auteur est requis"),
  tags:               z.array(z.string()).min(1, "Sélectionnez au moins un tag"),
  status:             z.enum(["Idea", "Draft", "Review", "Published"]),
  zenikaBlogLink:     z.string(),
  googleDocDraftLink: z.string(),
});

export type BlogPostFormData = z.infer<typeof blogPostFormSchema>;
