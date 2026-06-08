import { useQueryClient, useMutation } from "@tanstack/react-query";
import type { BlogPostData } from "@/entities/post";
import { queryKeys } from "@/shared/api";
import {
  createPostAction,
  updatePostAction,
  deletePostAction,
} from "@/entities/post/actions";

export function usePostsMutations() {
  const queryClient = useQueryClient();
  const queryKey = queryKeys.posts.lists();

  const createPost = useMutation({
    mutationFn: (post: BlogPostData) => createPostAction(post),
    onMutate: async (newPost) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<BlogPostData[]>(queryKey);
      queryClient.setQueryData<BlogPostData[]>(queryKey, (old = []) => [...old, newPost]);
      return { previous };
    },
    onError: (_err, _post, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const updatePost = useMutation({
    mutationFn: (post: BlogPostData) => updatePostAction(post),
    onMutate: async (updated) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<BlogPostData[]>(queryKey);
      queryClient.setQueryData<BlogPostData[]>(queryKey, (old = []) =>
        old.map((p) => (p.id === updated.id ? updated : p))
      );
      return { previous };
    },
    onError: (_err, _post, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const deletePost = useMutation({
    mutationFn: (id: string) => deletePostAction(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData<BlogPostData[]>(queryKey);
      queryClient.setQueryData<BlogPostData[]>(queryKey, (old = []) =>
        old.filter((p) => p.id !== id)
      );
      return { previous };
    },
    onError: (_err, _id, ctx) => {
      queryClient.setQueryData(queryKey, ctx?.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return {
    createPost: createPost.mutateAsync,
    updatePost: updatePost.mutateAsync,
    deletePost: deletePost.mutateAsync,
  };
}
