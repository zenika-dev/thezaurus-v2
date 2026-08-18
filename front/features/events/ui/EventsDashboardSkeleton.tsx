export function EventsContentSkeleton() {
  return (
    <div className="flex flex-col gap-6 animate-pulse">
      <div className="rounded-xl border border-border bg-surface p-6 shadow-card">
        <div className="mb-2 h-6 w-48 rounded bg-surface-muted" />
        <div className="mb-4 h-4 w-64 rounded bg-surface-muted" />
        <div className="h-[340px] w-full rounded bg-surface-muted" />
      </div>
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-6 shadow-card">
            <div className="h-5 w-40 rounded bg-surface-muted" />
            <div className="h-8 w-12 rounded bg-surface-muted" />
            <div className="h-4 w-32 rounded bg-surface-muted" />
            <div className="mt-2 flex flex-col gap-2">
              {Array.from({ length: 4 }).map((_, j) => (
                <div key={j} className="h-4 w-full rounded bg-surface-muted" />
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
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between animate-pulse">
        <div className="flex flex-col gap-2">
          <div className="h-9 w-56 rounded bg-surface-muted" />
          <div className="h-4 w-80 rounded bg-surface-muted" />
        </div>
        <div className="flex gap-2">
          <div className="h-6 w-24 rounded-2xl bg-surface-muted" />
          <div className="h-6 w-24 rounded-2xl bg-surface-muted" />
        </div>
      </div>
      <EventsContentSkeleton />
    </div>
  );
}
