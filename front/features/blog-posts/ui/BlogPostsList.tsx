"use client";

import { useState } from "react";
import { BlogPostStatus } from "@/shared/api";
import { ContributionFilters, PaginationBar } from "@/shared/ui";
import { SpeakerChip } from "@/entities/user";
import { blogPostStatusConfig } from "@/entities/post";
import dynamic from "next/dynamic";
import { usePosts } from "@/features/blog-posts/model";
import { usePaginationUrlParams } from "@/shared/lib";
import { StatusTag } from "./BlogPostTags";
import { BlogPostWorkflowHelp } from "./BlogPostWorkflowHelp";

const BlogPostDetailsDialog = dynamic(
  () => import("./BlogPostDetailsDialog").then((m) => ({ default: m.BlogPostDetailsDialog })),
  { ssr: false }
);

export function BlogPostsList() {
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const {
    page,
    pageSize,
    status,
    personal,
    setStatus,
    setPersonal,
    setPage,
    setPageSize,
    resetFilters,
    apiParams,
  } = usePaginationUrlParams(BlogPostStatus);

  const { posts, total, totalPages, updatePost, deletePost } = usePosts(apiParams);

  const selectedPost = posts.find((p) => p.id === selectedPostId) ?? null;

  return (
    <div className="flex flex-col flex-1">
      <div className="flex items-start gap-2 flex-wrap mb-4 shrink-0">
        <ContributionFilters
          statuses={BlogPostStatus}
          labels={blogPostStatusConfig}
          status={status}
          onStatusChange={setStatus}
          personalLabel="Mes articles"
          personalOnly={personal}
          onPersonalChange={setPersonal}
          onReset={resetFilters}
        />
        <BlogPostWorkflowHelp />
      </div>

      <div className="grid gap-3 [grid-template-columns:repeat(auto-fill,minmax(min(100%,460px),1fr))] mb-4">
        {posts.map((post) => (
          <div
            key={post.id}
            onClick={() => setSelectedPostId(post.id)}
            className="p-4 flex flex-col justify-between rounded-2xl border border-primary
              cursor-pointer transition-colors duration-200 hover:bg-surface-muted min-h-[110px]"
          >
            <div className="flex justify-between items-start gap-2">
              <span className="font-bold text-text">{post.title}</span>
              <div className="shrink-0">
                <StatusTag status={post.status} />
              </div>
            </div>

            <div className="flex flex-col gap-1.5 mt-3">
              <div className="flex gap-1 items-center flex-wrap">
                {post.writers.map((writer, index) => (
                  <SpeakerChip key={index} name={writer.name} email={writer.email} />
                ))}
                <span className="text-sm text-text-muted">{post.creationDate}</span>
              </div>
              <div className="flex gap-1 mt-0.5 flex-wrap">
                {post.tags.slice(0, 5).map((tag) => (
                  <span
                    key={tag}
                    className="px-1.5 py-0.5 rounded-2xl text-[0.7rem] bg-[rgba(117,117,117,0.12)] text-[#475569] dark:bg-[rgba(255,255,255,0.1)] dark:text-[#94a3b8] font-medium"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ))}

        {posts.length === 0 && (
          <div className="p-8 text-center text-text-muted border border-border rounded-2xl col-span-full">
            {personal || status !== "All"
              ? "Aucun article ne correspond aux filtres sélectionnés."
              : "Aucun article de blog pour le moment."}
          </div>
        )}
      </div>

      <PaginationBar
        page={page}
        pageSize={pageSize}
        total={total}
        totalPages={totalPages}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        label="Articles par page :"
      />

      <BlogPostDetailsDialog
        post={selectedPost}
        open={!!selectedPostId}
        onClose={() => setSelectedPostId(null)}
        onUpdate={updatePost}
        onDelete={deletePost}
      />
    </div>
  );
}
