"use client";

import { Globe, Building2 } from "lucide-react";
import { Badge } from "@/shared/ui";
import { visibilityColors } from "@/entities/event";
import { useEventsDashboard } from "@/features/events/model";

export function EventsCounters() {
  const { totals } = useEventsDashboard(2026);

  return (
    <div className="flex items-center gap-2">
      <Badge {...visibilityColors.external}>
        <Globe size={14} aria-hidden="true" />
        {totals.external} externes
      </Badge>
      <Badge {...visibilityColors.internal}>
        <Building2 size={14} aria-hidden="true" />
        {totals.internal} internes
      </Badge>
    </div>
  );
}

export function EventsCountersSkeleton() {
  return (
    <div className="flex items-center gap-2 animate-pulse">
      <div className="h-6 w-24 rounded-2xl bg-surface-muted" />
      <div className="h-6 w-24 rounded-2xl bg-surface-muted" />
    </div>
  );
}
