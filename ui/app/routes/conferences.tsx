import type { Route } from "./+types/conferences";
import Conferences from "~/components/Conferences";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Conférences" },
    { name: "description", content: "Gestion des conférences Zenika" },
  ];
}

export default function ConferencesPage() {
  return <Conferences />;
}
