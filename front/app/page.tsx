import type { Metadata } from "next";
import { Events } from "@/features/events";

export const metadata: Metadata = {
  title: "Événements",
  description: "Calendrier des événements Zenika.",
};

export default function HomePage() {
  return <Events />;
}
