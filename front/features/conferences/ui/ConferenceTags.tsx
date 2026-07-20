import { ConferenceCFPStatus } from "@/entities/conference";
import { conferenceCFPStatusConfig } from "@/entities/conference";
import { Badge } from "@/shared/ui";

export { conferenceCFPStatusConfig as statusConfig };

export function StatusTag({ status }: { status: ConferenceCFPStatus }) {
  if (status === "None" || !status) return null;
  const { text, bg, darkText, darkBg } = conferenceCFPStatusConfig[status];
  return (
    <Badge color={text} bg={bg} darkColor={darkText} darkBg={darkBg}>
      {status}
    </Badge>
  );
}

import { ConferenceType, conferenceTypeConfig } from "@/entities/conference";

export function TypeTag({
  type,
  small,
}: {
  type: ConferenceType;
  small?: boolean;
}) {
  const { text, bg, darkText, darkBg } = conferenceTypeConfig[type];
  return (
    <Badge
      color={text}
      bg={bg}
      darkColor={darkText}
      darkBg={darkBg}
      size={small ? "small" : "default"}
    >
      {type}
    </Badge>
  );
}
