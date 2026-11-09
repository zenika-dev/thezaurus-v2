"use client";

import { usePathname } from "next/navigation";
import { UserRound } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuProfileLinkProps {
  open: boolean;
  onNavigate: () => void;
}

export function SideMenuProfileLink({ open, onNavigate }: SideMenuProfileLinkProps) {
  const pathname = usePathname();

  return (
    <SideMenuNavItem
      icon={UserRound}
      label="Mon profil"
      open={open}
      active={pathname.startsWith("/profile")}
      href="/profile"
      onClick={onNavigate}
    />
  );
}
