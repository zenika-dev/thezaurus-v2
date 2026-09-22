"use client";

import { usePathname, useRouter } from "next/navigation";
import { UserRound } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuProfileLinkProps {
  open: boolean;
}

export function SideMenuProfileLink({ open }: SideMenuProfileLinkProps) {
  const router = useRouter();
  const pathname = usePathname();

  return (
    <SideMenuNavItem
      icon={UserRound}
      label="Mon profil"
      open={open}
      active={pathname.startsWith("/profile")}
      onClick={() => router.push("/profile")}
    />
  );
}
