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
      queryClient.invalidateQueries({ queryKey: queryKeys.profile.contributions() });
    },
  });

  const updatePost = useMutation({
    mutationFn: (post: BlogPostData) => updatePostAction(post),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.profile.contributions() });
    },
  });

  const deletePost = useMutation({
    mutationFn: (id: string) => deletePostAction(id),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.profile.contributions() });
    },
  });

  return {
    createPost: createPost.mutateAsync,
    updatePost: updatePost.mutateAsync,
    deletePost: deletePost.mutateAsync,
  };
}
