import { Lock, Globe } from "lucide-react";
import type { TalkStatus } from "@/entities/talk";
import { talkStatusConfig, visibilityLabels } from "@/entities/talk";
import { Badge } from "@/shared/ui";

export { talkStatusConfig as statusConfig };

export function StatusTag({ status }: { status: TalkStatus }) {
  const { text, bg } = talkStatusConfig[status];
  return (
    <Badge color={text} bg={bg}>
      {status}
    </Badge>
  );
}

export function VisibilityTag({ visibility }: { visibility: string }) {
  const isExternal = visibility === "external";
  const label = visibilityLabels[visibility] || visibility;
  const config = isExternal
    ? { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)", Icon: Globe }
    : { text: "#757575", bg: "rgba(117, 117, 117, 0.12)", Icon: Lock };

  return (
    <Badge color={config.text} bg={config.bg}>
      <config.Icon size={12} />
      <span>{label}</span>
    </Badge>
  );
}
