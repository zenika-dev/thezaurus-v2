import { z } from "zod";

export const contributorSchema = z.object({
  name: z.string().trim().min(1, "Le nom est requis"),
  email: z.string().email("Email invalide").optional().or(z.literal("")),
});

export type ContributorFormData = z.infer<typeof contributorSchema>;
