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

export const blogPostStatusConfig: Record<BlogPostStatus, { text: string; bg: string; darkText: string; darkBg: string }> = {
  Draft:     { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E" },
  Idea:      { text: "#9A0530", bg: "#FFEDD4", darkText: "#FFDD58", darkBg: "#7E2A0C" },
  Review:    { text: "#0132D1", bg: "#DBEAFE", darkText: "#94E5FF", darkBg: "#1C398E" },
  Published: { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B" },
};
