"use client";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <p className="text-lg font-semibold text-text">Une erreur est survenue.</p>
      {error.digest && (
        <p className="text-xs text-text-disabled font-mono">#{error.digest}</p>
      )}
      <button
        onClick={reset}
        className="px-4 py-2 text-sm rounded-lg border border-border hover:bg-surface-muted transition-colors cursor-pointer"
      >
        Réessayer
      </button>
    </div>
  );
}
