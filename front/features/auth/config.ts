import { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

export type Role = "membre" | "admin";

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
        try {
          const API_BASE = process.env.API_URL ?? "http://localhost:8080";
          const res = await fetch(`${API_BASE}/api/me`, {
            headers: { Authorization: `Bearer ${account.id_token}` },
          });

          if (!res.ok) {
            throw new Error("Not authenticated");
          }

          const data = await res.json();
          token.roles = (data.roles as Role[]) ?? ["membre"];
        } catch (err) {
          console.error("Erreur récupération des rôles:", err);
          // Si le backend refuse l'utilisateur ou si l'endpoint n'existe pas encore, on lui donne le rôle "membre" par défaut
          token.roles = ["membre"];
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