"use client";

import { Suspense, useState } from "react";
import { DataErrorBoundary, PageHeader, FilterChip } from "@/shared/ui";
import type { EventFilter } from "@/features/events/model";
import { EventsContent } from "./EventsContent";
import { EventsCounters, EventsCountersSkeleton } from "./EventsCounters";
import { EventsContentSkeleton } from "./EventsDashboardSkeleton";

const CURRENT_YEAR = new Date().getFullYear();

const FILTERS: { value: EventFilter; label: string }[] = [
  { value: "all", label: "Tous" },
  { value: "external", label: "Externes" },
  { value: "internal", label: "Internes" },
];

export function EventsDashboard() {
  const [year, setYear] = useState(CURRENT_YEAR);
  const [filter, setFilter] = useState<EventFilter>("all");

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader title={`Événements ${year}`} subtitle="Suivi des événements internes et externes par agence Zenika."
        action={<Suspense fallback={<EventsCountersSkeleton />}><DataErrorBoundary><EventsCounters year={year} /></DataErrorBoundary></Suspense>}
      />

      <div className="mb-4 flex items-center gap-3">
        <label htmlFor="events-year" className="text-sm font-medium text-text">Année</label>
        <select id="events-year" value={year} onChange={(event) => setYear(Number(event.target.value))}
          className="rounded-lg border border-border-strong bg-surface px-3 py-2 text-sm text-text">
          {Array.from({ length: 21 }, (_, index) => CURRENT_YEAR + 1 - index).map((option) => (
            <option key={option} value={option}>{option}</option>
          ))}
        </select>
      </div>

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
          <EventsContent filter={filter} year={year} />
        </DataErrorBoundary>
      </Suspense>
    </div>
  );
}

export default EventsDashboard;