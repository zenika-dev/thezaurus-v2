export interface BlogPostData {
  id: string;
  title: string;
  author: string;
  creationDate: string;
  expectedPublicationDate?: string;
  tags: string[];
  zenikaBlogLink?: string;
  googleDocDraftLink?: string;
  status: "Draft" | "Published";
}

export const blogPostTags: Record<string, string> = {
  frontend: "Frontend",
  backend: "Backend",
  architecture: "Architecture",
  react: "React",
  web: "Web",
  mobile: "Mobile",
  devops: "DevOps",
  data: "Data",
  ai: "IA",
  security: "Sécurité",
  cloud: "Cloud",
  testing: "Testing",
  tools: "Outils",
  other: "Autre",
};