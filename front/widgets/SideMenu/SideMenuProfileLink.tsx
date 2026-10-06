"use client";

import { usePathname } from "next/navigation";
import { useGuardedPush } from "@/shared/lib/navigation-guard";
import { UserRound } from "lucide-react";
import { SideMenuNavItem } from "./SideMenuNavItem";

interface SideMenuProfileLinkProps {
  open: boolean;
}

export function SideMenuProfileLink({ open }: SideMenuProfileLinkProps) {
  const push = useGuardedPush();
  const pathname = usePathname();

  return (
    <SideMenuNavItem
      icon={UserRound}
      label="Mon profil"
      open={open}
      active={pathname.startsWith("/profile")}
      onClick={() => push("/profile")}
    />
  );
}
