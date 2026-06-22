import { DefaultSession } from "next-auth";
import { Role } from "@/features/auth";
 
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
  }
}
 