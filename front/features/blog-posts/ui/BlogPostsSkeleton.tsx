import { FiltersSkeleton, PageHeaderSkeleton } from "@/shared/ui/LoadingLayout";

export function BlogPostsListSkeleton() {
  return (
    <div role="status" aria-label="Chargement des articles">
      <FiltersSkeleton />
      <div aria-hidden="true" className="grid gap-3 [grid-template-columns:repeat(auto-fill,minmax(min(100%,460px),1fr))]">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="p-4 flex flex-col justify-between rounded-2xl border border-primary animate-pulse min-h-[110px]"
        >
          <div className="flex justify-between items-start gap-2">
            <div className="h-5 bg-border-strong rounded w-[60%]" />
            <div className="h-6 w-16 bg-border-strong rounded-full shrink-0" />
          </div>
          <div className="flex flex-col gap-1 mt-2">
            <div className="h-4 bg-border-strong rounded w-[40%]" />
            <div className="flex gap-1 mt-1">
              {Array.from({ length: 3 }).map((_, j) => (
                <div key={j} className="h-4 w-12 bg-border-strong rounded" />
              ))}
            </div>
          </div>
        </div>
      ))}
      </div>
    </div>
  );
}

export function BlogPostsSkeleton() {
  return <div className="p-4 sm:p-6 lg:p-8"><PageHeaderSkeleton /><BlogPostsListSkeleton /></div>;
}
