"use client";

import { useTheme } from "next-themes";

interface BadgeProps {
  color: string;
  bg: string;
  darkColor?: string;
  darkBg?: string;
  size?: "default" | "small";
  children: React.ReactNode;
}

export function Badge({ color, bg, darkColor, darkBg, size = "default", children }: BadgeProps) {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";

  const appliedColor = isDark && darkColor ? darkColor : color;
  const appliedBg = isDark && darkBg ? darkBg : bg;

  const sizeClasses = size === "small" ? "text-[10px] px-1.5 py-0" : "text-xs px-2 py-0.5";

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-2xl font-bold border ${sizeClasses}`}
      style={{ color: appliedColor, backgroundColor: appliedBg, borderColor: appliedBg }}
    >
      {children}
    </span>
  );
}
