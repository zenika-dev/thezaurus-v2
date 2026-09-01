"use client";

import type { UserProfile } from "@/entities/user";

interface ProfileIdentityProps {
  profile: UserProfile;
}

/** Identité en lecture seule : le compte Google Zenika en est la source de vérité. */
export function ProfileIdentity({ profile }: ProfileIdentityProps) {
  return (
    <section
      aria-labelledby="profile-identity-title"
      className="rounded border border-gray-200 dark:border-[#2d2d2d] p-6"
    >
      <h2
        id="profile-identity-title"
        className="text-[20px] leading-[1.6] font-bold text-text m-0 mb-1"
      >
        Mes informations
      </h2>
      <p className="text-[14px] leading-[1.43] text-text-muted m-0 mb-4">
        Ces informations proviennent de ton compte Google Zenika et ne sont pas modifiables ici.
      </p>

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
