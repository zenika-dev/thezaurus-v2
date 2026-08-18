"use client";

import { Suspense } from "react";
import { signOut, useSession } from "next-auth/react";
import { useSearchParams } from "next/navigation";
import { Role } from "./config";
import { LandingPage } from "./landing-page";

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

  if (status === "loading") {
    return <div className="p-4 text-center">Chargement de la session...</div>;
  }

  if (status === "unauthenticated" || !session?.user) {
    return (
      <LandingPage
        error={
          authError
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
      <div className="p-4 text-center">
        <p className="text-red-500">
          Vous n&apos;avez pas les droits nécessaires pour accéder à cette page.
        </p>
        <button
          type="button"
          onClick={() => signOut()}
          className="mt-4 rounded bg-gray-200 px-4 py-2 text-sm hover:bg-gray-300"
        >
          Se reconnecter
        </button>
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
        <div className="p-4 text-center">Chargement de la session...</div>
      }
    >
      <ProtectedRouteContent {...props} />
    </Suspense>
  );
}
