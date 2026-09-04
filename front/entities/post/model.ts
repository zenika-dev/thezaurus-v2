import { enumValues } from "@/shared/api";
import type { BackendBlogPostStatus } from "@/shared/api";

/** Le statut garde la casse du back ; le libellé affiché est porté par `blogPostStatusConfig`. */
export type BlogPostStatus = BackendBlogPostStatus;

export const BLOG_POST_STATUSES = enumValues.BlogPostStatus;

/**
 * Forme du contrat, à deux nuances : les champs sont totaux (le contrat les déclare optionnels),
 * et les dates sont au format d'affichage `DD-MM-YYYY` plutôt qu'en ISO.
 */
export interface BlogPostData {
  id: string;
  title: string;
  writers: string[];
  creationDate: string;
  publicationDate?: string;
  tags: string[];
  link?: string;
  googleDocDraftLink?: string;
  status: BlogPostStatus;
}

/** Le formulaire n'expose qu'un auteur ; remplace le principal sans écraser les suivants. */
export function withPrimaryWriter(writers: string[], name: string): string[] {
  return [name, ...writers.slice(1)];
}

export const blogPostTags: Record<string, string> = {
  frontend:     "Frontend",
  backend:      "Backend",
  architecture: "Architecture",
  react:        "React",
  web:          "Web",
  mobile:       "Mobile",
  devops:       "DevOps",
  data:         "Data",
  ai:           "IA",
  security:     "Sécurité",
  cloud:        "Cloud",
  testing:      "Testing",
  tools:        "Outils",
  other:        "Autre",
};

/**
 * Le `Record` sur l'union du contrat vaut contrôle d'exhaustivité : ajouter une valeur à
 * `BlogPostStatus` côté Java fait échouer la compilation ici tant qu'elle n'a pas de libellé.
 */
export const blogPostStatusConfig: Record<BlogPostStatus, { label: string; text: string; bg: string; darkText: string; darkBg: string }> = {
  DRAFT:     { label: "Draft",     text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  IDEA:      { label: "Idea",      text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  REVIEW:    { label: "Review",    text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  PUBLISHED: { label: "Published", text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
};
