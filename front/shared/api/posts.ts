import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";
import type { BlogPostData, BlogPostStatus } from "@/entities/post";

dayjs.extend(customParseFormat);

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const API_URL = `${API_BASE}/blog-posts`;

function toFrontendStatus(s: string): BlogPostStatus {
  switch (s) {
    case "DRAFT":     return "Draft";
    case "PUBLISHED": return "Published";
    case "IDEA":      return "Idea";
    case "REVIEW":    return "Review";
    default:          return "Idea";
  }
}

function toBackendStatus(s: BlogPostStatus): string {
  switch (s) {
    case "Draft":     return "DRAFT";
    case "Published": return "PUBLISHED";
    case "Idea":      return "IDEA";
    case "Review":    return "REVIEW";
    default:          return "IDEA";
  }
}

function toFrontendDate(dateStr: string | undefined | null): string {
  if (!dateStr) return "";
  if (/^\d{2}-\d{2}-\d{4}$/.test(dateStr)) return dateStr;
  const parsed = dayjs(dateStr);
  return parsed.isValid() ? parsed.format("DD-MM-YYYY") : dateStr;
}

function toLocalDateTime(dateStr: string | undefined): string | null {
  if (!dateStr) return null;
  const ddmmyyyy = dateStr.match(/^(\d{2})-(\d{2})-(\d{4})$/);
  if (ddmmyyyy) return `${ddmmyyyy[3]}-${ddmmyyyy[2]}-${ddmmyyyy[1]}T00:00:00`;
  const yyyymmdd = dateStr.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (yyyymmdd) return `${dateStr}T00:00:00`;
  return dateStr;
}

interface BackendPost {
  id?: string;
  title?: string;
  writers?: string[];
  tags?: string[];
  creationDate?: string | null;
  publicationDate?: string | null;
  status?: string;
  link?: string;
}

interface BackendPostPayload {
  id: string;
  title: string;
  writers: string[];
  status: string;
  tags: string[];
  creationDate: string | null;
  publicationDate: string | null;
  link: string;
}

export function mapBackendToFrontend(p: BackendPost): BlogPostData {
  return {
    id: p.id || "",
    title: p.title || "",
    author: p.writers?.[0] || "",
    tags: p.tags || [],
    creationDate: toFrontendDate(p.creationDate),
    expectedPublicationDate: toFrontendDate(p.publicationDate),
    status: toFrontendStatus(p.status ?? ""),
    zenikaBlogLink: p.link || "",
    googleDocDraftLink: "",
  };
}

export function mapFrontendToBackend(p: BlogPostData): BackendPostPayload {
  return {
    id: p.id,
    title: p.title,
    writers: [p.author],
    status: toBackendStatus(p.status),
    tags: p.tags,
    creationDate: toLocalDateTime(p.creationDate),
    publicationDate: toLocalDateTime(p.expectedPublicationDate) || null,
    link: p.zenikaBlogLink || "",
  };
}

export const postApi = {
  getPosts: async (): Promise<BlogPostData[]> => {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error("Failed to fetch posts");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createPost: async (post: BlogPostData): Promise<BlogPostData> => {
    const res = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!res.ok) throw new Error("Failed to create post");
    return mapBackendToFrontend(await res.json());
  },
  updatePost: async (post: BlogPostData): Promise<BlogPostData> => {
    const res = await fetch(`${API_URL}/${post.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!res.ok) throw new Error("Failed to update post");
    return post;
  },
  deletePost: async (id: string): Promise<void> => {
    const res = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete post");
  },
};
