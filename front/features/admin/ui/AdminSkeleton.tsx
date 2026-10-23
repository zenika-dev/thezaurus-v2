export function AdminSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8 animate-pulse">
      <div className="h-[37.5px] sm:h-[42.5px] w-60 max-w-full rounded bg-border-strong mb-8" />

      <div className="flex flex-wrap gap-4 border-b border-border pb-2 mb-6">
        <div className="h-8 bg-border-strong rounded w-36" />
        <div className="h-8 bg-border-strong rounded w-44" />
      </div>

      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 mb-6">
        <div className="h-10 bg-border-strong rounded-xl w-full sm:w-72" />
        <div className="h-8 bg-border-strong rounded-2xl w-full sm:w-60" />
      </div>

      <div className="rounded-2xl border border-primary p-4 sm:p-6 flex flex-col gap-4 bg-surface">
        <div className="h-10 bg-border-strong rounded w-full" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-12 bg-border-strong rounded w-full" />
        ))}
      </div>
    </div>
  );
}
