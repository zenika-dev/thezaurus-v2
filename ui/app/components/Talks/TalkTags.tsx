import { Box } from "@mui/material";
import { Lock, Globe } from "lucide-react";
import { type TalkStatus, visibilityLabels } from "../../types/talk";

export const statusConfig: Record<TalkStatus, { text: string; bg: string }> = {
  Draft: { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Idea: { text: "#007fff", bg: "rgba(0, 127, 255, 0.12)" },
  Submitted: { text: "#f59f0a", bg: "rgba(245, 159, 10, 0.12)" },
  Accepted: { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
  Replayed: { text: "#d32f2f", bg: "rgba(211, 47, 47, 0.12)" },
};

export function StatusTag({ status }: { status: TalkStatus }) {
  const config = statusConfig[status];
  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.5,
        borderRadius: 1,
        display: "inline-block",
        fontSize: "0.75rem",
        fontWeight: "bold",
        border: `1px solid ${config.bg}`,
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      {status}
    </Box>
  );
}

export function VisibilityTag({ visibility }: { visibility: string }) {
  const isExternal = visibility === "external";
  const label = visibilityLabels[visibility] || visibility;
  const config = isExternal
    ? { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)", Icon: Globe }
    : { text: "#757575", bg: "rgba(117, 117, 117, 0.12)", Icon: Lock };

  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.8,
        borderRadius: 1,
        border: `1px solid ${config.bg}`,
        display: "inline-flex",
        alignItems: "center",
        gap: 0.5,
        fontSize: "0.75rem",
        lineHeight: 1,
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      <config.Icon size={12} />
      <span>{label}</span>
    </Box>
  );
}
