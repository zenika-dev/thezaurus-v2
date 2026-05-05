import type { Route } from "./+types/home";
import TalkDashboard from "~/components/CreateTalkForm";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Thezaurus — Talks" },
    { name: "description", content: "Gestion des talks Zenika" },
  ];
}

export default function Home() {
  return <TalkDashboard />;
}
