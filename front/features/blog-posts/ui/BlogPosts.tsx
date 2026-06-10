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
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">Blog Posts</h1>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
            Gérez les articles de blog Zenika.
          </p>
        </div>
        <Button
          variant="contained"
          onClick={() => setOpen(true)}
          className="gap-2! font-bold! py-2! shadow-none! rounded-[20px]! bg-[linear-gradient(135deg,#ed213c_0%,#BF1D67_100%)]! transition-transform! duration-200! hover:shadow-none! hover:-translate-y-[2px]!"
        >
          <PenLine size={16} />
          Nouveau billet
        </Button>
      </div>

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
