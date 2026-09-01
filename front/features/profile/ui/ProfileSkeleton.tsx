export function ProfileSectionsSkeleton() {
  return (
    <div className="flex flex-col gap-4">
      {Array.from({ length: 2 }).map((_, i) => (
        <div
          key={i}
          className="rounded border border-gray-200 dark:border-[#2d2d2d] p-6 animate-pulse"
        >
          <div className="h-6 bg-gray-200 dark:bg-[#121212] rounded w-40 mb-2" />
          <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-[70%] mb-6" />
          <div className="h-5 bg-gray-200 dark:bg-[#121212] rounded w-[45%] mb-3" />
          <div className="h-5 bg-gray-200 dark:bg-[#121212] rounded w-[35%]" />
        </div>
      ))}
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="p-8">
      <div className="mb-6 animate-pulse">
        <div className="h-9 bg-gray-200 dark:bg-[#121212] rounded w-36 mb-2" />
        <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-64" />
      </div>
      <ProfileSectionsSkeleton />
    </div>
  );
}
