"use client";

import { useConferences } from "@/features/conferences/model";
import { ConferenceData } from "@/entities/conference";
import { StatusTag } from "@/features/conferences/ui";

const fakeConferences: ConferenceData[] = [
  {
    id: "1",
    title: "Conférence 1",
    cfpLink: "https://conf1.com",
    cfpStatus: "Open",
    submittedTalksAmount: 3,
    location: "Paris",
    date: "2022-01-01",
  },
  {
    id: "2",
    title: "Conférence 2",
    cfpLink: "https://conf2.com",
    cfpStatus: "Closed",
    submittedTalksAmount: 2,
    location: "Lyon",
    date: "2022-02-01",
  },
  {
    id: "3",
    title: "Conférence 3",
    cfpLink: "https://conf3.com",
    cfpStatus: "Closed",
    submittedTalksAmount: 1,
    location: "Marseille",
    date: "2022-03-01",
  },
  {
    id: "4",
    title: "Conférence 4",
    cfpLink: "https://conf4.com",
    cfpStatus: "Open",
    submittedTalksAmount: 19,
    location: "Lille",
    date: "2026-09-10",
  },
];

export function ConferencesList() {
  // const { conferences } = useConferences();

  return (
    <>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* todo use real conferences */}
        {fakeConferences.map((conference: ConferenceData) => (
          <div
            key={conference.id}
            className="p-4 flex justify-between items-center rounded-2xl border border-primary"
          >
            <div className="flex flex-col gap-0.5">
              <span className="font-bold text-text">{conference.title}</span>
              <span className="text-text">{conference.cfpLink}</span>
              <span className="text-sm text-text-muted">
                {conference.location}
                <br />
                {/* todo change br / design */}
                {conference.date}
              </span>
            </div>
            <StatusTag status={conference.cfpStatus} />
          </div>
        ))}

        {fakeConferences.length === 0 && (
          <div className="p-8 text-center text-text-muted border border-border rounded-2xl">
            Aucune conférence listée pour le moment. De nouvelles seront
            ajoutées bientôt !
          </div>
        )}
      </div>
    </>
  );
}
