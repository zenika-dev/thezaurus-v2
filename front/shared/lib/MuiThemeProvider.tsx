"use client";

import { useTheme as useNextTheme } from "next-themes";
import { ThemeProvider, createTheme, type Shadows } from "@mui/material/styles";
import { useMemo, useEffect, useState } from "react";
import CssBaseline from "@mui/material/CssBaseline";

const getDesignTokens = (mode: "light" | "dark") => ({
  palette: {
    mode,
    primary: { main: "#ed213c" },
    background: {
      default: mode === "light" ? "#f1f5f9" : "#0a0a0a",
      paper: mode === "light" ? "#ffffff" : "#121212",
    },
    text: {
      primary: mode === "light" ? "#1e293b" : "#f1f5f9",
      secondary: mode === "light" ? "#64748b" : "#94a3b8",
    },
  },
  typography: {
    fontFamily: '"Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
  },
  shape: { borderRadius: 16 },
  shadows: [
    "none",
    "2px 2px 3px #00000059",
    ...createTheme().shadows.slice(2),
  ] as Shadows,
  components: {
    MuiBackdrop: {
      styleOverrides: {
        root: {
          backgroundColor: "#000000bf",
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: { textTransform: "none" as const },
        contained: {
          color: "#FFFFFF",
          boxShadow: "2px 2px 3px #00000059",
          "&:hover": { boxShadow: "2px 2px 4px #00000066" },
        },
        outlined: {
          color: mode === "light" ? "#000000" : "#FFFFFF",
          "&:hover": { backgroundColor: "#ed213c66" },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { 
          boxShadow: "0px 2px 4px rgba(0, 0, 0, 0.15)",
          backgroundImage: "none"
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": { borderColor: "#ed213c" },
          "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: "#ed213c" },
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: { "&.Mui-focused": { color: "#ed213c" } },
      },
    },
  },
});

export function MuiThemeProvider({ children }: { children: React.ReactNode }) {
  const { resolvedTheme } = useNextTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const theme = useMemo(
    () => createTheme(getDesignTokens(mounted && resolvedTheme === "dark" ? "dark" : "light")),
    [resolvedTheme, mounted]
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
