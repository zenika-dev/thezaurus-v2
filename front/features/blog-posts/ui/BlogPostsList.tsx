"use client";

import { useState } from "react";
import type { BlogPostData } from "@/entities/post";
import dynamic from "next/dynamic";
import { usePosts } from "@/features/blog-posts/model";
import { StatusTag } from "./BlogPostTags";

const BlogPostDetailsDialog = dynamic(
  () => import("./BlogPostDetailsDialog").then((m) => ({ default: m.BlogPostDetailsDialog })),
  { ssr: false }
);

export function BlogPostsList() {
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const { posts, updatePost, deletePost } = usePosts();

  const selectedPost = posts.find((p) => p.id === selectedPostId) ?? null;

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
      <div className="flex flex-col gap-2">
        {posts.map((post) => (
          <div
            key={post.id}
            onClick={() => setSelectedPostId(post.id)}
            className="p-4 flex justify-between items-center rounded border border-primary
              cursor-pointer transition-colors duration-200 hover:bg-surface-muted"
          >
            <div className="flex flex-col gap-0.5">
              <span className="font-bold text-text">{post.title}</span>
              <span className="text-sm text-text-muted">
                {post.author} · {post.creationDate}
              </span>
              <div className="flex gap-1 mt-1 flex-wrap">
                {post.tags.slice(0, 5).map((tag) => (
                  <span key={tag} className="px-1.5 py-0.5 rounded text-[0.7rem] bg-surface-muted text-[#475569] font-medium">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
            <StatusTag status={post.status} />
          </div>
        ))}

        {posts.length === 0 && (
          <div className="p-8 text-center text-text-muted border border-border rounded">
            Aucun article de blog pour le moment. Créez-en un avec &quot;Nouveau billet&quot; !
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
