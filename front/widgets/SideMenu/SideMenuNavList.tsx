"use client";

import { Box, List } from "@mui/material";
import { SideMenuNavItem, type NavItemConfig } from "./SideMenuNavItem";

interface SideMenuNavListProps {
  open: boolean;
  items: NavItemConfig[];
}

export function SideMenuNavList({ open, items }: SideMenuNavListProps) {
  return (
    <Box
      component="nav"
      aria-label="Navigation principale"
      sx={{ flexGrow: 1, overflowY: "auto", overflowX: "hidden", pt: 1 }}
    >
      <List>
        {items.map((item) => (
          <SideMenuNavItem key={item.label} open={open} {...item} />
        ))}
      </List>
    </Box>
  );
}
