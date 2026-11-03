import { createTheme } from "@mui/material/styles";
import type {} from "@mui/x-date-pickers/themeAugmentation";
import { cssTokens, designTokens as tokens } from "./design-tokens";

const selectedCalendarStyle = {
  "&:focus-visible": { outline: "3px solid var(--app-focus)", outlineOffset: 3 },
  "&.Mui-selected:not(.Mui-disabled), &.Mui-selected:not(.Mui-disabled):hover": {
    backgroundColor: tokens.primaryDark,
    color: "#fff",
  },
};

export function createAppTheme(mode: "light" | "dark") {
  const colors = tokens[mode];
  return createTheme({
    palette: {
      mode,
      primary: { main: tokens.primary, dark: tokens.primaryDark, contrastText: "#ffffff" },
      error: { main: colors.destructive },
      background: { default: colors.surfaceMuted, paper: colors.surface },
      text: { primary: colors.text, secondary: colors.textMuted },
    },
    typography: { fontFamily: tokens.font, button: { textTransform: "none", fontWeight: 700 } },
    shape: { borderRadius: tokens.radius },
    components: {
      MuiCssBaseline: { styleOverrides: {
        ":root": { ...cssTokens("light"), "--app-primary": tokens.primary, "--app-primary-dark": tokens.primaryDark,
          "--app-font": tokens.font, "--app-radius": `${tokens.radius}px`, "--app-shadow-card": tokens.shadowCard },
        ".dark": cssTokens("dark"),
        body: { color: "var(--app-text)", backgroundColor: "var(--app-surface-muted)" },
      } },
      MuiButtonBase: { styleOverrides: { root: {
        "&.Mui-focusVisible": { outline: "3px solid var(--app-focus)", outlineOffset: 3 },
      } } },
      MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: {
        root: { borderRadius: 20, minHeight: 40,
        "&.MuiButton-contained.MuiButton-colorPrimary": {
          "&:not(.Mui-disabled)": { background: tokens.primaryDark, color: "#fff" },
          "&.app-create-action:not(.Mui-disabled)": {
            backgroundImage: tokens.creationGradient,
            paddingTop: 8,
            paddingBottom: 8,
            gap: 8,
            transition: "transform 200ms cubic-bezier(0.4, 0, 0.2, 1)",
            "&:hover": { transform: "translateY(-2px)", boxShadow: "none" },
            "@media (prefers-reduced-motion: reduce)": {
              transition: "none",
              "&:hover": { transform: "none" },
            },
          },
          "&.Mui-disabled": { backgroundImage: "none", boxShadow: "none" },
        },
        "&.MuiButton-outlined.MuiButton-colorPrimary": {
          "&:not(.Mui-disabled)": { color: "var(--app-text)", borderColor: "var(--app-border-strong)" },
          "&:hover:not(.Mui-disabled)": { borderColor: tokens.primary, backgroundColor: "var(--app-surface-hover)" },
        },
        "&.MuiButton-text.MuiButton-colorPrimary:not(.Mui-disabled)": { color: "var(--app-focus)" },
        },
      } },
      MuiInputLabel: { styleOverrides: { root: {
        "&.Mui-focused:not(.Mui-error):not(.Mui-disabled)": { color: "var(--app-focus)" },
      } } },
      MuiTab: { styleOverrides: { root: {
        "&.Mui-selected:not(.Mui-disabled)": { color: "var(--app-focus)" },
      } } },
      MuiPickerDay: { styleOverrides: { root: selectedCalendarStyle } },
      MuiMonthCalendar: { styleOverrides: { button: selectedCalendarStyle } },
      MuiYearCalendar: { styleOverrides: { button: selectedCalendarStyle } },
      MuiSwitch: { styleOverrides: { switchBase: {
        "&.Mui-checked:not(.Mui-disabled)": { color: "#fff" },
        "&.Mui-checked:not(.Mui-disabled) + .MuiSwitch-track": { backgroundColor: tokens.primary, opacity: 1 },
        "&.Mui-focusVisible + .MuiSwitch-track": { outline: "3px solid var(--app-focus)", outlineOffset: 3, opacity: 1 },
      } } },
      MuiPaper: { styleOverrides: { root: { backgroundImage: "none" } } },
      MuiOutlinedInput: { styleOverrides: { root: {
        borderRadius: 12,
        "&:hover:not(.Mui-disabled):not(.Mui-error):not(.Mui-focused) .MuiOutlinedInput-notchedOutline": { borderColor: "var(--app-border-strong)" },
      }, input: { "&.MuiInputBase-inputMultiline": { resize: "vertical" } } } },
      MuiAutocomplete: { styleOverrides: { option: {
        "&.Mui-focused, &.Mui-focusVisible": { backgroundColor: "var(--app-surface-hover)" },
        '&[aria-selected="true"]': { backgroundColor: "var(--app-surface-hover)", fontWeight: 700 },
      } } },
      MuiDialog: { styleOverrides: { paper: {
        borderRadius: tokens.radius,
        "&.MuiDialog-paperFullScreen": { borderRadius: 0, height: "100dvh", maxHeight: "100dvh", margin: 0 },
      } } },
      MuiDialogTitle: { styleOverrides: { root: { overflowWrap: "anywhere", flexShrink: 0 } } },
      MuiDialogContent: { styleOverrides: { root: { minHeight: 0 } } },
      MuiDialogActions: { styleOverrides: { root: { flexShrink: 0, flexWrap: "wrap", gap: 8, padding: 16 } } },
      MuiTableCell: { styleOverrides: { root: { borderColor: "var(--app-border)" } } },
      MuiSkeleton: { styleOverrides: { root: { backgroundColor: "var(--app-surface-hover)" } } },
    },
  });
}

export default createAppTheme("light");
