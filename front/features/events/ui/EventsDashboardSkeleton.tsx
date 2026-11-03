import { FiltersSkeleton, PageHeaderSkeleton, SectionCardSkeleton } from "@/shared/ui";

export function EventsContentSkeleton() {
  return (
    <div className="flex flex-col gap-6 animate-pulse">
      <SectionCardSkeleton rows={4} />
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-3 rounded-2xl border border-primary bg-surface p-6 shadow-card">
            <div className="h-5 w-40 rounded bg-border-strong" />
            <div className="h-8 w-12 rounded bg-border-strong" />
            <div className="h-4 w-32 rounded bg-border-strong" />
            <div className="mt-2 flex min-w-0 max-w-full flex-col gap-2">
              {Array.from({ length: 4 }).map((_, j) => (
                <div key={j} className="h-4 w-full rounded bg-border-strong" />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function EventsDashboardSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeaderSkeleton />
      <FiltersSkeleton />
      <EventsContentSkeleton />
    </div>
  );
}
