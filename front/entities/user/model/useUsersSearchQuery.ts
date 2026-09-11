import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/shared/api";
import { userApi } from "../api";

export function useUsersSearchQuery(query: string) {
  const trimmed = query.trim();
  const isEnabled = trimmed.length >= 1;

  return useQuery({
    queryKey: queryKeys.users.search(trimmed),
    queryFn: () => userApi.getUsers(trimmed),
    enabled: isEnabled,
    staleTime: 60_000,
  });
}
