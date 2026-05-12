import type { Route } from "./+types/blog-posts";
import BlogPosts from "~/components/BlogPosts";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Blog posts" },
    { name: "description", content: "Gestion des publications Zenika" },
  ];
}

export default function BlogPostsPage() {
  return <BlogPosts />;
}
