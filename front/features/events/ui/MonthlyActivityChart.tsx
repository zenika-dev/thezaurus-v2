"use client";

import { BarChart } from "@mui/x-charts/BarChart";
import {
  MONTH_LABELS,
  visibilityColors,
  type MonthlyActivity,
} from "@/entities/event";
import type { EventFilter } from "@/features/events/model";

interface MonthlyActivityChartProps {
  monthly: MonthlyActivity[];
  filter: EventFilter;
}

export function MonthlyActivityChart({ monthly, filter }: MonthlyActivityChartProps) {

  const series = [];
  if (filter !== "internal") {
    series.push({
      id: "external",
      label: "Externes",
      color: visibilityColors.external.chart,
      data: monthly.map((m) => m.external),
    });
  }
  if (filter !== "external") {
    series.push({
      id: "internal",
      label: "Internes",
      color: visibilityColors.internal.chart,
      data: monthly.map((m) => m.internal),
    });
  }

  return (
    <div className="overflow-x-auto">
      <div className="min-w-[640px]">
        <BarChart
          skipAnimation
          height={340}
          series={series}
          xAxis={[{ scaleType: "band", data: [...MONTH_LABELS] }]}
          grid={{ horizontal: true }}
          margin={{ top: 16, bottom: 24, left: 8, right: 8 }}
          borderRadius={8}
        />
      </div>
    </div>
  );
}
