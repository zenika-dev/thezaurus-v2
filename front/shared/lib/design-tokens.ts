/** Shared values for MUI and the CSS variables emitted by CssBaseline. */
export const designTokens = {
  primary: "#ed213c",
  primaryDark: "#d51c35",
  creationGradient: "linear-gradient(135deg, #ed213c 0%, #bf1d67 100%)",
  font: 'var(--font-nunito), "Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
  radius: 16,
  shadowCard: "0px 2px 4px #00000026",
  light: {
    text: "#1e293b", textMuted: "#526174", textDisabled: "#64748b",
    surface: "#ffffff", surfaceMuted: "#f1f5f9", surfaceHover: "#e2e8f0",
    border: "#cbd5e1", borderStrong: "#64748b", destructive: "#b91c1c", focus: "#b31328",
  },
  dark: {
    text: "#f1f5f9", textMuted: "#b3b3b3", textDisabled: "#94a3b8",
    surface: "#121212", surfaceMuted: "#0a0a0a", surfaceHover: "#2d2d2d",
    border: "#475569", borderStrong: "#94a3b8", destructive: "#fca5a5", focus: "#ff6262",
  },
} as const;

export function cssTokens(mode: "light" | "dark") {
  return Object.fromEntries(Object.entries(designTokens[mode]).map(([key, value]) => [
    `--app-${key.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`, value,
  ]));
}
