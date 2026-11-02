import { useSuspenseQuery } from "@tanstack/react-query";
import { conferenceApi } from "@/entities/conference";
import { queryKeys } from "@/shared/api";
import { useConferencesMutations } from "./useConferencesMutations";

export function useConferences() {
  const { data: conferences } = useSuspenseQuery({
    queryKey: queryKeys.conferences.lists(),
    queryFn: () => conferenceApi.getConferences(),
  });

  const mutations = useConferencesMutations();

  return { conferences: conferences ?? [], ...mutations };
}
