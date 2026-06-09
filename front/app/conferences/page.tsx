import type { Metadata } from "next";
import { Conferences } from "@/features/conferences";

export const metadata: Metadata = {
  title: "Conférences",
  description: "Conférences et soumissions Zenika.",
};

export default function ConferencesPage() {
  return <Conferences />;
}
