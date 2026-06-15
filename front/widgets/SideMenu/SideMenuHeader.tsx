"use client";

import { Box, Typography, useTheme, styled } from "@mui/material";
import { BookOpen } from "lucide-react";
import { SideMenuToggle } from "./SideMenuToggle";

const ICON_SIZE = 36;
const COLLAPSED_WIDTH = 88;
const OPEN_PADDING_LEFT = 20;
const CLOSED_PADDING_LEFT = (COLLAPSED_WIDTH - ICON_SIZE) / 2;

const HeaderRoot = styled(Box, { shouldForwardProp: (p) => p !== "open" })<{
  open: boolean;
}>(({ theme, open }) => ({
  display: "flex",
  alignItems: "center",
  justifyContent: "flex-start",
  paddingLeft: open ? OPEN_PADDING_LEFT : CLOSED_PADDING_LEFT,
  height: 64,
  position: "relative",
  borderBottom: "1px solid rgba(0, 0, 0, 0.08)",
  flexShrink: 0,
  transition: theme.transitions.create("padding-left", {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.enteringScreen,
  }),
}));

interface SideMenuHeaderProps {
  open: boolean;
  onToggle: () => void;
}

export function SideMenuHeader({ open, onToggle }: SideMenuHeaderProps) {
  const theme = useTheme();

  return (
    <HeaderRoot open={open}>
      <Box sx={{ display: "flex", alignItems: "center" }}>
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: "12px",
            backgroundColor: "#D51F51",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "white",
            boxShadow: theme.shadows[1],
            flexShrink: 0,
          }}
        >
          <BookOpen size={20} aria-hidden="true" />
        </Box>

        <Box
          sx={{
            position: "absolute",
            left: OPEN_PADDING_LEFT + ICON_SIZE + 16,
            display: "flex",
            flexDirection: "column",
            opacity: open ? 1 : 0,
            pointerEvents: open ? "auto" : "none",
            whiteSpace: "nowrap",
            transition: open
              ? "opacity 0.15s ease-in 0.18s"
              : "opacity 0.1s ease-out 0s",
          }}
        >
          <Typography
            sx={{
              fontWeight: 800,
              color: theme.palette.mode === "dark" ? "hsl(var(--foreground))" : "#1e293b",
              letterSpacing: "-0.5px",
              fontSize: "1.125rem",
              lineHeight: 1.2,
            }}
          >
            Thezaurus
          </Typography>
          <Typography
            sx={{
              fontSize: "10px",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
              color: "#737373",
              fontWeight: 700,
              mt: 0.5,
            }}
          >
            by Zenika
          </Typography>
        </Box>
      </Box>

      <SideMenuToggle open={open} onToggle={onToggle} />
    </HeaderRoot>
  );
}
