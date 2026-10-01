import { NextAuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import GoogleProvider from "next-auth/providers/google";
import type { Role } from "@/shared/api";
import { AuthError } from "./types";
import { requestGoogleTokenRefresh } from "./google-token";

async function refreshGoogleTokens(token: JWT): Promise<JWT> {
  try {
    if (!token.refreshToken) {
      throw new Error("Missing refresh token for Google OAuth refresh");
    }

    const refreshed = await requestGoogleTokenRefresh(token.refreshToken);
    if (!refreshed) {
      throw new Error("Missing Google credentials");
    }

    return {
      ...token,
      idToken: refreshed.idToken ?? token.idToken,
      expiresAt: Math.floor(Date.now() / 1000 + (refreshed.expiresIn ?? 3600)),
      refreshToken: refreshed.refreshToken ?? token.refreshToken,
      error: undefined,
    };
  } catch (error) {
    console.error("Erreur de rafraîchissement du token Google:", error);
    return {
      ...token,
      error: AuthError.REFRESH_ACCESS_TOKEN_ERROR,
    };
  }
}


export const authOptions: NextAuthOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID as string,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET as string,
      authorization: {
        params: {
          prompt: "consent",
          access_type: "offline",
          response_type: "code",
        },
      },
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        if (account.id_token) {
          token.idToken = account.id_token;
        }
        if (account.refresh_token) {
          token.refreshToken = account.refresh_token;
        }
        const expiresIn = typeof account.expires_in === "number" ? account.expires_in : 3600;
        token.expiresAt =
          account.expires_at ??
          Math.floor(Date.now() / 1000 + expiresIn);

        try {
          const API_BASE = process.env.API_URL ?? "http://localhost:8080";

          const { headers } = await import("next/headers");
          const incoming = await headers();
          // L'appel au sidecar en localhost ne passe pas par IAP : on relaie l'assertion
          // de la requête entrante du navigateur plutôt que d'en fabriquer une nouvelle.
          const iapAssertion = incoming.get("x-goog-iap-jwt-assertion");
          const authHeader: Record<string, string> = iapAssertion
            ? { "x-goog-iap-jwt-assertion": iapAssertion }
            : { Authorization: `Bearer ${account.id_token}` };

          const res = await fetch(`${API_BASE}/api/me`, { headers: authHeader });

          if (!res.ok) {
            throw new Error("Not authenticated");
          }

          const data = await res.json();
          token.roles = (data.roles as Role[]) ?? [];
        } catch (err) {
          console.error("Erreur récupération des rôles:", err);
          // Fail-closed : sans réponse du backend, aucun rôle — l'accès sera refusé.
          token.roles = [];
        }
        return token;
      }

      if (token.expiresAt && Date.now() < (token.expiresAt - 60) * 1000) {
        return token;
      }

      if (token.refreshToken) {
        return refreshGoogleTokens(token);
      }

      return {
        ...token,
        error: AuthError.REFRESH_ACCESS_TOKEN_ERROR,
      };
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.roles = (token.roles as Role[]) ?? [];
      }
      if (token.error) {
        session.error = token.error;
      }
      return session;
    },
  },
  secret: process.env.NEXTAUTH_SECRET,
  useSecureCookies: process.env.NEXTAUTH_URL?.startsWith("https://"),
  session: {
    strategy: "jwt",
  },
};
