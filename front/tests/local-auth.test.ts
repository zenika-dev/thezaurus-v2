import { afterEach, describe, expect, it, vi } from "vitest";
import { authorizeLocalUser, isLocalAuthEnabled } from "@/features/auth/local-auth";

afterEach(() => { vi.unstubAllEnvs(); vi.unstubAllGlobals(); });
describe("local authentication boundary", () => {
  it.each(["production", "test"])("rejects %s even with explicit opt-in", (mode) => {
    vi.stubEnv("NODE_ENV", mode);
    vi.stubEnv("LOCAL_AUTH_ENABLED", "true");
    expect(isLocalAuthEnabled()).toBe(false);
  });
  it("requires explicit opt-in and a loopback API", () => {
    vi.stubEnv("NODE_ENV", "development");
    vi.stubEnv("LOCAL_AUTH_ENABLED", "false");
    expect(isLocalAuthEnabled()).toBe(false);
    vi.stubEnv("LOCAL_AUTH_ENABLED", "true");
    vi.stubEnv("API_URL", "https://example.com");
    expect(isLocalAuthEnabled()).toBe(false);
    vi.stubEnv("API_URL", "http://127.0.0.1:8088");
    expect(isLocalAuthEnabled()).toBe(true);
  });
  it.each(["http://localhost:8088", "http://127.0.0.1:8088", "http://[::1]:8088"])("accepts loopback %s", (url) => {
    vi.stubEnv("NODE_ENV", "development");
    vi.stubEnv("LOCAL_AUTH_ENABLED", "true");
    vi.stubEnv("API_URL", url);
    expect(isLocalAuthEnabled()).toBe(true);
  });
  it.each(["invalid", "https://localhost:8088", "http://host.docker.internal:8088", "http://localhost.example.com", "http://user:password@localhost", "http://localhost/path", "http://localhost?target=external"])("rejects unsafe API %s", (url) => {
    vi.stubEnv("NODE_ENV", "development");
    vi.stubEnv("LOCAL_AUTH_ENABLED", "true");
    vi.stubEnv("API_URL", url);
    expect(isLocalAuthEnabled()).toBe(false);
  });
});

describe("local API identity", () => {
  function enable() {
    vi.stubEnv("NODE_ENV", "development");
    vi.stubEnv("LOCAL_AUTH_ENABLED", "true");
    vi.stubEnv("API_URL", "http://localhost:8088");
  }

  it("uses only the mock identity and API roles, without Google tokens", async () => {
    enable();
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ email: "dev@zenika.com", roles: ["CONSULTANT", "DT"] }) });
    vi.stubGlobal("fetch", fetchMock);
    expect(await authorizeLocalUser()).toEqual({ id: "dev@zenika.com", email: "dev@zenika.com", name: "Développement local", roles: ["CONSULTANT", "DT"] });
    expect(fetchMock).toHaveBeenCalledWith(new URL("http://localhost:8088/api/me"), expect.objectContaining({ redirect: "error", cache: "no-store" }));
  });

  it.each([
    { email: "someone@zenika.com", roles: ["ADMIN"] },
    { email: "dev@zenika.com", roles: "ADMIN" },
    { email: "dev@zenika.com", roles: ["UNKNOWN"] },
    { email: "dev@zenika.com" }, null,
  ])("rejects unexpected identity or malformed roles: %j", async (body) => {
    enable();
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, json: async () => body }));
    expect(await authorizeLocalUser()).toBeNull();
  });

  it("fails closed on HTTP errors, network errors and invalid JSON", async () => {
    enable();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: false })
      .mockRejectedValueOnce(new Error("unavailable or redirect"))
      .mockResolvedValueOnce({ ok: true, json: async () => { throw new Error("invalid JSON"); } });
    vi.stubGlobal("fetch", fetchMock);
    for (let attempt = 0; attempt < 3; attempt++) expect(await authorizeLocalUser()).toBeNull();
  });

  it("rechecks the guard at authorization time without calling the API", async () => {
    enable();
    vi.stubEnv("LOCAL_AUTH_ENABLED", "false");
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    expect(await authorizeLocalUser()).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
