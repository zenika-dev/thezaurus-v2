"use client";

import { useTheme as useNextTheme } from "next-themes";
import { ThemeProvider } from "@mui/material/styles";
import { useEffect, useMemo, useState } from "react";
import CssBaseline from "@mui/material/CssBaseline";
import { createAppTheme } from "./theme";

export function MuiThemeProvider({ children }: { children: React.ReactNode }) {
  const { resolvedTheme } = useNextTheme();
  const [mounted, setMounted] = useState(false);
  useEffect(() => { queueMicrotask(() => setMounted(true)); }, []);
  const mode = mounted && resolvedTheme === "dark" ? "dark" : "light";
  const theme = useMemo(() => createAppTheme(mode), [mode]);
  return <ThemeProvider theme={theme}><CssBaseline />{children}</ThemeProvider>;
}
