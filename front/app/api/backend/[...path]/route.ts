import { NextRequest, NextResponse } from "next/server";
import { buildAuthHeaders } from "@/shared/api/auth-headers";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";

/**
 * Proxy same-origin vers l'API Quarkus pour les fetches déclenchés dans le navigateur
 * (refetch TanStack Query après invalidation). Le navigateur ne porte que son cookie de
 * session NextAuth (et le cookie IAP en production) : c'est ici, côté serveur, que
 * l'identité est traduite en headers compréhensibles par l'API (cf. auth-headers).
 */
async function forward(
  req: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  const { path } = await params;
  const url = `${API_BASE}/${path.join("/")}${req.nextUrl.search}`;

  const headers: Record<string, string> = await buildAuthHeaders(
    (name) => req.headers.get(name),
    (name) => req.cookies.get(name)?.value,
  );
  const contentType = req.headers.get("content-type");
  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  const res = await fetch(url, {
    method: req.method,
    headers,
    body: req.method === "GET" || req.method === "HEAD" ? undefined : await req.text(),
  });

  const body = await res.text();
  return new NextResponse(body.length > 0 ? body : null, {
    status: res.status,
    headers: {
      "Content-Type": res.headers.get("content-type") ?? "application/json",
    },
  });
}

export { forward as GET, forward as POST, forward as PUT, forward as DELETE };
