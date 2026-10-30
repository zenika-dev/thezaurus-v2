"use client";

import { Suspense } from "react";
import Button from "@mui/material/Button";
import { LogOut } from "lucide-react";
import { signOut } from "next-auth/react";
import { DataErrorBoundary, PageHeader } from "@/shared/ui";
import { ProfileSections } from "./ProfileSections";
import { ProfileSectionsSkeleton } from "./ProfileSkeleton";
import { ContributionStatisticsSection } from "./ContributionStatisticsSection";

export function Profile() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader title="Mon profil" subtitle="Tes informations personnelles, tes contributions et tes préférences de notification."
        action={<Button variant="contained" onClick={() => signOut()} startIcon={<LogOut size={16} />}>Se déconnecter</Button>} />
      <div className="flex flex-col gap-4 max-w-3xl">
        <ContributionStatisticsSection />
        <Suspense fallback={<ProfileSectionsSkeleton />}>
          <DataErrorBoundary><ProfileSections /></DataErrorBoundary>
        </Suspense>
      </div>
    </div>
  );
}
export default Profile;
