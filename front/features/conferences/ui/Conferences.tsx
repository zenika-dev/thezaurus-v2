"use client";

import { DataErrorBoundary, PageHeader, CreateButton } from "@/shared/ui";
import { Suspense, useState } from "react";
import { ConferencesListSkeleton } from "./ConferencesSkeleton";
import { ConferencesList } from "./ConferencesList";

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
    <div className="p-4 sm:p-6 lg:p-8">
      <PageHeader title="Conférences et CFP" subtitle={<>Suivez les conférences et les deadlines de CFP.</>} action={
        <CreateButton
          onClick={() => setOpen(true)}
        >
          <Plus size={16} />
          Nouvelle conférence
        </CreateButton>
      } />

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
