import { z } from "zod";
import { BLOG_POST_STATUSES } from "./model";

/**
 * Le formulaire n'expose qu'un auteur, mais le modèle en porte une liste : `writers` est édité via
 * son premier élément, les suivants étant préservés à l'enregistrement (`withPrimaryWriter`).
 */
export const blogPostFormSchema = z.object({
  title:              z.string().min(1, "Le titre est requis"),
  author:             z.string().min(1, "L'auteur est requis"),
  tags:               z.array(z.string()).min(1, "Sélectionnez au moins un tag"),
  status:             z.enum(BLOG_POST_STATUSES),
  link:               z.string(),
  googleDocDraftLink: z.string(),
});

export type BlogPostFormData = z.infer<typeof blogPostFormSchema>;
