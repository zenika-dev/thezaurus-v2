"use client";

import {
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Tooltip,
  useTheme,
} from "@mui/material";

export interface NavItemConfig {
  icon: React.ElementType;
  label: string;
  active: boolean;
  onClick: () => void;
}

interface SideMenuNavItemProps extends NavItemConfig {
  open: boolean;
}

export function SideMenuNavItem({
  icon: Icon,
  label,
  open,
  active,
  onClick,
}: SideMenuNavItemProps) {
  const theme = useTheme();

  const button = (
    <ListItem disablePadding sx={{ display: "block", mb: 0.4, px: 1.5 }}>
      <ListItemButton
        onClick={onClick}
        aria-current={active ? "page" : undefined}
        sx={{
          minHeight: 44,
          display: "flex",
          alignItems: "center",
          justifyContent: open ? "initial" : "center",
          px: 2,
          borderRadius: 1,
          backgroundColor: active ? "rgba(237, 33, 60, 0.08)" : "transparent",
          color: active
            ? theme.palette.primary.main
            : theme.palette.mode === "dark"
              ? "hsl(var(--sidebar-foreground))"
              : "#4D4D4D",
          "&:hover": {
            backgroundColor: active
              ? "rgba(237, 33, 60, 0.12)"
              : "rgba(237, 33, 60, 0.04)",
            color: theme.palette.primary.main,
          },
          transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
        }}
      >
        <ListItemIcon
          sx={{
            minWidth: 0,
            mr: open ? 2 : 0,
            justifyContent: "center",
            display: "flex",
            alignItems: "center",
            color: active ? theme.palette.primary.main : "inherit",
            transition: "margin 0.25s ease",
          }}
        >
          <Icon size={22} aria-hidden="true" />
        </ListItemIcon>
        <ListItemText
          primary={label}
          sx={{
            m: 0,
            opacity: open ? 1 : 0,
            transition: open
              ? "opacity 0.15s ease-in 0.18s"
              : "opacity 0.1s ease-out 0s",
            whiteSpace: "nowrap",
            pointerEvents: open ? "auto" : "none",
            overflow: "hidden",
            "& .MuiTypography-root": { fontSize: "0.9rem", lineHeight: 1 },
          }}
        />
      </ListItemButton>
    </ListItem>
  );

  if (open) return button;

  return (
    <Tooltip title={label} placement="right" arrow describeChild>
      {button}
    </Tooltip>
  );
}
