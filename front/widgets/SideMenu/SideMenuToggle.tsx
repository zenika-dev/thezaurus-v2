"use client";

import { IconButton, useTheme } from "@mui/material";

interface SideMenuToggleProps {
  open: boolean;
  onToggle: () => void;
}

export function SideMenuToggle({ open, onToggle }: SideMenuToggleProps) {
  const theme = useTheme();

  return (
    <IconButton
      onClick={onToggle}
      aria-expanded={open}
      aria-label={open ? "Réduire la navigation" : "Développer la navigation"}
      sx={{
        width: 28,
        height: 28,
        backgroundColor: open ? "rgba(0, 0, 0, 0.04)" : "white",
        borderRadius: "50%",
        color: "#64748b",
        position: "absolute",
        right: open ? 20 : -14,
        top: "50%",
        transform: "translateY(-50%)",
        zIndex: 10,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow: open ? "none" : "0 2px 8px rgba(0,0,0,0.08)",
        border: open ? "none" : "1px solid rgba(0, 0, 0, 0.08)",
        "&:hover": {
          backgroundColor: "#D51F51",
          color: "white",
          transform: "translateY(-50%) scale(1.1)",
          boxShadow: open
            ? "none"
            : `0 4px 12px ${theme.palette.primary.main}40`,
        },
        transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
        fontSize: "1.2rem",
        lineHeight: 1,
        p: 0,
      }}
    >
      {open ? "‹" : "›"}
    </IconButton>
  );
}
