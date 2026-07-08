"use client";

import { useEventsDashboard, type EventFilter } from "@/features/events/model";
import { MonthlyActivityChart } from "./MonthlyActivityChart";
import { EventTypeCard } from "./EventTypeCard";

interface EventsContentProps {
  filter: EventFilter;
}

export function EventsContent({ filter }: EventsContentProps) {
  const dashboard = useEventsDashboard(2026);

  const eventTypes = dashboard.eventTypes.filter(
    (type) => filter === "all" || type.visibility === filter
  );

  return (
    <div className="flex flex-col gap-6">
      <section className="rounded-xl border  border-[var(--color-primary)] bg-surface p-6 shadow-card">
        <h2 className="m-0 text-xl font-bold text-text">Activité mensuelle</h2>
        <p className="m-0 mb-2 text-sm text-text-muted">
          Événements internes vs externes par mois
        </p>
        <MonthlyActivityChart monthly={dashboard.monthly} filter={filter} />
      </section>

      {eventTypes.length > 0 && (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {eventTypes.map((type) => (
            <EventTypeCard key={type.name} summary={type} year={dashboard.year} />
          ))}
        </div>
      )}
    </div>
  );
}
