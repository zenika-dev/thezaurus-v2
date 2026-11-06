import { useSuspenseQuery } from "@tanstack/react-query";
import { eventApi } from "@/entities/event";
import { queryKeys } from "@/shared/api";

export function useEventsDashboard(year: number) {
  const { data } = useSuspenseQuery({
    queryKey: queryKeys.events.dashboard(year),
    queryFn: () => eventApi.getEventsDashboard(year),
  });

  return data;
}