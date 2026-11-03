"use client";

import Button from "@mui/material/Button";

export function RouteError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <div role="alert" className="p-4 sm:p-6 flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center">
    <p className="text-lg font-semibold text-text">Une erreur est survenue.</p>
    {error.digest && <p className="text-xs text-text-muted font-mono">#{error.digest}</p>}
    <Button variant="outlined" onClick={reset}>Réessayer</Button>
  </div>;
}
