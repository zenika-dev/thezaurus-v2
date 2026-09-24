"use client";

import { Funnel } from "lucide-react";
import FormControlLabel from "@mui/material/FormControlLabel";
import Switch from "@mui/material/Switch";

interface ContributionFiltersProps<S extends string> {
  statuses: readonly S[];
  labels: Record<S, { label: string }>;
  status: S | "All";
  onStatusChange: (status: S | "All") => void;
  personalLabel: string;
  personalOnly: boolean;
  onPersonalChange: (enabled: boolean) => void;
}

export function ContributionFilters<S extends string>({
  statuses, labels, status, onStatusChange, personalLabel, personalOnly, onPersonalChange,
}: ContributionFiltersProps<S>) {
  return (
    <div className="flex items-center gap-6 flex-wrap mb-6">
      <div className="flex items-center gap-2 flex-wrap" role="group" aria-label="Statut">
        <Funnel size={14} className="text-text-muted shrink-0" />
        <span className="text-xs text-text-muted mr-2">Statut :</span>
        {(["All", ...statuses] as const).map((value) => (
          <button
            key={value}
            type="button"
            aria-pressed={status === value}
            onClick={() => onStatusChange(value)}
            className={`inline-flex items-center gap-1 px-3 py-1 rounded-2xl text-xs font-sans border cursor-pointer transition-colors ${
              status === value ? "bg-primary text-white border-primary" : "bg-surface-hover text-text border-transparent hover:bg-border-strong"
            }`}
          >
            {value === "All" ? "Tous" : labels[value].label}
          </button>
        ))}
      </div>
      <FormControlLabel
        control={<Switch size="small" checked={personalOnly} onChange={(_, checked) => onPersonalChange(checked)} />}
        label={personalLabel}
      />
      {(status !== "All" || personalOnly) && (
        <button
          type="button"
          onClick={() => { onStatusChange("All"); onPersonalChange(false); }}
          className="text-xs cursor-pointer text-primary border border-primary/20 px-3 py-1 rounded-2xl bg-primary/10 hover:bg-primary/20"
        >
          Réinitialiser
        </button>
      )}
    </div>
  );
}
