"use client";

import { Funnel } from "lucide-react";
import Button from "@mui/material/Button";
import { FilterChip } from "./FilterChip";

interface ContributionFiltersProps<S extends string> {
  statuses: readonly S[];
  labels: Record<S, { label: string }>;
  status: S | "All";
  onStatusChange: (status: S | "All") => void;
  personalLabel: string;
  personalOnly: boolean;
  onPersonalChange: (enabled: boolean) => void;
  onReset?: () => void;
}

export function ContributionFilters<S extends string>({
  statuses, labels, status, onStatusChange, personalLabel, personalOnly, onPersonalChange, onReset,
}: ContributionFiltersProps<S>) {
  return (
    <div className="flex items-center gap-6 flex-wrap mb-6">
      <FilterChip
        active={personalOnly}
        onClick={() => onPersonalChange(!personalOnly)}
        label={personalLabel}
      />
      <div className="flex items-center gap-2 flex-wrap" role="group" aria-label="Statut">
        <Funnel size={14} className="text-text-muted shrink-0" />
        <span className="text-xs text-text-muted mr-2">Statut :</span>
        {(["All", ...statuses] as const).map((value) => (
          <FilterChip
            key={value}
            active={status === value}
            onClick={() => onStatusChange(value)}
            label={value === "All" ? "Tous" : labels[value].label}
          />
        ))}
      </div>
      {(status !== "All" || personalOnly) && (
        <Button
          type="button"
          onClick={() => {
            if (onReset) {
              onReset();
            } else {
              onStatusChange("All");
              onPersonalChange(false);
            }
          }}
          variant="outlined" size="small"
        >
          Réinitialiser
        </Button>
      )}
    </div>
  );
}
