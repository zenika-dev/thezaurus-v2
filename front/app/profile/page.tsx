import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { profileApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import { getQueryClient } from "@/shared/lib";
import { Profile } from "@/features/profile";
import { ProfileSkeleton } from "@/features/profile/ui/ProfileSkeleton";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Mon profil",
  description: "Informations personnelles et préférences de notification.",
};

export default function ProfilePage() {
  return (
    <Suspense fallback={<ProfileSkeleton />}>
      <ProfileLoader />
    </Suspense>
  );
}

async function ProfileLoader() {
  const queryClient = getQueryClient();
  // La session NextAuth ne porte que les rôles : le profil vient du backend.
  await queryClient.prefetchQuery({
    queryKey: queryKeys.profile.me(),
    queryFn: () => profileApi.getProfile(),
  });
  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <Profile />
    </HydrationBoundary>
  );
}
