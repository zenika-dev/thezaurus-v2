import { getConferenceYear, type ConferenceData } from "@/entities/conference";
import { Surface } from "@/shared/ui";

export function ConferenceAnnualStatistics({ conferences }: { conferences: readonly ConferenceData[] }) {
  const years = new Map<number | null, { conferences: number; submitted: number; accepted: number }>();
  for (const conference of conferences) {
    const year = getConferenceYear(conference.date);
    const key = Number.isFinite(year) ? year : null;
    const totals = years.get(key) ?? { conferences: 0, submitted: 0, accepted: 0 };
    totals.conferences++;
    totals.submitted += conference.submittedTalksAmount;
    totals.accepted += conference.acceptedTalksAmount ?? 0;
    years.set(key, totals);
  }
  const rows = [...years].sort(([left], [right]) => {
    if (left === right) return 0;
    if (left === null) return 1;
    if (right === null) return -1;
    return right - left;
  });
  const total = rows.reduce((sum, [, row]) => ({
    conferences: sum.conferences + row.conferences,
    submitted: sum.submitted + row.submitted,
    accepted: sum.accepted + row.accepted,
  }), { conferences: 0, submitted: 0, accepted: 0 });

  return (
    <section aria-labelledby="conference-annual-statistics">
      <Surface className="p-4 sm:p-6">
        <h2 id="conference-annual-statistics" className="font-bold text-lg text-text">Cumuls par année</h2>
        <p className="mt-1 mb-4 text-xs text-text-muted">
          Toutes les conférences, regroupées par année de début.
        </p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-text">
            <thead className="text-xs text-text-muted">
              <tr>
                <th scope="col" className="py-2 pr-3 text-left">Année</th>
                <th scope="col" className="p-2 text-right">Conférences</th>
                <th scope="col" className="p-2 text-right">Talks soumis</th>
                <th scope="col" className="py-2 pl-3 text-right">Talks acceptés</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(([year, row]) => (
                <tr key={year ?? "unknown"} className="border-t border-border">
                  <th scope="row" className="py-2 pr-3 text-left font-semibold">{year ?? "Année non renseignée"}</th>
                  <td className="p-2 text-right tabular-nums">{row.conferences}</td>
                  <td className="p-2 text-right tabular-nums">{row.submitted}</td>
                  <td className="py-2 pl-3 text-right tabular-nums">{row.accepted}</td>
                </tr>
              ))}
            </tbody>
            <tfoot className="border-t border-border font-bold">
              <tr>
                <th scope="row" className="py-2 pr-3 text-left">Total</th>
                <td className="p-2 text-right tabular-nums">{total.conferences}</td>
                <td className="p-2 text-right tabular-nums">{total.submitted}</td>
                <td className="py-2 pl-3 text-right tabular-nums">{total.accepted}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </Surface>
    </section>
  );
}
