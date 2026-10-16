export function BlogPostsListSkeleton() {
  return (
    <div className="grid gap-3 [grid-template-columns:repeat(auto-fill,minmax(min(100%,460px),1fr))]">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="p-4 flex flex-col justify-between rounded-2xl border border-gray-200 dark:border-[#2d2d2d] animate-pulse min-h-[110px]"
        >
          <div className="flex justify-between items-start gap-2">
            <div className="h-5 bg-gray-200 dark:bg-[#121212] rounded w-[60%]" />
            <div className="h-6 w-16 bg-gray-200 dark:bg-[#121212] rounded-full shrink-0" />
          </div>
          <div className="flex flex-col gap-1 mt-2">
            <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-[40%]" />
            <div className="flex gap-1 mt-1">
              {Array.from({ length: 3 }).map((_, j) => (
                <div key={j} className="h-4 w-12 bg-gray-200 dark:bg-[#121212] rounded" />
              ))}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export function BlogPostsSkeleton() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6 animate-pulse">
        <div className="flex flex-col gap-2">
          <div className="h-9 bg-gray-200 dark:bg-[#121212] rounded w-36" />
          <div className="h-4 bg-gray-200 dark:bg-[#121212] rounded w-64" />
        </div>
        <div className="h-9 bg-gray-200 dark:bg-[#121212] rounded w-36" />
      </div>
      <BlogPostsListSkeleton />
    </div>
  );
}
