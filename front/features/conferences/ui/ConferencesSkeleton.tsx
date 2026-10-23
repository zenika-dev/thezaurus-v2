import { FiltersSkeleton, PageHeaderSkeleton } from "@/shared/ui/LoadingLayout";

export function ConferencesListSkeleton() {
  return <div role="status" aria-label="Chargement des conférences">
    <FiltersSkeleton />
    <div aria-hidden="true" className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse">
      {Array.from({ length: 6 }, (_, i) => <div key={i} className="p-6 rounded-2xl border border-primary space-y-3">
        <div className="h-5 w-3/4 rounded bg-border-strong" />
        <div className="h-4 w-1/2 rounded bg-border-strong" />
        <div className="h-4 w-2/3 rounded bg-border-strong" />
        <div className="h-6 w-24 rounded-full bg-border-strong" />
        <div className="h-4 w-full rounded bg-border-strong" />
      </div>)}
    </div>
  </div>;
}

export function ConferencesSkeleton() {
  return <div className="p-4 sm:p-6 lg:p-8"><PageHeaderSkeleton /><ConferencesListSkeleton /></div>;
}
