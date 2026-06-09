import { useSuspenseQuery } from "@tanstack/react-query";
import { postApi } from "@/entities/post";
import { queryKeys } from "@/shared/api";
import { usePostsMutations } from "./usePostsMutations";

export function usePosts() {
  const { data: posts } = useSuspenseQuery({
    queryKey: queryKeys.posts.lists(),
    queryFn: () => postApi.getPosts(),
  });

  const mutations = usePostsMutations();

  return { posts: posts ?? [], ...mutations };
}
