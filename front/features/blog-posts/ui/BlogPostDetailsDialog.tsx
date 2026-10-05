"use client";

import { useState } from "react";
import type { BlogPostData } from "@/entities/post";
import { BlogPostFormDialog } from "./BlogPostFormDialog";

interface BlogPostDetailsDialogProps {
  post: BlogPostData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (post: BlogPostData) => void | Promise<unknown>;
  onDelete: (id: string) => void | Promise<unknown>;
}

export function BlogPostDetailsDialog({ post, open, onClose, onUpdate, onDelete }: BlogPostDetailsDialogProps) {
  const [cachedPost, setCachedPost] = useState<BlogPostData | null>(post);
  if (post && post !== cachedPost) {
    setCachedPost(post);
  }
  const currentPost = post ?? cachedPost;
  if (!currentPost || !open) return null;
  return <BlogPostFormDialog key={currentPost.id} post={currentPost} onClose={onClose} onSave={onUpdate} onDelete={onDelete} />;
}

