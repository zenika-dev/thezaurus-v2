import { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

// Vocabulaire backend repris verbatim (enum Role côté API) : toute couche de traduction
// front serait un piège de plus. Cf. Role.java côté Quarkus.
export const ROLES = ["ADMIN", "DT", "CONSULTANT"] as const;
export type Role = (typeof ROLES)[number];

export const authOptions: NextAuthOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID as string,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET as string,
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      if (account?.id_token) {
        // Persisté pour la propagation d'identité des fetches server-side en dev
        // (cf. shared/api/auth-headers). En production, l'assertion IAP prime.
        token.idToken = account.id_token;
        try {
          const API_BASE = process.env.API_URL ?? "http://localhost:8080";
          const res = await fetch(`${API_BASE}/api/me`, {
            headers: { Authorization: `Bearer ${account.id_token}` },
          });

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
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.roles = (token.roles as Role[]) ?? [];
      }
      return session;
    },
  },
  secret: process.env.NEXTAUTH_SECRET,
  session: {
    strategy: "jwt",
  },
};
