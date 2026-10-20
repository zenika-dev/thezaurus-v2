import { useSuspenseQuery } from "@tanstack/react-query";
import { talkApi } from "@/entities/talk";
import { queryKeys, type PaginationParams } from "@/shared/api";
import { DEFAULT_PAGE_SIZE } from "@/shared/lib/pagination-params";
import { useTalksMutations } from "./useTalksMutations";

export function useTalks(params?: PaginationParams) {
  const { data } = useSuspenseQuery({
    queryKey: queryKeys.talks.lists(params),
    queryFn: () => talkApi.getTalks(params),
  });

  const mutations = useTalksMutations();

  return {
    talks: data?.items ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? 0,
    pageSize: data?.pageSize ?? DEFAULT_PAGE_SIZE,
    totalPages: data?.totalPages ?? 0,
    pagedResult: data,
    ...mutations,
  };
}
