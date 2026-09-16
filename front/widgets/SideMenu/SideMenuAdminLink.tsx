"use client";

import { usePathname, useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { Settings } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuAdminLinkProps {
  open: boolean;
}

export function SideMenuAdminLink({ open }: SideMenuAdminLinkProps) {
  const router = useRouter();
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
      onClick={() => router.push("/admin")}
    />
  );
}
