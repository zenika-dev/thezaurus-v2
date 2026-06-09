import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { talkApi } from "@/entities/talk";
import { queryKeys } from "@/shared/api";
import { getQueryClient } from "@/shared/lib";
import { TalkDashboard } from "@/features/talks";
import { TalkDashboardSkeleton } from "@/features/talks/ui/TalkDashboardSkeleton";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Talks",
  description: "Gérez le cycle de vie des talks, de l'idée au replay.",
};

export default function TalksPage() {
  return (
    <Suspense fallback={<TalkDashboardSkeleton />}>
      <TalksLoader />
    </Suspense>
  );
}

async function TalksLoader() {
  const queryClient = getQueryClient();
  await queryClient.prefetchQuery({
    queryKey: queryKeys.talks.lists(),
    queryFn: () => talkApi.getTalks(),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <TalkDashboard />
    </HydrationBoundary>
  );
}
