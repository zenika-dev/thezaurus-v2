export type BlogPostStatus = "Idea" | "Draft" | "Review" | "Published";

export interface BlogPostData {
  id: string;
  title: string;
  author: string;
  creationDate: string;
  expectedPublicationDate?: string;
  tags: string[];
  zenikaBlogLink?: string;
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

export const blogPostStatusConfig: Record<BlogPostStatus, { text: string; bg: string }> = {
  Draft:     { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Idea:      { text: "#007fff", bg: "rgba(0, 127, 255, 0.12)" },
  Review:    { text: "#f59f0a", bg: "rgba(245, 159, 10, 0.12)" },
  Published: { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
};
