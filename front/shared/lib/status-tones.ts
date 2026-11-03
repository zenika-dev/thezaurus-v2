/** Shared badge tones; domain models retain the status-to-tone associations. */
export const statusTones = {
  neutral: { text: "#334155", bg: "#f1f5f9", darkText: "#e2e8f0", darkBg: "#334155" },
  idea: { text: "#9a0530", bg: "#ffedd4", darkText: "#ffdd58", darkBg: "#7e2a0c" },
  info: { text: "#0132d1", bg: "#dbeafe", darkText: "#94e5ff", darkBg: "#1c398e" },
  success: { text: "#245e12", bg: "#dcfce7", darkText: "#47ffb4", darkBg: "#0d542b" },
  danger: { text: "#9a0530", bg: "#ffe4e6", darkText: "#ffb3ba", darkBg: "#7e1a2c" },
  replayed: { text: "#681ac1", bg: "#f3e8ff", darkText: "#ffb9ff", darkBg: "#59168b" },
} as const;

/** Dedicated tone palette for audience visibility levels (internal / external) */
export const visibilityTones = {
  internal: { text: "#9a0530", bg: "#ffe4e6", darkText: "#ffb3ba", darkBg: "#7e1a2c" },
  external: { text: "#245e12", bg: "#dcfce7", darkText: "#47ffb4", darkBg: "#0d542b" },
} as const;
