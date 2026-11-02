"use client";

import Paper, { type PaperProps } from "@mui/material/Paper";

export function Surface({ sx, ...props }: PaperProps) {
  return <Paper variant="outlined" {...props} sx={[
    { borderColor: "var(--app-primary)", borderRadius: "var(--app-radius)", backgroundImage: "none" },
    ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
  ]} />;
}
