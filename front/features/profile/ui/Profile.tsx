"use client";

import { Suspense } from "react";
import Button from "@mui/material/Button";
import { LogOut } from "lucide-react";
import { signOut } from "next-auth/react";
import { DataErrorBoundary, PageHeader } from "@/shared/ui";
import { ProfileSections } from "./ProfileSections";
import { ProfileSectionsSkeleton } from "./ProfileSkeleton";

export function Profile() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader title="Mon profil" subtitle="Tes informations personnelles et tes préférences de notification."
        action={<Button variant="contained" onClick={() => signOut()} startIcon={<LogOut size={16} />}>Se déconnecter</Button>} />
      <Suspense fallback={<ProfileSectionsSkeleton />}>
        <DataErrorBoundary><ProfileSections /></DataErrorBoundary>
      </Suspense>
    </div>
  );
}
export default Profile;
