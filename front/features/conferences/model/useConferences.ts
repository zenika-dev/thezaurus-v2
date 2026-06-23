import { useSuspenseQuery } from "@tanstack/react-query";
import { conferenceApi } from "@/entities/conference";
import { queryKeys } from "@/shared/api";

export function useConferences() {
  const { data: conferences } = useSuspenseQuery({
    queryKey: queryKeys.conferences.lists(),
    queryFn: () => conferenceApi.getConferences(),
  });

  return { conferences: conferences ?? [] };
}
