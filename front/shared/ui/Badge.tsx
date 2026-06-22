"use client";

import { useTheme } from "next-themes";

interface BadgeProps {
  color: string;
  bg: string;
  darkColor?: string;
  darkBg?: string;
  children: React.ReactNode;
}

export function Badge({ color, bg, darkColor, darkBg, children }: BadgeProps) {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  const appliedColor = isDark && darkColor ? darkColor : color;
  const appliedBg = isDark && darkBg ? darkBg : bg;

  return (
    <span
      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-2xl text-xs font-bold border"
      style={{ color: appliedColor, backgroundColor: appliedBg, borderColor: appliedBg }}
    >
      {children}
    </span>
  );
}
