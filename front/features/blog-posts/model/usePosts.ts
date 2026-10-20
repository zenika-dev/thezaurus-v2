import { useSuspenseQuery } from "@tanstack/react-query";
import { postApi } from "@/entities/post";
import { queryKeys, type PaginationParams } from "@/shared/api";
import { DEFAULT_PAGE_SIZE } from "@/shared/lib/pagination-params";
import { usePostsMutations } from "./usePostsMutations";

export function usePosts(params?: PaginationParams) {
  const { data } = useSuspenseQuery({
    queryKey: queryKeys.posts.lists(params),
    queryFn: () => postApi.getPosts(params),
  });

  const mutations = usePostsMutations();

  return {
    posts: data?.items ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? 0,
    pageSize: data?.pageSize ?? DEFAULT_PAGE_SIZE,
    totalPages: data?.totalPages ?? 0,
    pagedResult: data,
    ...mutations,
  };
}
