import { decode } from "next-auth/jwt";

/**
 * Construit les headers d'authentification à propager vers l'API Quarkus. Deux chemins,
 * alignés sur la config JWT du backend :
 * - production derrière IAP : l'assertion `x-goog-iap-jwt-assertion` de la requête entrante
 *   est retransmise telle quelle (IAP la rafraîchit à chaque requête, rien à gérer) ;
 * - dev : l'id_token Google persisté dans le JWT NextAuth part en `Authorization: Bearer`.
 *
 * Fonction pure vis-à-vis de Next : la requête entrante est fournie par l'appelant
 * (NextRequest côté route handler, next/headers côté apiFetch), ce qui permet de garder ce
 * module hors du graphe client sans dépendre de next/headers.
 */
export async function buildAuthHeaders(
  getHeader: (name: string) => string | null,
  getCookie: (name: string) => string | undefined,
): Promise<Record<string, string>> {
  const iapAssertion = getHeader("x-goog-iap-jwt-assertion");
  if (iapAssertion) {
    return { "x-goog-iap-jwt-assertion": iapAssertion };
  }

  const sessionCookie =
    getCookie("__Secure-next-auth.session-token") ??
    getCookie("next-auth.session-token");
  if (!sessionCookie) {
    return {};
  }

  const token = await decode({
    token: sessionCookie,
    secret: process.env.NEXTAUTH_SECRET as string,
  });
  return typeof token?.idToken === "string"
    ? { Authorization: `Bearer ${token.idToken}` }
    : {};
}
