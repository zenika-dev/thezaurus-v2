import { Box } from "@mui/material";
import { type BlogPostStatus } from "../../types/post";

export const statusConfig: Record<
  BlogPostStatus,
  { text: string; bg: string }
> = {
  Draft: { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Published: { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
  Idea: { text: "#007fff", bg: "rgba(0, 127, 255, 0.12)" },
  Review: { text: "#f59f0a", bg: "rgba(245, 159, 10, 0.12)" },
};

export function StatusTag({ status }: { status: BlogPostStatus }) {
  const config = statusConfig[status];
  return (
    <Box
      sx={{
        px: 1,
        py: 0.25,
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
