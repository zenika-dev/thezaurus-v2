"use client";

import { Suspense, useState } from "react";

import { Plus } from "lucide-react";
import dynamic from "next/dynamic";
import { useTalksMutations } from "@/features/talks/model/useTalksMutations";
import { DataErrorBoundary, PageHeader, CreateButton } from "@/shared/ui";
import { TalkTable } from "./TalkTable";
import { TalkTableSkeleton } from "./TalkDashboardSkeleton";

const CreateTalkDialog = dynamic(
  () => import("./CreateTalkDialog").then((m) => ({ default: m.CreateTalkDialog })),
  { ssr: false }
);

export function TalkDashboard() {
  const [open, setOpen] = useState(false);
  const { createTalk } = useTalksMutations();

  const handleSubmit = createTalk;

  return (
    <div className="p-4 sm:p-6 lg:p-8 pb-0 flex flex-col min-h-full">
      {/* Header — always rendered, independent of data */}
      <PageHeader title="Talks" subtitle={<>Gérez le cycle de vie des talks, de l&apos;idée au replay.</>} action={
        <CreateButton
          onClick={() => setOpen(true)}
        >
          <Plus size={16} />
          New Talk
        </CreateButton>
      } />

      {/* Data section — can load/fail independently */}
      <Suspense fallback={<TalkTableSkeleton />}>
        <DataErrorBoundary>
          <TalkTable />
        </DataErrorBoundary>
      </Suspense>

      <CreateTalkDialog open={open} onClose={() => setOpen(false)} onSubmit={handleSubmit} />
    </div>
  );
}

export default TalkDashboard;
