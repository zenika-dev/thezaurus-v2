import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { postApi } from "@/entities/post";
import { BlogPostStatus, queryKeys } from "@/shared/api";
import { getQueryClient, parsePaginationSearchParams } from "@/shared/lib";
import { BlogPosts } from "@/features/blog-posts";
import { BlogPostsSkeleton } from "@/features/blog-posts/ui/BlogPostsSkeleton";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Blog Posts",
  description: "Suivi des articles de blog Zenika.",
};

interface BlogPostsPageProps {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

export default async function BlogPostsPage({ searchParams }: BlogPostsPageProps) {
  const resolvedParams = await searchParams;
  return (
    <Suspense fallback={<BlogPostsSkeleton />}>
      <BlogPostsLoader rawParams={resolvedParams} />
    </Suspense>
  );
}

async function BlogPostsLoader({
  rawParams,
}: {
  rawParams: { [key: string]: string | string[] | undefined };
}) {
  const queryClient = getQueryClient();
  const apiParams = parsePaginationSearchParams(rawParams, BlogPostStatus);

  await queryClient.prefetchQuery({
    queryKey: queryKeys.posts.lists(apiParams),
    queryFn: () => postApi.getPosts(apiParams),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <BlogPosts />
    </HydrationBoundary>
  );
}
