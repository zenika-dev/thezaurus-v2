import type { Route } from "./+types/talks";
import TalkDashboard from "~/components/CreateTalkForm";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Talks" },
    { name: "description", content: "Gestion des talks Zenika" },
  ];
}

export default function TalksPage() {
  return <TalkDashboard />;
}
