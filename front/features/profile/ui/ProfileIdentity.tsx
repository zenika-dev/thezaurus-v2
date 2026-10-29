"use client";

import { ProfileAvatar, ProfilePhotoEditor } from "@/entities/user";
import type { UserProfile } from "@/entities/user";
import { SectionCard } from "@/shared/ui";

interface ProfileIdentityProps {
  profile: UserProfile;
}

/** Le nom et l’email viennent de Google ; la photo peut être personnalisée. */
export function ProfileIdentity({ profile }: ProfileIdentityProps) {

  return (
    <SectionCard
      titleId="profile-identity-title"
      header={
        <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
          <ProfileAvatar email={profile.email} name={profile.name} />

          <div>
            <h2
              id="profile-identity-title"
              className="text-xl leading-relaxed font-bold text-text m-0 mb-1"
            >
              Mes informations
            </h2>
            <p className="text-sm leading-normal text-text-muted m-0">
              Ton nom et ton email proviennent de ton compte Google Zenika. Tu peux personnaliser ta photo.
            </p>
          </div>
        </div>
      }
    >
      <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 m-0 [&>div]:min-w-0">
        <div>
          <dt className="text-xs uppercase tracking-[0.5px] text-text-muted m-0">Nom</dt>
          <dd className="text-base break-words text-text m-0 mt-1">{profile.name || "—"}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-[0.5px] text-text-muted m-0">Email</dt>
          <dd className="text-base break-words text-text m-0 mt-1">{profile.email || "—"}</dd>
        </div>
      </dl>
      <div className="mt-6"><ProfilePhotoEditor email={profile.email} /></div>
    </SectionCard>
  );
}
