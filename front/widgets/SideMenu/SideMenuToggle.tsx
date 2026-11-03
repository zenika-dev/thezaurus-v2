"use client";

import { IconButton } from "@mui/material";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface SideMenuToggleProps { open: boolean; onToggle: () => void; }

export function SideMenuToggle({ open, onToggle }: SideMenuToggleProps) {
  return (
    <IconButton onClick={onToggle} aria-expanded={open} aria-label={open ? "Réduire la navigation" : "Développer la navigation"}
      sx={{ width: 44, height: 44, bgcolor: "background.paper", color: "text.secondary", position: "absolute", right: open ? 8 : -22, top: "50%", transform: "translateY(-50%)", zIndex: 10, border: 1, borderColor: "divider", "&:hover": { bgcolor: "action.hover", color: "primary.main" } }}>
      {open ? <ChevronLeft size={20} /> : <ChevronRight size={20} />}
    </IconButton>
  );
}
