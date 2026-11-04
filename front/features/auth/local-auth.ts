import { Role } from "@/shared/api/enums";

/** Opt-in local authentication is never available in a production build. */
export function isLocalAuthEnabled() {
  if (process.env.NODE_ENV !== "development" || process.env.LOCAL_AUTH_ENABLED !== "true") return false;
  try {
    const api = new URL(process.env.API_URL ?? "http://localhost:8080");
    return api.protocol === "http:" && ["localhost", "127.0.0.1", "[::1]"].includes(api.hostname)
      && !api.username && !api.password && api.pathname === "/" && !api.search && !api.hash;
  } catch {
    return false;
  }
}

/** The mock API owns the identity and roles; no credentials are accepted from the browser. */
export async function authorizeLocalUser() {
  if (!isLocalAuthEnabled()) return null;
  try {
    const res = await fetch(new URL("/api/me", process.env.API_URL ?? "http://localhost:8080"), {
      signal: AbortSignal.timeout(5000),
      redirect: "error",
      cache: "no-store",
    });
    if (!res.ok) return null;
    const user = await res.json();
    if (user?.email !== "dev@zenika.com" || !Array.isArray(user.roles)
      || !user.roles.every((role: unknown) => Role.some((allowed) => allowed === role))) return null;
    return { id: user.email, email: user.email, name: "Développement local", roles: user.roles as Role[] };
  } catch {
    return null;
  }
}
