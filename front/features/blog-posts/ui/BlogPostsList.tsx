"use client";

import { useState } from "react";
import { useSession } from "next-auth/react";
import { BlogPostStatus } from "@/shared/api";
import { ContributionFilters } from "@/shared/ui/ContributionFilters";
import { isContributor } from "@/entities/user/lib/isContributor";
import { SpeakerChip } from "@/entities/user";
import { blogPostStatusConfig } from "@/entities/post";
import type { BlogPostData } from "@/entities/post";
import dynamic from "next/dynamic";
import { usePosts } from "@/features/blog-posts/model";
import { StatusTag } from "./BlogPostTags";

const BlogPostDetailsDialog = dynamic(
  () => import("./BlogPostDetailsDialog").then((m) => ({ default: m.BlogPostDetailsDialog })),
  { ssr: false }
);

export function BlogPostsList() {
  const { data: session } = useSession();
  const [statusFilter, setStatusFilter] = useState<BlogPostStatus | "All">("All");
  const [personalOnly, setPersonalOnly] = useState(false);
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const { posts, updatePost, deletePost } = usePosts();

  const selectedPost = posts.find((p) => p.id === selectedPostId) ?? null;
  const filteredPosts = posts.filter((post) =>
    (statusFilter === "All" || post.status === statusFilter) &&
    (!personalOnly || isContributor(post.writers, session?.user?.email)),
  );

  const handleUpdate = async (updated: BlogPostData) => {
    try { await updatePost(updated); }
    catch { alert("Erreur lors de la mise à jour du post"); }
  };

  const handleDelete = async (id: string) => {
    try { await deletePost(id); }
    catch { alert("Erreur lors de la suppression du post"); }
  };

  return (
    <>
      <ContributionFilters
        statuses={BlogPostStatus} labels={blogPostStatusConfig}
        status={statusFilter} onStatusChange={setStatusFilter}
        personalLabel="Mes articles" personalOnly={personalOnly} onPersonalChange={setPersonalOnly}
      />
      <div className="flex flex-col gap-2">
        {filteredPosts.map((post) => (
          <div
            key={post.id}
            onClick={() => setSelectedPostId(post.id)}
            className="p-4 flex justify-between items-center rounded-2xl border border-primary
              cursor-pointer transition-colors duration-200 hover:bg-surface-muted"
          >
            <div className="flex flex-col gap-0.5">
              <span className="font-bold text-text">{post.title}</span>
              <div className="flex gap-1 items-center flex-wrap">
                {post.writers.map((writer, index) => (
                  <SpeakerChip key={index} name={writer.name} email={writer.email} />
                ))}
                <span className="text-sm text-text-muted">{post.creationDate}</span>
              </div>
              <div className="flex gap-1 mt-1 flex-wrap">
                {post.tags.slice(0, 5).map((tag) => (
                  <span key={tag} className="px-1.5 py-0.5 rounded-2xl text-[0.7rem] bg-[rgba(117,117,117,0.12)] text-[#475569] dark:bg-[rgba(255,255,255,0.1)] dark:text-[#94a3b8] font-medium">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
            <StatusTag status={post.status} />
          </div>
        ))}

        {filteredPosts.length === 0 && (
          <div className="p-8 text-center text-text-muted border border-border rounded-2xl">
            {personalOnly || statusFilter !== "All"
              ? "Aucun article ne correspond aux filtres sélectionnés."
              : "Aucun article de blog pour le moment."}
          </div>
        )}
      </div>

      <BlogPostDetailsDialog
        post={selectedPost}
        open={!!selectedPostId}
        onClose={() => setSelectedPostId(null)}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
      />
    </>
  );
}
