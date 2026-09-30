"use client";

import { useRef } from "react";
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
  const lastPostRef = useRef<BlogPostData | null>(post);
  if (post) lastPostRef.current = post;
  const currentPost = post ?? lastPostRef.current;
  if (!currentPost || !open) return null;
  return <BlogPostFormDialog key={currentPost.id} post={currentPost} onClose={onClose} onSave={onUpdate} onDelete={onDelete} />;
}
