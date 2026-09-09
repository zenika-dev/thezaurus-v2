"use client";

import { Suspense } from "react";
import Button from "@mui/material/Button";
import { LogOut } from "lucide-react";
import { signOut } from "next-auth/react";
import { DataErrorBoundary } from "@/shared/ui";
import { ProfileSections } from "./ProfileSections";
import { ProfileSectionsSkeleton } from "./ProfileSkeleton";

export function Profile() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">
            Mon profil
          </h1>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
            Tes informations personnelles et tes préférences de notification.
          </p>
        </div>
        <Button
          variant="contained"
          onClick={() => signOut()}
          className="gap-2! font-bold! py-2! shadow-none! rounded-[20px]! bg-[linear-gradient(135deg,#ed213c_0%,#BF1D67_100%)]! transition-transform! duration-200! hover:shadow-none! hover:-translate-y-[2px]!"
        >
          <LogOut size={16} />
          Se déconnecter
        </Button>
      </div>

      <Suspense fallback={<ProfileSectionsSkeleton />}>
        <DataErrorBoundary>
          <ProfileSections />
        </DataErrorBoundary>
      </Suspense>
    </div>
  );
}

export default Profile;
