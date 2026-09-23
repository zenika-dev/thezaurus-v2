"use client";

import { useState } from "react";
import { useSession } from "next-auth/react";
import Image from "next/image";
import type { UserProfile } from "@/entities/user";

interface ProfileIdentityProps {
  profile: UserProfile;
}

/** Identité en lecture seule : le compte Google Zenika en est la source de vérité. */
export function ProfileIdentity({ profile }: ProfileIdentityProps) {
  const { data: session } = useSession();
  const [imageFailed, setImageFailed] = useState(false);
  const avatarUrl = session?.user?.image;

  return (
    <section
      aria-labelledby="profile-identity-title"
      className="rounded border border-gray-200 dark:border-[#2d2d2d] p-6"
    >
      <div className="flex items-center gap-4 mb-6">
        {avatarUrl && !imageFailed ? (
          <Image
            src={avatarUrl}
            alt={profile.name || "Photo de profil"}
            width={64}
            height={64}
            unoptimized
            referrerPolicy="no-referrer"
            onError={() => setImageFailed(true)}
            className="w-16 h-16 rounded-full object-cover border border-gray-200 dark:border-[#2d2d2d] shrink-0"
          />
        ) : (
          <div className="w-16 h-16 rounded-full bg-gray-100 dark:bg-[#2d2d2d] flex items-center justify-center text-[20px] font-bold text-text-muted shrink-0">
            {profile.name?.charAt(0)?.toUpperCase() || "?"}
          </div>
        )}

        <div>
          <h2
            id="profile-identity-title"
            className="text-[20px] leading-[1.6] font-bold text-text m-0 mb-1"
          >
            Mes informations
          </h2>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0">
            Ces informations proviennent de ton compte Google Zenika et ne sont pas modifiables ici.
          </p>
        </div>
      </div>

      <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 m-0">
        <div>
          <dt className="text-[12px] uppercase tracking-[0.5px] text-text-muted m-0">Nom</dt>
          <dd className="text-[15px] text-text m-0 mt-1">{profile.name || "—"}</dd>
        </div>
        <div>
          <dt className="text-[12px] uppercase tracking-[0.5px] text-text-muted m-0">Email</dt>
          <dd className="text-[15px] text-text m-0 mt-1">{profile.email || "—"}</dd>
        </div>
      </dl>
    </section>
  );
}
