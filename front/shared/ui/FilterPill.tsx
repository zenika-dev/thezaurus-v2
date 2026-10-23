"use client";

import type { ReactNode } from "react";

export interface FilterPillProps {
  label: ReactNode;
  active: boolean;
  onClick: () => void;
  disabled?: boolean;
  className?: string;
}

export function FilterPill({
  label,
  active,
  onClick,
  disabled = false,
  className = "",
}: FilterPillProps) {
  return (
    <button
      type="button"
      aria-pressed={active}
      disabled={disabled}
      onClick={onClick}
      className={`inline-flex items-center justify-center gap-1 min-h-10 px-3 py-1 rounded-2xl text-xs font-bold font-sans border cursor-pointer transition-colors disabled:opacity-50 disabled:cursor-default ${
        active
          ? "bg-primary-dark text-white border-primary-dark"
          : "bg-surface-hover text-text border-border hover:border-primary"
      } ${className}`}
    >
      {label}
    </button>
  );
}
