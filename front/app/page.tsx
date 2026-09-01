import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { eventApi } from "@/entities/event";
import { queryKeys } from "@/shared/api";
import { getQueryClient } from "@/shared/lib";
import { EventsDashboard, EventsDashboardSkeleton } from "@/features/events";

export const dynamic = "force-dynamic";

const YEAR = 2026;

export const metadata: Metadata = {
  title: "Événements",
  description: "Suivi des événements internes et externes par agence Zenika.",
};

export default function HomePage() {
  return (
    <Suspense fallback={<EventsDashboardSkeleton />}>
      <EventsLoader />
    </Suspense>
  );
}

async function EventsLoader() {
  const queryClient = getQueryClient();
  await queryClient.prefetchQuery({
    queryKey: queryKeys.events.dashboard(YEAR),
    queryFn: () => eventApi.getEventsDashboard(YEAR),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <EventsDashboard />
    </HydrationBoundary>
  );
}
