import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import type { Session, User } from "next-auth";
import type { JWT } from "next-auth/jwt";
import type { AdapterUser } from "next-auth/adapters";
import { authOptions, AuthError, ProtectedRoute } from "@/features/auth";
import * as nextAuthReact from "next-auth/react";

vi.mock("next-auth/react", () => ({
  useSession: vi.fn(),
  signOut: vi.fn(),
  signIn: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(),
}));

const mockUser: User = { id: "user-1", name: "Test User", email: "test@zenika.com" };

describe("Auth Session & Token Refresh", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("authOptions.callbacks.jwt", () => {
    it("garde le token intact s'il n'est pas encore expiré", async () => {
      const nowInSeconds = Math.floor(Date.now() / 1000);
      const token: JWT = {
        idToken: "valid-id-token",
        refreshToken: "refresh-token",
        expiresAt: nowInSeconds + 3600,
      };

      const result = await authOptions.callbacks!.jwt!({
        token,
        user: mockUser,
        account: null,
      });

      expect(result).toEqual(token);
    });

    it("renouvelle le token Google s'il a expiré et qu'un refresh_token est présent", async () => {
      process.env.GOOGLE_CLIENT_ID = "test-client-id";
      process.env.GOOGLE_CLIENT_SECRET = "test-client-secret";

      const nowInSeconds = Math.floor(Date.now() / 1000);
      const token: JWT = {
        idToken: "expired-id-token",
        refreshToken: "valid-refresh-token",
        expiresAt: nowInSeconds - 10,
      };

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          id_token: "new-id-token",
          expires_in: 3600,
          refresh_token: "new-refresh-token",
        }),
      });
      global.fetch = mockFetch;

      const result = await authOptions.callbacks!.jwt!({
        token,
        user: mockUser,
        account: null,
      });

      expect(mockFetch).toHaveBeenCalledWith(
        "https://oauth2.googleapis.com/token",
        expect.objectContaining({
          method: "POST",
        }),
      );
      expect(result.idToken).toBe("new-id-token");
      expect(result.refreshToken).toBe("new-refresh-token");
      expect(result.error).toBeUndefined();
    });

    it("définit AuthError.REFRESH_ACCESS_TOKEN_ERROR si le refresh Google échoue", async () => {
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      process.env.GOOGLE_CLIENT_ID = "test-client-id";
      process.env.GOOGLE_CLIENT_SECRET = "test-client-secret";

      const nowInSeconds = Math.floor(Date.now() / 1000);
      const token: JWT = {
        idToken: "expired-id-token",
        refreshToken: "invalid-refresh-token",
        expiresAt: nowInSeconds - 10,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        json: async () => ({ error: "invalid_grant" }),
      });

      const result = await authOptions.callbacks!.jwt!({
        token,
        user: mockUser,
        account: null,
      });

      expect(result.error).toBe(AuthError.REFRESH_ACCESS_TOKEN_ERROR);
      consoleErrorSpy.mockRestore();
    });

    it("définit AuthError.REFRESH_ACCESS_TOKEN_ERROR si expiré sans refresh token", async () => {
      const nowInSeconds = Math.floor(Date.now() / 1000);
      const token: JWT = {
        idToken: "expired-id-token",
        expiresAt: nowInSeconds - 10,
      };

      const result = await authOptions.callbacks!.jwt!({
        token,
        user: mockUser,
        account: null,
      });

      expect(result.error).toBe(AuthError.REFRESH_ACCESS_TOKEN_ERROR);
    });
  });

  describe("authOptions.callbacks.session", () => {
    it("propage l'erreur d'authentification vers la session", async () => {
      const session: Session = {
        user: { name: "Test User", email: "test@zenika.com", roles: [] },
        expires: "2099-01-01",
      };
      const token: JWT = {
        roles: ["CONSULTANT"],
        error: AuthError.REFRESH_ACCESS_TOKEN_ERROR,
      };
      const user: AdapterUser = {
        id: "user-1",
        email: "test@zenika.com",
        emailVerified: null,
      };

      const result = (await authOptions.callbacks!.session!({
        session,
        token,
        user,
        newSession: undefined,
        trigger: "update",
      })) as Session;

      expect(result.error).toBe(AuthError.REFRESH_ACCESS_TOKEN_ERROR);
      expect(result.user?.roles).toEqual(["CONSULTANT"]);
    });
  });

  describe("ProtectedRoute", () => {
    it("affiche le message d'expiration et déclenche signOut si la session a expiré", () => {
      const signOutMock = vi.fn();
      vi.mocked(nextAuthReact.signOut).mockImplementation(signOutMock);
      vi.mocked(nextAuthReact.useSession).mockReturnValue({
        data: {
          user: { name: "Test User", roles: ["CONSULTANT" as const] },
          error: AuthError.REFRESH_ACCESS_TOKEN_ERROR,
          expires: "2099-01-01",
        },
        status: "authenticated",
        update: vi.fn(),
      });

      render(
        <ProtectedRoute allowedRoles={["CONSULTANT"]}>
          <div>Contenu Protégé</div>
        </ProtectedRoute>,
      );

      expect(signOutMock).toHaveBeenCalledWith({ redirect: false });
      expect(
        screen.getByText("Votre session a expiré. Merci de vous reconnecter."),
      ).toBeInTheDocument();
      expect(screen.queryByText("Contenu Protégé")).not.toBeInTheDocument();
    });
  });
});
