"use client";

import type { ReactNode } from "react";
import Button, { type ButtonProps } from "@mui/material/Button";

export type ZenikaButtonVariant = "primary" | "secondary" | "outlined" | "ghost";

export interface ZenikaButtonProps extends Omit<ButtonProps, "variant"> {
  variant?: ZenikaButtonVariant;
  children?: ReactNode;
}

export function ZenikaButton({
  variant = "primary",
  className = "",
  sx,
  children,
  ...props
}: ZenikaButtonProps) {
  if (variant === "primary") {
    return (
      <Button
        {...props}
        variant="contained"
        color="primary"
        className={`app-create-action font-bold transition-all duration-200 active:scale-95 ${className}`}
        sx={[
          {
            borderRadius: "20px",
            textTransform: "none",
            fontWeight: 700,
          },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
        ]}
      >
        {children}
      </Button>
    );
  }

  if (variant === "ghost") {
    return (
      <Button
        {...props}
        variant="text"
        className={`font-medium transition-colors ${className}`}
        sx={[
          {
            borderRadius: "var(--app-radius, 12px)",
            textTransform: "none",
            color: "text.primary",
            "&:hover": {
              bgcolor: "action.hover",
            },
          },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
        ]}
      >
        {children}
      </Button>
    );
  }

  return (
    <Button
      {...props}
      variant={variant === "secondary" ? "contained" : "outlined"}
      color={variant === "secondary" ? "secondary" : "primary"}
      className={`font-semibold transition-colors ${className}`}
      sx={[
        {
          borderRadius: "var(--app-radius, 12px)",
          textTransform: "none",
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {children}
    </Button>
  );
}
