import { NextRequest, NextResponse } from "next/server";
import { buildAuthHeaders } from "@/shared/api/auth-headers";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";
const MAX_BODY_BYTES = 6 * 1024 * 1024;

async function readBody(req: NextRequest): Promise<Uint8Array<ArrayBuffer> | null> {
  if (!req.body) return null;
  const reader = req.body.getReader();
  const chunks: Uint8Array[] = [];
  let size = 0;
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > MAX_BODY_BYTES) {
      await reader.cancel();
      throw new RangeError("Request body too large");
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return bytes;
}

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

  const headers: Record<string, string> = {
    ...await buildAuthHeaders(
      (name) => req.headers.get(name),
      (name) => req.cookies.get(name)?.value,
    ),
  };
  const contentType = req.headers.get("content-type");
  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  if (req.method !== "GET" && req.method !== "HEAD") {
    const origin = req.headers.get("origin");
    const expectedOrigin = new URL(process.env.NEXTAUTH_URL ?? req.url).origin;
    if (origin && origin !== expectedOrigin) {
      return new NextResponse(null, { status: 403 });
    }
  }

  let requestBody: Uint8Array<ArrayBuffer> | null = null;
  if (req.method !== "GET" && req.method !== "HEAD") {
    try {
      requestBody = await readBody(req);
    } catch (error) {
      if (error instanceof RangeError) return new NextResponse(null, { status: 413 });
      throw error;
    }
  }

  const res = await fetch(url, {
    method: req.method,
    headers,
    body: requestBody,
    cache: "no-store",
  });

  return new NextResponse(res.status === 204 || res.status === 304 ? null : res.body, {
    status: res.status,
    headers: {
      "Content-Type": res.headers.get("content-type") ?? "application/json",
      "Cache-Control": res.headers.get("cache-control") ?? "no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}

export { forward as GET, forward as POST, forward as PUT, forward as DELETE };
