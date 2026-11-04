"use client";

import { usePathname } from "next/navigation";
import { useSession } from "next-auth/react";
import { Settings } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuAdminLinkProps {
  open: boolean;
  onNavigate: () => void;
}

export function SideMenuAdminLink({ open, onNavigate }: SideMenuAdminLinkProps) {
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
      href="/admin"
      onClick={onNavigate}
    />
  );
}
