import { DefaultSession } from "next-auth";
import type { Role } from "@/shared/api";
import type { AuthError } from "@/features/auth";

declare module "next-auth" {
  interface Session {
    user: {
      roles: Role[];
    } & DefaultSession["user"];
    error?: AuthError;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    roles?: Role[];
    idToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    error?: AuthError;
  }
}