"use client";

import { useState } from "react";
import { useSession } from "next-auth/react";
import Image from "next/image";
import type { UserProfile } from "@/entities/user";
import { SectionCard } from "@/shared/ui";

interface ProfileIdentityProps {
  profile: UserProfile;
}

/** Identité en lecture seule : le compte Google Zenika en est la source de vérité. */
export function ProfileIdentity({ profile }: ProfileIdentityProps) {
  const { data: session } = useSession();
  const [imageFailed, setImageFailed] = useState(false);
  const avatarUrl = session?.user?.image;

  return (
    <SectionCard
      titleId="profile-identity-title"
      header={
        <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
          {avatarUrl && !imageFailed ? (
            <Image
              src={avatarUrl}
              alt={profile.name || "Photo de profil"}
              width={64}
              height={64}
              unoptimized
              referrerPolicy="no-referrer"
              onError={() => setImageFailed(true)}
              className="w-16 h-16 rounded-full object-cover border border-border shrink-0"
            />
          ) : (
            <div className="w-16 h-16 rounded-full bg-surface-muted flex items-center justify-center text-[20px] font-bold text-text-muted shrink-0">
              {profile.name?.charAt(0)?.toUpperCase() || "?"}
            </div>
          )}

          <div>
            <h2
              id="profile-identity-title"
              className="text-xl leading-relaxed font-bold text-text m-0 mb-1"
            >
              Mes informations
            </h2>
            <p className="text-sm leading-normal text-text-muted m-0">
              Ces informations proviennent de ton compte Google Zenika et ne sont pas modifiables ici.
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
    </SectionCard>
  );
}
