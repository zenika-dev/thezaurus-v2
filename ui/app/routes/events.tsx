import type { Route } from "./+types/events";
import Events from "~/components/Events/Events";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Événements" },
    { name: "description", content: "Gestion des événements Zenika" },
  ];
}

export default function EventsPage() {
  return <Events />;
}
