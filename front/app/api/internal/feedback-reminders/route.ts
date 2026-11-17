import { NextRequest, NextResponse } from "next/server";

const API_BASE = process.env.API_URL ?? "http://localhost:8080";

/**
 * Point d'entrée public de Cloud Scheduler pour les rappels post-talk : seul le conteneur front
 * est exposé par Cloud Run, l'API (sidecar) ne l'est pas. Contrairement au proxy /api/backend,
 * on retransmet tel quel le token OIDC du job Scheduler ; l'API le vérifie (signature, audience,
 * compte de service appelant).
 */
export async function POST(req: NextRequest) {
  const authorization = req.headers.get("authorization");
  if (!authorization) {
    return new NextResponse(null, { status: 401 });
  }
  const res = await fetch(`${API_BASE}/api/internal/feedback-reminders`, {
    method: "POST",
    headers: { Authorization: authorization },
    cache: "no-store",
  });
  return new NextResponse(null, { status: res.status });
}
