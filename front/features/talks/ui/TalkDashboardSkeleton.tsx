import { FiltersSkeleton, PageHeaderSkeleton } from "@/shared/ui/LoadingLayout";

export function TalkTableSkeleton() {
  return <div role="status" aria-label="Chargement des talks">
    <FiltersSkeleton />
    <div aria-hidden="true" className="border border-primary rounded-2xl overflow-x-auto animate-pulse">
      <table className="w-full min-w-[760px] border-collapse">
        <thead className="bg-surface-muted"><tr>{Array.from({ length: 7 }, (_, i) =>
          <th key={i} className="p-4"><div className="h-5 w-3/4 rounded bg-border-strong" /></th>
        )}</tr></thead>
        <tbody>{Array.from({ length: 5 }, (_, i) => <tr key={i} className="border-t border-border">
          {Array.from({ length: 7 }, (_, j) => <td key={j} className="p-4"><div className="h-6 w-4/5 rounded bg-border-strong" /></td>)}
        </tr>)}</tbody>
      </table>
    </div>
  </div>;
}

export function TalkDashboardSkeleton() {
  return <div className="p-4 sm:p-6 lg:p-8"><PageHeaderSkeleton /><TalkTableSkeleton /></div>;
}
