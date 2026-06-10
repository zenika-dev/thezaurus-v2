"use client";

import { Suspense, useState } from "react";
import Button from "@mui/material/Button";
import { Plus } from "lucide-react";
import type { TalkData } from "@/entities/talk";
import dynamic from "next/dynamic";
import { useTalksMutations } from "@/features/talks/model/useTalksMutations";
import { DataErrorBoundary } from "@/shared/ui";
import { TalkTable } from "./TalkTable";
import { TalkTableSkeleton } from "./TalkDashboardSkeleton";

const CreateTalkDialog = dynamic(
  () => import("./CreateTalkDialog").then((m) => ({ default: m.CreateTalkDialog })),
  { ssr: false }
);

export function TalkDashboard() {
  const [open, setOpen] = useState(false);
  const { createTalk } = useTalksMutations();

  const handleSubmit = async (talk: TalkData) => {
    try { await createTalk(talk); }
    catch { alert("Erreur lors de la création du talk"); }
  };

  return (
    <div className="p-8">
      {/* Header — always rendered, independent of data */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">Talks</h1>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
            Gérez le cycle de vie des talks, de l&apos;idée au replay.
          </p>
        </div>
        <Button
          variant="contained"
          onClick={() => setOpen(true)}
          className="gap-2! font-bold! py-2! shadow-none! rounded-[20px]! bg-[linear-gradient(135deg,#ed213c_0%,#BF1D67_100%)]! transition-transform! duration-200! hover:shadow-none! hover:-translate-y-[2px]!"
        >
          <Plus size={16} />
          New Talk
        </Button>
      </div>

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
