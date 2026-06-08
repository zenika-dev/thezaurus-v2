"use server";

import { postApi } from "./api";
import { revalidatePath } from "next/cache";
import type { BlogPostData } from "./model";

export async function createPostAction(post: BlogPostData): Promise<BlogPostData> {
  const created = await postApi.createPost(post);
  revalidatePath("/blog-posts");
  return created;
}

export async function updatePostAction(post: BlogPostData): Promise<void> {
  await postApi.updatePost(post);
  revalidatePath("/blog-posts");
}

export async function deletePostAction(id: string): Promise<void> {
  await postApi.deletePost(id);
  revalidatePath("/blog-posts");
}
