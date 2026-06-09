const COLUMNS = 7;

export function TalkTableSkeleton() {
  return (
    <div className="border border-gray-200 rounded overflow-hidden animate-pulse">
      <table className="w-full">
        <thead className="bg-gray-50">
          <tr>
            {Array.from({ length: COLUMNS }).map((_, i) => (
              <th key={i} className="p-3 text-left">
                <div className="h-4 bg-gray-200 rounded w-[70%]" />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: 5 }).map((_, i) => (
            <tr key={i} className="border-t border-gray-100">
              {Array.from({ length: COLUMNS }).map((_, j) => (
                <td key={j} className="p-3">
                  <div className={`h-4 bg-gray-200 rounded ${j === COLUMNS - 1 ? "w-12" : "w-[80%]"}`} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function TalkDashboardSkeleton() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6 animate-pulse">
        <div className="flex flex-col gap-2">
          <div className="h-9 bg-gray-200 rounded w-24" />
          <div className="h-4 bg-gray-200 rounded w-72" />
        </div>
        <div className="h-9 bg-gray-200 rounded w-32" />
      </div>
      <TalkTableSkeleton />
    </div>
  );
}
