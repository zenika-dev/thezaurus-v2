import { ConferenceCFPStatus } from "@/entities/conference";
import { conferenceCFPStatusConfig } from "@/entities/conference";
import { Badge } from "@/shared/ui";

export { conferenceCFPStatusConfig as statusConfig };

export function StatusTag({ status }: { status: ConferenceCFPStatus }) {
  const { text, bg, darkText, darkBg } = conferenceCFPStatusConfig[status];
  return (
    <Badge color={text} bg={bg} darkColor={darkText} darkBg={darkBg}>
      {status}
    </Badge>
  );
}
