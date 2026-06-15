"use client";

import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";
import { ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip } from "@mui/material";

interface SideMenuThemeToggleProps {
  open: boolean;
}

export function SideMenuThemeToggle({ open }: SideMenuThemeToggleProps) {
  const { resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  if (!mounted) {
    return null; // avoid hydration mismatch
  }

  const isDark = resolvedTheme === "dark";

  return (
    <ListItem disablePadding sx={{ display: "block", mt: "auto", pb: 2 }}>
      <Tooltip title={isDark ? "Passer en mode clair" : "Passer en mode sombre"} placement="right" disableHoverListener={open}>
        <ListItemButton
          onClick={() => setTheme(isDark ? "light" : "dark")}
          sx={[
            {
              minHeight: 48,
              px: 2.5,
              mx: 2,
              borderRadius: "12px",
              color: "text.primary",
              transition: "all 0.2s ease-in-out",
              "&:hover": {
                bgcolor: "var(--color-surface-hover)",
              },
            },
            open ? { justifyContent: "initial" } : { justifyContent: "center", mx: "auto", width: 48 },
          ]}
        >
          <ListItemIcon
            sx={[
              { minWidth: 0, justifyContent: "center", color: "text.primary" },
              open ? { mr: 2 } : { mr: "auto" },
            ]}
          >
            {isDark ? <Moon size={20} /> : <Sun size={20} />}
          </ListItemIcon>
          <ListItemText
            primary={isDark ? "Light mode" : "Dark mode"}
            sx={[
              { margin: 0 },
              open ? { opacity: 1 } : { opacity: 0, display: "none" },
            ]}
            slotProps={{ primary: { sx: { fontSize: "14px", fontWeight: 600 } } }}
          />
        </ListItemButton>
      </Tooltip>
    </ListItem>
  );
}
