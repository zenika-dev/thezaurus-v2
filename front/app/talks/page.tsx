import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { talkApi } from "@/entities/talk";
import { queryKeys, TalkStatus } from "@/shared/api";
import { getQueryClient, parsePaginationSearchParams } from "@/shared/lib";
import { TalkDashboard } from "@/features/talks";
import { TalkDashboardSkeleton } from "@/features/talks/ui/TalkDashboardSkeleton";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Talks",
  description: "Gérez le cycle de vie des talks, de l'idée au replay.",
};

interface TalksPageProps {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

export default async function TalksPage({ searchParams }: TalksPageProps) {
  const resolvedParams = await searchParams;
  return (
    <Suspense fallback={<TalkDashboardSkeleton />}>
      <TalksLoader rawParams={resolvedParams} />
    </Suspense>
  );
}

async function TalksLoader({
  rawParams,
}: {
  rawParams: { [key: string]: string | string[] | undefined };
}) {
  const queryClient = getQueryClient();
  const apiParams = parsePaginationSearchParams(rawParams, TalkStatus);

  await queryClient.prefetchQuery({
    queryKey: queryKeys.talks.lists(apiParams),
    queryFn: () => talkApi.getTalks(apiParams),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <TalkDashboard />
    </HydrationBoundary>
  );
}
