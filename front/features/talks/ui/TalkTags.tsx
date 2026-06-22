import { Lock, Globe } from "lucide-react";
import type { TalkStatus } from "@/entities/talk";
import { talkStatusConfig, visibilityLabels } from "@/entities/talk";
import { Badge } from "@/shared/ui";

export { talkStatusConfig as statusConfig };

export function StatusTag({ status }: { status: TalkStatus }) {
  const { text, bg, darkText, darkBg } = talkStatusConfig[status];
  return (
    <Badge color={text} bg={bg} darkColor={darkText} darkBg={darkBg}>
      {status}
    </Badge>
  );
}

export function VisibilityTag({ visibility }: { visibility: string }) {
  const isExternal = visibility === "external";
  const label = visibilityLabels[visibility] || visibility;
  const config = isExternal
    ? { text: "#245E12", bg: "#DCFCE7", darkText: "#47FFB4", darkBg: "#0D542B", Icon: Globe }
    : { text: "#000000", bg: "#F7F7F7", darkText: "#FFFFFF", darkBg: "#5E5E5E", Icon: Lock };

  return (
    <Badge color={config.text} bg={config.bg} darkColor={config.darkText} darkBg={config.darkBg}>
      <config.Icon size={12} />
      <span>{label}</span>
    </Badge>
  );
}
