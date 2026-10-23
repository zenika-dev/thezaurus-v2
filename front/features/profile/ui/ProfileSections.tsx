"use client";

import { useProfile } from "../model/useProfile";
import { NotificationPreferencesSection } from "./NotificationPreferencesSection";
import { ProfileIdentity } from "./ProfileIdentity";
import { GeneralPreferencesSection } from "./GeneralPreferencesSection";
import { AiPhotoSection } from "./AiPhotoSection";

export function ProfileSections() {
  const { profile } = useProfile();

  return (
    <div className="flex flex-col gap-4 max-w-3xl">
      <ProfileIdentity profile={profile} />
      <AiPhotoSection profile={profile} />
      <GeneralPreferencesSection profile={profile} />
      <NotificationPreferencesSection profile={profile} />
    </div>
  );
}
