import type { Route } from "./+types/api.$";

export async function loader({ request }: Route.LoaderArgs) {
  return proxyRequest(request);
}

export async function action({ request }: Route.ActionArgs) {
  return proxyRequest(request);
}

async function proxyRequest(request: Request) {
  const url = new URL(request.url);
  const targetBaseUrl = process.env.API_URL || "http://localhost:8080";
  
  // Construit l'URL cible (ex: http://api:8080/api/me)
  const targetUrl = `${targetBaseUrl}${url.pathname}${url.search}`;

  const headers = new Headers(request.headers);
  // On retire le host pour éviter les conflits
  headers.delete("host");

  try {
    const response = await fetch(targetUrl, {
      method: request.method,
      headers: headers,
      body: request.method !== "GET" && request.method !== "HEAD" ? await request.arrayBuffer() : undefined,
      redirect: "manual",
    });

    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    });
  } catch (err) {
    console.error("Erreur Proxy API:", err);
    return new Response("Erreur de communication avec l'API", { status: 502 });
  }
}
