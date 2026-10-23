"use client";

import { Alert, Button } from "@mui/material";
import { Suspense, useEffect } from "react";
import { signOut, useSession } from "next-auth/react";
import { useSearchParams } from "next/navigation";
import type { Role } from "@/shared/api";
import { LandingPage } from "./landing-page";
import { AuthError } from "./types";

function ProtectedRouteContent({
  children,
  allowedRoles,
}: {
  children: React.ReactNode;
  allowedRoles: Role[];
}) {
  const { data: session, status } = useSession();
  const searchParams = useSearchParams();
  const authError = searchParams.get("error");

  const isSessionExpired = session?.error === AuthError.REFRESH_ACCESS_TOKEN_ERROR;

  useEffect(() => {
    if (isSessionExpired) {
      signOut({ redirect: false });
    }
  }, [isSessionExpired]);

  if (status === "loading") {
    return <div className="p-4 text-center text-text w-full">Chargement de la session...</div>;
  }

  if (status === "unauthenticated" || !session?.user || isSessionExpired) {
    return (
      <LandingPage
        error={
          isSessionExpired
            ? "Votre session a expiré. Merci de vous reconnecter."
            : authError
            ? "Une erreur est survenue lors de la connexion. Merci de réessayer."
            : null
        }
      />
    );
  }

  const hasRole = allowedRoles.some((role) =>
    (session.user.roles ?? []).includes(role),
  );
  if (!hasRole) {
    return (
      <div className="p-4 text-center text-text w-full">
        <Alert severity="error">
          Vous n&apos;avez pas les droits nécessaires pour accéder à cette page.
        </Alert>
        <Button variant="contained"
          type="button"
          onClick={() => signOut()}
          sx={{ mt: 2 }}
        >
          Se reconnecter
        </Button>
      </div>
    );
  }

  return <>{children}</>;
}

export function ProtectedRoute(props: {
  children: React.ReactNode;
  allowedRoles: Role[];
}) {
  return (
    <Suspense
      fallback={
        <div className="p-4 text-center text-text w-full">Chargement de la session...</div>
      }
    >
      <ProtectedRouteContent {...props} />
    </Suspense>
  );
}
