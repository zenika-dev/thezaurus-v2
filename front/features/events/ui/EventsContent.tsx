"use client";

import { useEventsDashboard, type EventFilter } from "@/features/events/model";
import { MonthlyActivityChart } from "./MonthlyActivityChart";
import { EventTypeCard } from "./EventTypeCard";
import { SectionCard } from "@/shared/ui";

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
      <SectionCard
        title="Activité mensuelle"
        description="Événements internes vs externes par mois"
      >
        <MonthlyActivityChart monthly={dashboard.monthly} filter={filter} />
      </SectionCard>

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
