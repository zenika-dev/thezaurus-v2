export function AdminSkeleton() {
  return (
    <div className="p-8 animate-pulse">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-xl bg-gray-200 dark:bg-[#121212]" />
        <div className="h-8 bg-gray-200 dark:bg-[#121212] rounded w-48" />
      </div>

      <div className="flex gap-4 border-b border-border pb-2 mb-6">
        <div className="h-8 bg-gray-200 dark:bg-[#121212] rounded w-36" />
        <div className="h-8 bg-gray-200 dark:bg-[#121212] rounded w-44" />
      </div>

      <div className="flex justify-between items-center mb-6">
        <div className="h-10 bg-gray-200 dark:bg-[#121212] rounded-xl w-72" />
        <div className="h-8 bg-gray-200 dark:bg-[#121212] rounded-2xl w-60" />
      </div>

      <div className="rounded-2xl border border-border p-6 flex flex-col gap-4 bg-surface">
        <div className="h-10 bg-gray-200 dark:bg-[#121212] rounded w-full" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-12 bg-gray-200 dark:bg-[#121212] rounded w-full" />
        ))}
      </div>
    </div>
  );
}
