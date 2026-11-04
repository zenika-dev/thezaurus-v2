import type { Metadata } from "next";
import { ConferenceStatistics } from "@/features/conferences";

export const metadata: Metadata = {
  title: "Statistiques des conférences",
  description: "Cumuls annuels des soumissions et des acceptations des conférences.",
};

export default function ConferenceStatisticsPage() {
  return <ConferenceStatistics />;
}
