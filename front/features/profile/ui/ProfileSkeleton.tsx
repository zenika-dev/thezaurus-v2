import { PageHeaderSkeleton, SectionCardSkeleton } from "@/shared/ui";

export function ProfileSectionsSkeleton() {
  return (
    <div className="flex flex-col gap-4 max-w-3xl">
      {Array.from({ length: 3 }).map((_, i) => (
        <SectionCardSkeleton key={i} rows={2} />
      ))}
    </div>
  );
}

export function ProfileSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeaderSkeleton />
      <ProfileSectionsSkeleton />
    </div>
  );
}
