"use client";

import { usePathname } from "next/navigation";
import { useGuardedPush } from "@/shared/lib/navigation-guard";
import { useSession } from "next-auth/react";
import { Settings } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuAdminLinkProps {
  open: boolean;
}

export function SideMenuAdminLink({ open }: SideMenuAdminLinkProps) {
  const push = useGuardedPush();
  const pathname = usePathname();
  const { data: session } = useSession();

  const isAdmin = (session?.user?.roles ?? []).includes("ADMIN");
  if (!isAdmin) {
    return null;
  }

  return (
    <SideMenuNavItem
      icon={Settings}
      label="Administration"
      open={open}
      active={pathname.startsWith("/admin")}
      onClick={() => push("/admin")}
    />
  );
}
