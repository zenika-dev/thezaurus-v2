import { useSuspenseQuery } from "@tanstack/react-query";
import { profileApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";

export function useProfile() {
  const { data: profile } = useSuspenseQuery({
    queryKey: queryKeys.profile.me(),
    queryFn: () => profileApi.getProfile(),
  });

  return { profile };
}
