"use client";

import { ErrorBoundary } from "react-error-boundary";
import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import type { ReactNode } from "react";

function ErrorFallback({ resetErrorBoundary }: { resetErrorBoundary: () => void }) {
  return (
    <Alert
      severity="error"
      action={
        <Button size="small" color="inherit" onClick={resetErrorBoundary}>
          Réessayer
        </Button>
      }
      sx={{ m: 2 }}
    >
      Erreur lors du chargement des données.
    </Alert>
  );
}

export function DataErrorBoundary({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      {children}
    </ErrorBoundary>
  );
}
