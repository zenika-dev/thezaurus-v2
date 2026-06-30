"use client";

import { useConferences } from "@/features/conferences/model";
import { ConferenceData } from "@/entities/conference";
import { StatusTag } from "@/features/conferences/ui";
import { useState } from "react";
import {
  MapPin,
  Calendar,
  ExternalLink as ExternalLinkIcon,
  Funnel,
} from "lucide-react";

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
  const { conferences } = useConferences();

  const [statusFilter, setStatusFilter] = useState<"All" | "Open" | "Closed">(
    "All",
  );
  const [yearFilter, setYearFilter] = useState<
    "All" | "This Year" | "Next Year"
  >("All");

  const currentYear = new Date().getFullYear();

  // const filteredConferences = conferences.filter((conference) => {

  const filteredConferences = fakeConferences.filter((conference) => {
    if (statusFilter !== "All" && conference.cfpStatus !== statusFilter) {
      return false;
    }
    if (yearFilter !== "All") {
      const confYear = new Date(conference.date).getFullYear();
      if (yearFilter === "This Year" && confYear !== currentYear) return false;
      if (yearFilter === "Next Year" && confYear !== currentYear + 1)
        return false;
    }
    return true;
  });

  const FilterBadge = ({
    label,
    active,
    onClick,
  }: {
    label: string;
    active: boolean;
    onClick: () => void;
  }) => (
    <button
      onClick={onClick}
      className={`inline-flex items-center gap-1 px-3 py-1 rounded-2xl text-xs font-sans border cursor-pointer transition-colors ${
        active
          ? "bg-primary text-white border-primary"
          : "bg-surface-hover text-text border-transparent hover:bg-border-strong"
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-6 flex-wrap">
        <div className="flex items-center gap-2 flex-wrap">
          <Funnel size={14} className="text-text-muted shrink-0" />
          <span className="text-xs text-text-muted mr-2">Année :</span>
          {(["All", "This Year", "Next Year"] as const).map((year) => (
            <FilterBadge
              key={year}
              label={
                year === "All"
                  ? "Toutes"
                  : year === "This Year"
                    ? currentYear.toString()
                    : (currentYear + 1).toString()
              }
              active={yearFilter === year}
              onClick={() => setYearFilter(year)}
            />
          ))}
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-text-muted mr-2">Statut CFP :</span>
          {(["All", "Open", "Closed"] as const).map((status) => (
            <FilterBadge
              key={status}
              label={status === "All" ? "Tous" : status}
              active={statusFilter === status}
              onClick={() => setStatusFilter(status)}
            />
          ))}
        </div>

        {(statusFilter !== "All" || yearFilter !== "All") && (
          <button
            onClick={() => {
              setStatusFilter("All");
              setYearFilter("All");
            }}
            className="flex text-xs items-center font-sans gap-1 cursor-pointer text-primary border border-primary/20 px-3 py-1 rounded-2xl bg-primary/10 transition-colors no-underline hover:bg-primary/20 hover:text-primary"
          >
            Réinitialiser
          </button>
        )}

        <div className="ml-auto">
          <a
            href={`https://developers.events/#/${currentYear}/cfp`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex text-xs items-center gap-1 text-primary px-3 py-1.5 rounded-2xl transition-colors no-underline hover:bg-primary/10 hover:text-primary"
          >
            developers.events <ExternalLinkIcon size={14} />
          </a>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* todo use real conferences */}
        {filteredConferences.map(
          (conference: ConferenceData, index: number) => (
            <div
              key={`${conference.id}-${statusFilter}-${yearFilter}`}
              className="p-6 flex flex-col gap-2 rounded-2xl border border-primary animate-fade-in-up"
              style={{ animationDelay: `${index * 50}ms` }}
            >
              <div className="flex justify-between items-start">
                <div className="flex flex-col gap-2">
                  <span className="font-bold text-text">
                    {conference.title}
                  </span>
                  <div className="flex flex-col gap-1 text-xs text-text-muted">
                    <span className="flex items-center gap-1.5">
                      <MapPin size={14} className="text-text-muted shrink-0" />
                      {conference.location}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Calendar
                        size={14}
                        className="text-text-muted shrink-0"
                      />
                      {conference.date}
                    </span>
                  </div>
                </div>
                <StatusTag status={conference.cfpStatus} />
              </div>
              <div className="flex items-center justify-between text-xs mt-2 text-text-muted">
                <span>
                  {conference.submittedTalksAmount} talk
                  {conference.submittedTalksAmount !== 1 ? "s" : ""} soumis
                </span>
                {conference.cfpLink && (
                  <a
                    href={conference.cfpLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 text-primary no-underline hover:underline font-bold"
                  >
                    Lien CFP <ExternalLinkIcon size={14} />
                  </a>
                )}
              </div>
            </div>
          ),
        )}

        {filteredConferences.length === 0 && (
          <div className="p-8 text-center text-text-muted border border-border rounded-2xl md:col-span-2 lg:col-span-3">
            Aucune conférence listée pour le moment. De nouvelles seront
            ajoutées bientôt !
          </div>
        )}
      </div>
    </div>
  );
}
