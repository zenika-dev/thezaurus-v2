import { statusTones } from "@/shared/lib/status-tones";
import type { BlogPostStatus, BackendUser, Office } from "@/shared/api";

/**
 * Forme du contrat, à deux nuances : les champs sont totaux (le contrat les déclare optionnels),
 * et les dates sont au format d'affichage `DD-MM-YYYY` plutôt qu'en ISO.
 */
export interface BlogPostData {
  id: string;
  title: string;
  writers: BackendUser[];
  creationDate: string;
  office?: Office | "";
  publicationDate?: string;
  actualPublicationDate?: string;
  tags: string[];
  link?: string;
  googleDocDraftLink?: string;
  status: BlogPostStatus;
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
  IDEA:      { label: "Idée", ...statusTones.idea },
  DRAFT:     { label: "En rédaction", ...statusTones.neutral },
  REVIEW:    { label: "En relecture", ...statusTones.info },
  READY_TO_PUBLISH: { label: "Prêt à publier", ...statusTones.replayed },
  PUBLISHED: { label: "Publié", ...statusTones.success },
};
