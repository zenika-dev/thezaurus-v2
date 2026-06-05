"use client";

import { useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { Calendar, CalendarDays, MicVocal, PenLine } from "lucide-react";
import { SideMenuView } from "./SideMenuView";
import { type NavItemConfig } from "./SideMenuNavItem";

const iconMap: Record<string, React.ElementType> = {
  "/":            Calendar,
  "/talks":       MicVocal,
  "/conferences": CalendarDays,
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
  const router = useRouter();
  const pathname = usePathname();

  const isActive = (path: string) =>
    path === "/" ? pathname === "/" : pathname.startsWith(path);

  const navItems: NavItemConfig[] = navLinks.map((link) => ({
    icon: iconMap[link.path] ?? Calendar,
    label: link.label,
    active: isActive(link.path),
    onClick: () => router.push(link.path),
  }));

  return (
    <SideMenuView
      open={open}
      onToggle={() => setOpen((o) => !o)}
      navItems={navItems}
    />
  );
}
