"use client";

import { Suspense, useState } from "react";
import Button from "@mui/material/Button";
import { PenLine } from "lucide-react";
import type { BlogPostData } from "@/entities/post";
import dynamic from "next/dynamic";
import { usePostsMutations } from "@/features/blog-posts/model/usePostsMutations";
import { DataErrorBoundary } from "@/shared/ui";
import { BlogPostsList } from "./BlogPostsList";
import { BlogPostsListSkeleton } from "./BlogPostsSkeleton";

const CreateBlogPostDialog = dynamic(
  () => import("./CreateBlogPostDialog").then((m) => ({ default: m.CreateBlogPostDialog })),
  { ssr: false }
);

export function BlogPosts() {
  const [open, setOpen] = useState(false);
  const { createPost } = usePostsMutations();

  const handleSubmit = async (post: BlogPostData) => {
    try { await createPost(post); }
    catch { alert("Erreur lors de la création du post"); }
  };

  return (
    <div className="p-8">
      {/* Header — always rendered, independent of data */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-text">Blog Posts</h1>
          <p className="text-sm text-text-muted mt-0.5">
            Gérez les articles de blog Zenika.
          </p>
        </div>
        <Button variant="contained" onClick={() => setOpen(true)} className="gap-2!">
          <PenLine size={16} />
          Nouveau billet
        </Button>
      </div>

      {/* Data section — can load/fail independently */}
      <Suspense fallback={<BlogPostsListSkeleton />}>
        <DataErrorBoundary>
          <BlogPostsList />
        </DataErrorBoundary>
      </Suspense>

      <CreateBlogPostDialog open={open} onClose={() => setOpen(false)} onSubmit={handleSubmit} />
    </div>
  );
}

export default BlogPosts;
