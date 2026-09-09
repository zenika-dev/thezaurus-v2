import { DefaultSession } from "next-auth";
import type { Role } from "@/shared/api";
 
declare module "next-auth" {
  interface Session {
    user: {
      roles: Role[];
    } & DefaultSession["user"];
  }
}
 
declare module "next-auth/jwt" {
  interface JWT {
    roles?: Role[];
    idToken?: string;
  }
}
 