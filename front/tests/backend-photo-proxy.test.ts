// @vitest-environment node
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";
import { GET, PUT, DELETE } from "@/app/api/backend/[...path]/route";

vi.mock("@/shared/api/auth-headers", () => ({ buildAuthHeaders: vi.fn().mockResolvedValue({ Authorization: "Bearer test" }) }));
beforeEach(() => vi.stubEnv("NEXTAUTH_URL", "http://localhost"));
afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); });
const context = { params: Promise.resolve({ path: ["api", "me", "profile", "photo"] }) };

it("preserves binary uploads and photo responses byte for byte", async () => {
  const bytes = new Uint8Array([137, 80, 78, 71, 255, 0, 128]);
  const fetchMock = vi.fn().mockResolvedValue(new Response(bytes, { headers: { "Content-Type": "image/png", "Cache-Control": "private, no-store" } }));
  vi.stubGlobal("fetch", fetchMock);
  const req = new NextRequest("http://localhost/api/backend/api/me/profile/photo", {
    method: "PUT", headers: { "Content-Type": "image/png", Origin: "http://localhost" }, body: bytes,
  });
  const result = await PUT(req, context);
  expect(fetchMock.mock.calls[0][1].body).toEqual(bytes);
  expect(new Uint8Array(await result.arrayBuffer())).toEqual(bytes);
  expect(result.headers.get("cache-control")).toBe("private, no-store");
});

it("handles empty reset responses without introducing a response body", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
  const result = await DELETE(new NextRequest("http://localhost/api/backend/api/me/profile/photo", { method: "DELETE" }), context);
  expect(result.status).toBe(204);
  expect(result.body).toBeNull();
});

it("uses the configured public origin behind a reverse proxy", async () => {
  vi.stubEnv("NEXTAUTH_URL", "https://thezaurus.example.com");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
  const request = new NextRequest("http://localhost:3000/api/backend/api/me/profile/photo", {
    method: "DELETE", headers: { Origin: "https://thezaurus.example.com" },
  });
  expect((await DELETE(request, context)).status).toBe(204);
});

it("rejects cross-origin changes and oversized bodies before forwarding", async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  const foreign = new NextRequest("http://localhost/api/backend/api/me/profile/photo", { method: "PUT", headers: { Origin: "https://foreign.test" }, body: "bad" });
  expect((await PUT(foreign, context)).status).toBe(403);
  const oversized = new NextRequest("http://localhost/api/backend/api/me/profile/photo", { method: "PUT", body: new Uint8Array(6 * 1024 * 1024 + 1) });
  expect((await PUT(oversized, context)).status).toBe(413);
  expect(fetchMock).not.toHaveBeenCalled();
});

it("forwards authenticated image reads", async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 404 }));
  vi.stubGlobal("fetch", fetchMock);
  const result = await GET(new NextRequest("http://localhost/api/backend/api/users/jane/photo"), {
    params: Promise.resolve({ path: ["api", "users", "jane", "photo"] }),
  });
  expect(result.status).toBe(404);
  expect(fetchMock).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({ headers: { Authorization: "Bearer test" }, cache: "no-store" }));
});
