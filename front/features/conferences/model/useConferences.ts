import { useSuspenseQuery } from "@tanstack/react-query";
import { conferenceApi } from "@/entities/conference";
import { queryKeys } from "@/shared/api";
import { useConferencesMutations } from "./useConferencesMutations";

export function useConferences() {
  const { data: conferences } = useSuspenseQuery({
    queryKey: [...queryKeys.conferences.lists(), "statistics"],
    queryFn: () => conferenceApi.getConferencesWithStatistics(),
  });

  const mutations = useConferencesMutations();

  return { conferences: conferences ?? [], ...mutations };
}
