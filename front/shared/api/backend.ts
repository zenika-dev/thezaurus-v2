const API_BASE = process.env.API_URL ?? "http://localhost:8080";

/**
 * Point de passage unique des appels à l'API Quarkus, qui propage l'identité de
 * l'utilisateur dans les deux contextes d'exécution :
 * - côté serveur (SSR, Server Actions) : appel direct à l'API avec les headers
 *   d'authentification de la requête entrante (cf. auth-headers) ;
 * - côté navigateur (refetch TanStack Query) : appel same-origin vers le proxy
 *   /api/backend, qui attache ces mêmes headers côté serveur — le navigateur n'a
 *   jamais besoin de connaître l'URL interne de l'API ni de porter un token.
 */
export async function apiFetch(path: string, init?: RequestInit): Promise<Response> {
  if (typeof window !== "undefined") {
    return fetch(`/api/backend${path}`, init);
  }

  // Imports dynamiques : next/headers est interdit dans le bundle client, et ce module
  // est aussi importé par les queryFn qui tournent dans le navigateur.
  const { headers, cookies } = await import("next/headers");
  const { buildAuthHeaders } = await import("./auth-headers");
  const incoming = await headers();
  const cookieStore = await cookies();
  const auth = await buildAuthHeaders(
    (name) => incoming.get(name),
    (name) => cookieStore.get(name)?.value,
  );
  return fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { ...(init?.headers ?? {}), ...auth },
  });
}
