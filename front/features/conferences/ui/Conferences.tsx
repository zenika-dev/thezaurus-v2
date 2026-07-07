"use client";

import { DataErrorBoundary } from "@/shared/ui";
import { Suspense, useState } from "react";
import { ConferencesListSkeleton } from "./ConferencesSkeleton";
import { ConferencesList } from "./ConferencesList";
import { Button } from "@mui/material";
import { Plus } from "lucide-react";
import { useConferencesMutations } from "../model/useConferencesMutations";
import { ConferenceData } from "@/entities/conference";
import dynamic from "next/dynamic";

const CreateConferenceDialog = dynamic(
  () =>
    import("./CreateConferenceDialog").then((m) => ({
      default: m.CreateConferenceDialog,
    })),
  { ssr: false },
);

export function Conferences() {
  const [open, setOpen] = useState(false);
  const { createConference } = useConferencesMutations();

  const handleSubmit = async (conference: ConferenceData) => {
    try {
      await createConference(conference);
    } catch {
      alert("Erreur lors de la création de la conférence");
    }
  };

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">
            Conférences et CFP
          </h1>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
            Suivez les conférences et les deadlines de CFP.
          </p>
        </div>
        <Button
          variant="contained"
          onClick={() => setOpen(true)}
          className="gap-2! font-bold! py-2! shadow-none! rounded-[20px]! bg-[linear-gradient(135deg,#ed213c_0%,#BF1D67_100%)]! transition-transform! duration-200! hover:shadow-none! hover:-translate-y-[2px]!"
        >
          <Plus size={16} />
          Nouvelle conférence
        </Button>
      </div>

      <Suspense fallback={<ConferencesListSkeleton />}>
        <DataErrorBoundary>
          <ConferencesList />
        </DataErrorBoundary>
      </Suspense>

      <CreateConferenceDialog
        open={open}
        onClose={() => setOpen(false)}
        onSubmit={handleSubmit}
      />
    </div>
  );
}

export default Conferences;
