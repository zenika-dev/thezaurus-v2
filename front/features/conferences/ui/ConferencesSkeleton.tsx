// TODO edit to match conference page design

export function ConferencesListSkeleton() {
  return (
    <div className="flex flex-col gap-2">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="p-4 flex justify-between items-center rounded border border-gray-200 dark:border-[#2d2d2d] animate-pulse"
        >
          <div className="flex flex-col gap-1 flex-1">
            <div className="h-5 bg-gray-200 dark:bg-[#121212] rounded w-[55%]" />
            <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-[30%]" />
            <div className="flex gap-1 mt-1">
              {Array.from({ length: 3 }).map((_, j) => (
                <div
                  key={j}
                  className="h-4 w-12 bg-gray-200 dark:bg-[#121212] rounded"
                />
              ))}
            </div>
          </div>
          <div className="h-6 w-16 bg-gray-200 dark:bg-[#121212] rounded-full ml-2" />
        </div>
      ))}
    </div>
  );
}

export function ConferencesSkeleton() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6 animate-pulse">
        <div className="flex flex-col gap-2">
          <div className="h-9 bg-gray-200 dark:bg-[#121212] rounded w-36" />
          <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-64" />
        </div>
        <div className="h-9 bg-gray-200 dark:bg-[#121212] rounded w-36" />
      </div>
      <ConferencesListSkeleton />
    </div>
  );
}
