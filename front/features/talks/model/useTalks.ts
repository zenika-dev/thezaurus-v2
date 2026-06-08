import { useSuspenseQuery } from "@tanstack/react-query";
import { talkApi } from "@/entities/talk";
import { queryKeys } from "@/shared/api";
import { useTalksMutations } from "./useTalksMutations";

export function useTalks() {
  const { data: talks } = useSuspenseQuery({
    queryKey: queryKeys.talks.lists(),
    queryFn: () => talkApi.getTalks(),
  });

  const mutations = useTalksMutations();

  return { talks: talks ?? [], ...mutations };
}
