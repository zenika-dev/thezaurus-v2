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
          const res = await fetch(`${process.env.BACKEND_URL ?? ""}/api/me`, {
            headers: { Authorization: `Bearer ${account.id_token}` },
          });

          if (!res.ok) {
            throw new Error("Not authenticated");
          }

          const data = await res.json();
          token.roles = (data.roles as Role[]) ?? ["membre"];
        } catch (err) {
          console.error("Erreur récupération des rôles:", err);
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
  session: {
    strategy: "jwt",
  },
};