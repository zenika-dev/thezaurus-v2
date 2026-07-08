"use client";

import { Suspense, useState } from "react";
import { DataErrorBoundary } from "@/shared/ui";
import type { EventFilter } from "@/features/events/model";
import { EventsContent } from "./EventsContent";
import { EventsCounters, EventsCountersSkeleton } from "./EventsCounters";
import { EventsContentSkeleton } from "./EventsDashboardSkeleton";
import Button from "@mui/material/Button";

const YEAR = 2026;

const FILTERS: { value: EventFilter; label: string }[] = [
  { value: "all", label: "Tous" },
  { value: "external", label: "Externes" },
  { value: "internal", label: "Internes" },
];

export function EventsDashboard() {
  const [filter, setFilter] = useState<EventFilter>("all");

  return (
    <div className="p-8">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="m-0 text-[34px] font-bold leading-[1.235] tracking-[0.25px] text-text">
            Événements {YEAR}
          </h1>
          <p className="m-0 text-[14px] leading-[1.43] tracking-[0.15px] text-text-muted">
            Suivi des événements internes et externes par agence Zenika.
          </p>
        </div>
        <Suspense fallback={<EventsCountersSkeleton />}>
          <DataErrorBoundary>
            <EventsCounters />
          </DataErrorBoundary>
        </Suspense>
      </div>

      <div className="mb-6 flex gap-2" role="group" aria-label="Filtrer les événements">
        {FILTERS.map((f) => {
          const active = filter === f.value;
          return (
            <Button
              key={f.value}
              type="button"
              onClick={() => setFilter(f.value)}
              aria-pressed={active}
              className={`rounded-2xl px-4 py-1.5 text-sm font-bold transition-colors ${
                active
                  ? "bg-[linear-gradient(135deg,#ed213c_0%,#bf1d67_100%)] text-white!"
                  : "bg-surface-muted text-text-muted hover:bg-surface-hover"
              }`}
            >
              {f.label}
            </Button>
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
