"use client";

import { useSession } from "next-auth/react";
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import type { BlogPostData } from "@/entities/post";
import { BlogPostFormDialog } from "./BlogPostFormDialog";

interface CreateBlogPostDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (post: BlogPostData) => void | Promise<unknown>;
}

export function CreateBlogPostDialog({ open, onClose, onSubmit }: CreateBlogPostDialogProps) {
  const { data: session } = useSession();
  const { data: profile } = useQuery({ queryKey: queryKeys.profile.me(), queryFn: profileApi.getProfile, enabled: open });
  if (!open) return null;

  const authorName = profile?.name || session?.user?.name;
  const authorEmail = profile?.email || session?.user?.email || "";
  const defaultAuthor = authorName ? { name: authorName, email: authorEmail } : undefined;
  const defaultOffice = profile?.office ?? "";

  return (
    <BlogPostFormDialog
      onClose={onClose}
      onSave={onSubmit}
      defaultOffice={defaultOffice}
      defaultAuthor={defaultAuthor}
    />
  );
}
