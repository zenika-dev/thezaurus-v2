"use client";

import { Drawer, styled } from "@mui/material";
import Divider from "@mui/material/Divider";
import List from "@mui/material/List";
import { SideMenuHeader } from "./SideMenuHeader";
import { SideMenuNavList } from "./SideMenuNavList";
import { type NavItemConfig } from "./SideMenuNavItem";
import { SideMenuProfileLink } from "./SideMenuProfileLink";
import { SideMenuAdminLink } from "./SideMenuAdminLink";
import { SideMenuThemeToggle } from "./SideMenuThemeToggle";

const DRAWER_WIDTH = 255;
const COLLAPSED_WIDTH = 88;

const StyledDrawer = styled(Drawer, { shouldForwardProp: (p) => p !== "open" })(
  ({ theme, open }) => ({
    width: open ? DRAWER_WIDTH : COLLAPSED_WIDTH,
    flexShrink: 0,
    whiteSpace: "nowrap",
    boxSizing: "border-box",
    transition: theme.transitions.create("width", {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
    "& .MuiDrawer-paper": {
      width: open ? DRAWER_WIDTH : COLLAPSED_WIDTH,
      display: "flex",
      flexDirection: "column",
      transition: theme.transitions.create(["width", "background-color"], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
      }),
      overflow: "visible",
      borderRight: "1px solid var(--color-border)",
      background: theme.palette.background.paper,
      boxShadow: "none",
    },
  }),
);

export interface SideMenuViewProps {
  open: boolean;
  onToggle: () => void;
  navItems: NavItemConfig[];
}

export function SideMenuView({ open, onToggle, navItems }: SideMenuViewProps) {
  return (
    <StyledDrawer
      variant="permanent"
      open={open}
      slots={{ docked: "aside" }}
      slotProps={{
        docked: { "aria-label": "Navigation latérale" } as React.HTMLAttributes<HTMLElement>,
      }}
    >
      <SideMenuHeader open={open} onToggle={onToggle} />
      <SideMenuNavList open={open} items={navItems} />
      <Divider sx={{ mx: 2, borderColor: "var(--color-border)" }} />
      <List sx={{ py: 0.5 }}>
        <SideMenuProfileLink open={open} />
        <SideMenuAdminLink open={open} />
      </List>
      <SideMenuThemeToggle open={open} />
    </StyledDrawer>
  );
}
