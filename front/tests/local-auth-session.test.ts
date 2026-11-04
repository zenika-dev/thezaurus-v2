import { afterEach, describe, expect, it, vi } from "vitest";
import type { Account, Session, User } from "next-auth";
import type { JWT } from "next-auth/jwt";
import type { AdapterUser } from "next-auth/adapters";
import { AuthError } from "@/features/auth/types";

afterEach(() => { vi.unstubAllEnvs(); vi.resetModules(); });

async function options(mode = "development", enabled = "true") {
  vi.stubEnv("NODE_ENV", mode);
  vi.stubEnv("LOCAL_AUTH_ENABLED", enabled);
  vi.stubEnv("API_URL", "http://localhost:8088");
  vi.resetModules();
  return (await import("@/features/auth/config")).authOptions;
}

const user: User & { roles: ["CONSULTANT"] } = { id: "dev@zenika.com", email: "dev@zenika.com", roles: ["CONSULTANT"] };
const account: Account = { provider: "local", type: "credentials", providerAccountId: user.id };

describe("local NextAuth session", () => {
  it.each([["production", "true"], ["test", "true"], ["development", "false"]])("does not register a local provider in %s with flag %s", async (mode, enabled) => {
    const auth = await options(mode, enabled);
    expect(auth.providers.map(p => p.id)).toEqual(["google"]);
  });

  it("registers a credentials provider and keeps a normal JWT session", async () => {
    const auth = await options();
    expect(auth.providers.find(p => p.options?.id === "local")?.type).toBe("credentials");
    expect(auth.session?.strategy).toBe("jwt");
    const token = await auth.callbacks!.jwt!({ token: {}, user, account });
    expect(token).toMatchObject({ localAuth: true, roles: ["CONSULTANT"] });
    expect(token.idToken).toBeUndefined();
    const renewed = await auth.callbacks!.jwt!({ token, user, account: null });
    expect(renewed).toEqual(token);
    const session: Session = { user: { email: user.email, roles: [] }, expires: "2099-01-01" };
    const adapterUser: AdapterUser = { ...user, email: user.email!, emailVerified: null };
    const result = await auth.callbacks!.session!({ session, token, user: adapterUser, newSession: undefined, trigger: "update" }) as Session;
    expect(result.user?.roles).toEqual(["CONSULTANT"]);
  });

  it.each(["production", "development"])("invalidates an existing local token when disabled in %s", async mode => {
    const auth = await options(mode, "false");
    const token: JWT = { localAuth: true, roles: ["ADMIN"], email: user.email };
    const result = await auth.callbacks!.jwt!({ token, user, account: null });
    expect(result.roles).toEqual([]);
    expect(result.error).toBe(AuthError.REFRESH_ACCESS_TOKEN_ERROR);
  });

  it("rejects a local sign-in if the flag was removed after registration", async () => {
    const auth = await options();
    vi.stubEnv("LOCAL_AUTH_ENABLED", "false");
    const token = await auth.callbacks!.jwt!({ token: {}, user, account });
    expect(token.roles).toEqual([]);
    expect(token.error).toBe(AuthError.REFRESH_ACCESS_TOKEN_ERROR);
  });
});
