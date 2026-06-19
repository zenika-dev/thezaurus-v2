import type { Metadata } from "next";
import { Events } from "@/features/events";

export const metadata: Metadata = {
  title: "TheZaurus",
  description: "Partagez vos connaissances chez Zenika.",
};

export default function HomePage() {
  return <Events />;
}
