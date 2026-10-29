"use client";

import { Suspense, useState } from "react";
import { PenLine } from "lucide-react";
import dynamic from "next/dynamic";
import { usePostsMutations } from "@/features/blog-posts/model/usePostsMutations";
import { DataErrorBoundary, PageHeader, CreateButton } from "@/shared/ui";
import { BlogPostsList } from "./BlogPostsList";
import { BlogPostsListSkeleton } from "./BlogPostsSkeleton";

const CreateBlogPostDialog = dynamic(
  () => import("./CreateBlogPostDialog").then((m) => ({ default: m.CreateBlogPostDialog })),
  { ssr: false }
);

export function BlogPosts() {
  const [open, setOpen] = useState(false);
  const { createPost } = usePostsMutations();

  return (
    <div className="p-4 sm:p-6 lg:p-8 pb-0! flex flex-col min-h-full">
      <PageHeader title="Blog Posts" subtitle="Gérez les articles de blog Zenika." className="shrink-0" action={
        <CreateButton onClick={() => setOpen(true)}>
          <PenLine size={16} />
          Nouveau billet
        </CreateButton>
      } />

      <Suspense fallback={<BlogPostsListSkeleton />}>
        <DataErrorBoundary>
          <BlogPostsList />
        </DataErrorBoundary>
      </Suspense>

      <CreateBlogPostDialog open={open} onClose={() => setOpen(false)} onSubmit={createPost} />
    </div>
  );
}

export default BlogPosts;
