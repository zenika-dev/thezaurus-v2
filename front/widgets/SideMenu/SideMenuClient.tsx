"use client";

import { useState } from "react";
import { usePathname } from "next/navigation";
import { BarChart3, Calendar, CalendarDays, MicVocal, PenLine } from "lucide-react";
import { SideMenuView } from "./SideMenuView";
import { type NavItemConfig } from "./SideMenuNavItem";

const iconMap: Record<string, React.ElementType> = {
  "/":            Calendar,
  "/talks":       MicVocal,
  "/conferences": CalendarDays,
  "/conferences/statistics": BarChart3,
  "/blog-posts":  PenLine,
};

interface NavLink {
  label: string;
  path: string;
}

interface SideMenuClientProps {
  navLinks: NavLink[];
}

export function SideMenuClient({ navLinks }: SideMenuClientProps) {
  const [open, setOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const pathname = usePathname();

  const activeLink = navLinks
    .filter(({ path }) => path === "/" ? pathname === "/" : pathname === path || pathname.startsWith(`${path}/`))
    .sort((left, right) => right.path.length - left.path.length)[0];

  const navItems: NavItemConfig[] = navLinks.map((link) => ({
    icon: iconMap[link.path] ?? Calendar,
    label: link.label,
    active: activeLink?.path === link.path,
    href: link.path,
    onClick: () => setMobileOpen(false),
  }));

  return (
    <SideMenuView
      open={open}
      mobileOpen={mobileOpen}
      onMobileToggle={() => setMobileOpen((value) => !value)}
      onMobileClose={() => setMobileOpen(false)}
      onToggle={() => setOpen((o) => !o)}
      navItems={navItems}
    />
  );
}
