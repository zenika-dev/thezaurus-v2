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

  const createPost = useMutation({
    mutationFn: (post: BlogPostData) => createPostAction(post),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
    },
  });

  const updatePost = useMutation({
    mutationFn: (post: BlogPostData) => updatePostAction(post),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
    },
  });

  const deletePost = useMutation({
    mutationFn: (id: string) => deletePostAction(id),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
    },
  });

  return {
    createPost: createPost.mutateAsync,
    updatePost: updatePost.mutateAsync,
    deletePost: deletePost.mutateAsync,
  };
}
