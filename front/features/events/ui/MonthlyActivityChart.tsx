"use client";

import { BarChart } from "@mui/x-charts/BarChart";
import useMediaQuery from "@mui/material/useMediaQuery";
import {
  visibilityColors,
  type MonthlyActivity,
} from "@/entities/event";
import { MonthLabel } from "@/shared/api";
import type { EventFilter } from "@/features/events/model";

interface MonthlyActivityChartProps {
  monthly: MonthlyActivity[];
  filter: EventFilter;
}

export function MonthlyActivityChart({ monthly, filter }: MonthlyActivityChartProps) {
  const mobile = useMediaQuery("(max-width: 599px)");

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
    <div className="min-w-0" role="region" aria-label="Activité mensuelle">
      <BarChart
        skipAnimation
        height={mobile ? 560 : 340}
        layout={mobile ? "horizontal" : "vertical"}
        series={series}
        xAxis={mobile ? [{ min: 0 }] : [{ scaleType: "band", data: [...MonthLabel] }]}
        yAxis={mobile ? [{ scaleType: "band", data: [...MonthLabel] }] : undefined}
        grid={mobile ? { vertical: true } : { horizontal: true }}
        margin={{ top: 16, bottom: 24, left: 8, right: 8 }}
        borderRadius={8}
      />
    </div>
  );
}
