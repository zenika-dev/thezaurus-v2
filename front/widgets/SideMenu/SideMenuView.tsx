"use client";

import { Box, Drawer, IconButton, Divider, List, Typography, useMediaQuery } from "@mui/material";
import { Menu, X } from "lucide-react";
import { SideMenuHeader } from "./SideMenuHeader";
import { SideMenuNavList } from "./SideMenuNavList";
import { type NavItemConfig } from "./SideMenuNavItem";
import { SideMenuProfileLink } from "./SideMenuProfileLink";
import { SideMenuAdminLink } from "./SideMenuAdminLink";
import { SideMenuThemeToggle } from "./SideMenuThemeToggle";

export interface SideMenuViewProps {
  open: boolean;
  onToggle: () => void;
  mobileOpen: boolean;
  onMobileToggle: () => void;
  onMobileClose: () => void;
  navItems: NavItemConfig[];
}

export function SideMenuView({ open, onToggle, mobileOpen, onMobileToggle, onMobileClose, navItems }: SideMenuViewProps) {
  const mobile = useMediaQuery("(max-width: 767px)");
  const content = (expanded: boolean, temporary: boolean) => (
    <>
      {temporary ? (
        <Box sx={{ height: 64, px: 2, display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: 1, borderColor: "divider" }}>
          <Typography sx={{ fontWeight: 800 }}>Thezaurus</Typography>
          <IconButton aria-label="Fermer la navigation" onClick={onMobileClose}><X size={22} /></IconButton>
        </Box>
      ) : <SideMenuHeader open={expanded} onToggle={onToggle} />}
      <SideMenuNavList open={expanded} items={navItems} />
      <Divider sx={{ mx: 2 }} />
      <List sx={{ py: 0.5 }}>
        <SideMenuProfileLink open={expanded} onNavigate={onMobileClose} />
        <SideMenuAdminLink open={expanded} onNavigate={onMobileClose} />
      </List>
      <List disablePadding><SideMenuThemeToggle open={expanded} /></List>
    </>
  );
  return (
    <>
      <Box component="header" sx={{ "@media (min-width: 768px)": { display: "none" }, height: 64, flexShrink: 0, display: "flex", alignItems: "center", gap: 1, px: 2, bgcolor: "background.paper", borderBottom: 1, borderColor: "divider" }}>
        <IconButton aria-label="Ouvrir la navigation" aria-expanded={mobileOpen} aria-controls={mobileOpen ? "mobile-navigation" : undefined} onClick={onMobileToggle}><Menu size={24} /></IconButton>
        <Typography sx={{ fontWeight: 800 }}>Thezaurus</Typography>
      </Box>
      <Drawer variant="temporary" open={mobile && mobileOpen} onClose={onMobileClose} slotProps={{ paper: { id: "mobile-navigation", "aria-label": "Navigation latérale", sx: { width: 280, maxWidth: "calc(100vw - 32px)", borderRadius: 0 } } }}>
        {content(true, true)}
      </Drawer>
      <Drawer variant="permanent" open={open} sx={{ display: "none", "@media (min-width: 768px)": { display: "block" }, width: open ? 255 : 88, flexShrink: 0, transition: "width 200ms", "& .MuiDrawer-paper": { width: open ? 255 : 88, transition: "width 200ms", overflow: "visible", borderRadius: 0, boxShadow: "none", borderRight: 1, borderColor: "divider" } }}>
        {content(open, false)}
      </Drawer>
    </>
  );
}
