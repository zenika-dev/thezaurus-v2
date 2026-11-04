"use client";

import Link from "next/link";
import { Suspense } from "react";
import { ArrowLeft } from "lucide-react";
import { DataErrorBoundary, PageHeader, Surface } from "@/shared/ui";
import { useConferences } from "../model";
import { ConferenceAnnualStatistics } from "./ConferenceAnnualStatistics";

function StatisticsContent() {
  const { conferences } = useConferences();
  return <ConferenceAnnualStatistics conferences={conferences} />;
}

export function ConferenceStatistics() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader
        title="Statistiques des conférences"
        subtitle="Retrouvez les soumissions et les acceptations par année de conférence."
        action={
          <Link href="/conferences" className="inline-flex items-center gap-2 text-sm text-link hover:underline">
            <ArrowLeft size={16} />
            Retour aux conférences
          </Link>
        }
      />
      <Suspense fallback={<Surface className="p-6" role="status">Chargement des statistiques…</Surface>}>
        <DataErrorBoundary>
          <StatisticsContent />
        </DataErrorBoundary>
      </Suspense>
    </div>
  );
}
