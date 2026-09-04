import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";
import { apiFetch, type BackendBlogPost } from "@/shared/api";
import type { BlogPostData } from "./model";

dayjs.extend(customParseFormat);

/** Le back stocke des dates ISO (`YYYY-MM-DDT00:00:00`), l'interface affiche du `DD-MM-YYYY`. */
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

/**
 * Le back accepte `null` pour les dates absentes, ce que le schéma généré ne décrit pas : les
 * champs Java n'ont pas d'annotation `@Schema` et ressortent simplement optionnels.
 */
type BackendBlogPostPayload = Omit<BackendBlogPost, "creationDate" | "publicationDate"> & {
  creationDate: string | null;
  publicationDate: string | null;
};

/**
 * Ne fait que totaliser le payload et convertir les dates : le contrat déclare tous les champs
 * optionnels, faute d'annotations `@Schema(required = true)` sur les modèles Java.
 */
export function mapBackendToFrontend(p: BackendBlogPost): BlogPostData {
  return {
    id: p.id ?? "",
    title: p.title ?? "",
    writers: p.writers ?? [],
    tags: p.tags ?? [],
    creationDate: toFrontendDate(p.creationDate),
    publicationDate: toFrontendDate(p.publicationDate),
    status: p.status ?? "IDEA",
    link: p.link ?? "",
    googleDocDraftLink: p.googleDocDraftLink ?? "",
  };
}

export function mapFrontendToBackend(p: BlogPostData): BackendBlogPostPayload {
  return {
    ...p,
    creationDate: toLocalDateTime(p.creationDate),
    publicationDate: toLocalDateTime(p.publicationDate),
  };
}

export const postApi = {
  getPosts: async (): Promise<BlogPostData[]> => {
    const res = await apiFetch("/blog-posts");
    if (!res.ok) throw new Error("Failed to fetch posts");
    const data = await res.json();
    return data.map(mapBackendToFrontend);
  },
  createPost: async (post: BlogPostData): Promise<BlogPostData> => {
    const res = await apiFetch("/blog-posts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!res.ok) throw new Error("Failed to create post");
    return mapBackendToFrontend(await res.json());
  },
  updatePost: async (post: BlogPostData): Promise<BlogPostData> => {
    const res = await apiFetch(`/blog-posts/${post.id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!res.ok) throw new Error("Failed to update post");
    return post;
  },
  deletePost: async (id: string): Promise<void> => {
    const res = await apiFetch(`/blog-posts/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Failed to delete post");
  },
};
