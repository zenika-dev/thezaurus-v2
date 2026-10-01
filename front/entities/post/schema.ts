import { z } from "zod";
import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";
import { BlogPostStatus, Office } from "@/shared/api";
import { contributorSchema } from "@/entities/user";
import { blogPostToday } from "./lib/today";

dayjs.extend(customParseFormat);

const optionalDate = z.string().refine(
  value => !value || dayjs(value, "DD-MM-YYYY", true).isValid(),
  "Indiquez une date valide",
);
const pastDate = (message: string) => optionalDate.refine(
  value => !value || !dayjs(value, "DD-MM-YYYY", true).isAfter(dayjs(blogPostToday(), "DD-MM-YYYY", true), "day"), message,
);
const optionalUrl = z.string().trim().refine(value => {
  if (!value) return true;
  try {
    const url = new URL(value);
    return /^https?:$/.test(url.protocol) && !!url.hostname && !/\s/.test(value);
  } catch { return false; }
}, "Indiquez une URL HTTP ou HTTPS valide");

export const blogPostFormSchema = z.object({
  title:              z.string().trim().min(1, "Le titre est requis"),
  writers:            z.array(contributorSchema).min(1, "Au moins un auteur est requis"),
  tags:               z.array(z.string()),
  office:             z.enum(Office).or(z.literal("")),
  creationDate:       pastDate("La date de création ne peut pas être future"),
  publicationDate:    optionalDate,
  actualPublicationDate: pastDate("La date de publication réelle ne peut pas être future"),
  status:             z.enum(BlogPostStatus),
  link:               optionalUrl,
  googleDocDraftLink: optionalUrl,
}).superRefine((post, ctx) => {
  if ((post.status === "REVIEW" || post.status === "READY_TO_PUBLISH") && !post.googleDocDraftLink.trim()) {
    ctx.addIssue({ code: "custom", path: ["googleDocDraftLink"], message: "Le lien du texte à relire est requis" });
  }
  if (post.status === "PUBLISHED") {
    if (!post.link.trim()) ctx.addIssue({ code: "custom", path: ["link"], message: "L’URL publique est requise" });
    if (!post.actualPublicationDate) ctx.addIssue({ code: "custom", path: ["actualPublicationDate"], message: "La date de publication réelle est requise" });
  }
});

export type BlogPostFormData = z.infer<typeof blogPostFormSchema>;
