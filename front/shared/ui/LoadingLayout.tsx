import type { HTMLAttributes } from "react";

export function FiltersSkeleton() {
  return (
    <div aria-hidden="true" className="mb-6 flex flex-wrap gap-2 animate-pulse">
      {[24, 20, 24, 20].map((width, index) => (
        <div key={index} className="h-11 rounded-full bg-border-strong" style={{ width: `${width / 4}rem` }} />
      ))}
    </div>
  );
}

export function PageHeaderSkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-4 mb-8 sm:flex-row sm:items-center sm:justify-between animate-pulse">
      <div className="min-w-0 max-w-full space-y-2">
        <div className="h-[37.5px] sm:h-[42.5px] w-40 max-w-full rounded bg-border-strong" />
        <div className="h-6 w-72 max-w-full rounded bg-border-strong" />
      </div>
      <div className="h-10 w-40 shrink-0 rounded-full bg-border-strong" />
    </div>
  );
}

export interface SectionCardSkeletonProps extends HTMLAttributes<HTMLDivElement> {
  rows?: number;
  hasHeader?: boolean;
  className?: string;
}

export function SectionCardSkeleton({
  rows = 3,
  hasHeader = true,
  className = "",
  ...props
}: SectionCardSkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={`rounded-2xl border border-primary bg-surface p-4 sm:p-6 animate-pulse ${className}`}
      {...props}
    >
      {hasHeader && (
        <>
          <div className="h-6 bg-border-strong rounded w-40 mb-2" />
          <div className="h-4 bg-border-strong rounded w-[70%] mb-6" />
        </>
      )}
      <div className="flex flex-col gap-3">
        {Array.from({ length: rows }).map((_, i) => (
          <div
            key={i}
            className="h-5 bg-border-strong rounded"
            style={{ width: `${Math.max(35, 100 - i * 20)}%` }}
          />
        ))}
      </div>
    </div>
  );
}
