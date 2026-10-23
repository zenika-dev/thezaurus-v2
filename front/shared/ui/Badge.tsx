"use client";

import type { CSSProperties } from "react";

interface BadgeProps {
  color: string;
  bg: string;
  darkColor?: string;
  darkBg?: string;
  size?: "default" | "small";
  children: React.ReactNode;
}

export function Badge({ color, bg, darkColor, darkBg, size = "default", children }: BadgeProps) {
  const sizeClasses = size === "small" ? "text-xs px-1.5 py-0" : "text-xs px-2 py-0.5";

  return (
    <span
      className={`app-badge inline-flex items-center gap-1 rounded-2xl font-bold ${sizeClasses}`}
      style={{ "--badge-color": color, "--badge-bg": bg, "--badge-dark-color": darkColor ?? color, "--badge-dark-bg": darkBg ?? bg } as CSSProperties}
    >
      {children}
    </span>
  );
}
