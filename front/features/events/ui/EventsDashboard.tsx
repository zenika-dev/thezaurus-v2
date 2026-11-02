"use client";

import { Suspense, useState } from "react";
import { DataErrorBoundary, PageHeader, FilterChip } from "@/shared/ui";
import type { EventFilter } from "@/features/events/model";
import { EventsContent } from "./EventsContent";
import { EventsCounters, EventsCountersSkeleton } from "./EventsCounters";
import { EventsContentSkeleton } from "./EventsDashboardSkeleton";

const YEAR = 2026;

const FILTERS: { value: EventFilter; label: string }[] = [
  { value: "all", label: "Tous" },
  { value: "external", label: "Externes" },
  { value: "internal", label: "Internes" },
];

export function EventsDashboard() {
  const [filter, setFilter] = useState<EventFilter>("all");

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader title={`Événements ${YEAR}`} subtitle="Suivi des événements internes et externes par agence Zenika."
        action={<Suspense fallback={<EventsCountersSkeleton />}><DataErrorBoundary><EventsCounters /></DataErrorBoundary></Suspense>}
      />

      <div className="mb-6 flex flex-wrap gap-2" role="group" aria-label="Filtrer les événements">
        {FILTERS.map((f) => {
          const active = filter === f.value;
          return (
            <FilterChip key={f.value} label={f.label} active={active} onClick={() => setFilter(f.value)} />
          );
        })}
      </div>

      <Suspense fallback={<EventsContentSkeleton />}>
        <DataErrorBoundary>
          <EventsContent filter={filter} />
        </DataErrorBoundary>
      </Suspense>
    </div>
  );
}

export default EventsDashboard;
