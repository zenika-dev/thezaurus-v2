"use client";

import { MapPin } from "lucide-react";
import { Badge } from "@/shared/ui";
import { visibilityColors, visibilityLabels, type EventTypeSummary } from "@/entities/event";

interface EventTypeCardProps {
  summary: EventTypeSummary;
  year: number;
}

export function EventTypeCard({ summary, year }: EventTypeCardProps) {
  const badge = visibilityColors[summary.visibility];

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-[var(--color-primary)] bg-surface p-6 shadow-card">
      <div className="flex items-start justify-between gap-2">
        <h3 className="m-0 text-base font-bold text-text">{summary.name}</h3>
        <Badge color={badge.color} bg={badge.bg} darkColor={badge.darkColor} darkBg={badge.darkBg}>
          {visibilityLabels[summary.visibility]}
        </Badge>
      </div>

      <div>
        <p className="m-0 text-2xl font-extrabold text-text">{summary.total}</p>
        <p className="m-0 text-xs text-text-muted">événements en {year}</p>
      </div>

      <ul className="m-0 flex list-none flex-col gap-2 p-0">
        {summary.cities.map((city) => (
          <li key={city.city} className="flex items-center justify-between text-xs text-text">
            <span className="flex items-center gap-1.5 text-text-muted">
              <MapPin size={14} aria-hidden="true" />
              {city.city}
            </span>
            <span className="font-semibold text-text">{city.count}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
