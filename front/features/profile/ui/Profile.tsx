"use client";

import { Suspense } from "react";
import { DataErrorBoundary } from "@/shared/ui";
import { ProfileSections } from "./ProfileSections";
import { ProfileSectionsSkeleton } from "./ProfileSkeleton";

export function Profile() {
  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">
          Mon profil
        </h1>
        <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
          Tes informations personnelles et tes préférences de notification.
        </p>
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
