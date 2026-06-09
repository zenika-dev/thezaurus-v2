import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { postApi } from "@/entities/post";
import { queryKeys } from "@/shared/api";
import { getQueryClient } from "@/shared/lib";
import { BlogPosts } from "@/features/blog-posts";
import { BlogPostsSkeleton } from "@/features/blog-posts/ui/BlogPostsSkeleton";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Blog Posts",
  description: "Suivi des articles de blog Zenika.",
};

export default function BlogPostsPage() {
  return (
    <Suspense fallback={<BlogPostsSkeleton />}>
      <BlogPostsLoader />
    </Suspense>
  );
}

async function BlogPostsLoader() {
  const queryClient = getQueryClient();
  await queryClient.prefetchQuery({
    queryKey: queryKeys.posts.lists(),
    queryFn: () => postApi.getPosts(),
  });
  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <BlogPosts />
    </HydrationBoundary>
  );
}
