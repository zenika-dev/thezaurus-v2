"use client";

import { useConferences } from "@/features/conferences/model";
import {
  ConferenceData,
  formatConferenceDate,
  getConferenceYear,
  formatLocation,
} from "@/entities/conference";
import { StatusTag, TypeTag } from "@/features/conferences/ui";
import { FilterChip, Surface } from "@/shared/ui";
import { useState } from "react";
import dayjs from "dayjs";
import {
  MapPin,
  Calendar,
  ExternalLink as ExternalLinkIcon,
  Funnel,
} from "lucide-react";

export function ConferencesList() {
  const { conferences } = useConferences();

  const [statusFilter, setStatusFilter] = useState<"All" | "Open" | "Closed">(
    "All",
  );
  const [yearFilter, setYearFilter] = useState<"All" | number>("All");
  const [sort, setSort] = useState("default");

  const currentYear = new Date().getFullYear();
  const years = [...new Set([
    currentYear, currentYear + 1,
    ...conferences.map(conference => getConferenceYear(conference.date)).filter(Number.isFinite),
  ])].sort((left, right) => right - left);

  const filteredConferences = conferences.filter((conference) => {
    if (statusFilter !== "All" && conference.cfpStatus !== statusFilter) {
      return false;
    }
    if (yearFilter !== "All") {
      const confYear = getConferenceYear(conference.date);
      if (confYear !== yearFilter) return false;
    }
    return true;
  });
  if (sort !== "default") {
    filteredConferences.sort((left, right) => {
      const difference = sort === "submitted"
        ? right.submittedTalksAmount - left.submittedTalksAmount
        : (right.acceptedTalksAmount ?? 0) - (left.acceptedTalksAmount ?? 0);
      return difference || left.name.localeCompare(right.name, "fr") || left.id.localeCompare(right.id);
    });
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-6 flex-wrap">
        <div className="flex items-center gap-2 flex-wrap">
          <Funnel size={14} className="text-text-muted shrink-0" />
          <span className="text-xs text-text-muted mr-2">Année :</span>
          {(["All", ...years] as const).map((year) => (
            <FilterChip
              key={year}
              label={year === "All" ? "Toutes" : year.toString()}
              active={yearFilter === year}
              onClick={() => setYearFilter(year)}
            />
          ))}
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-text-muted mr-2">Statut CFP :</span>
          {(["All", "Open", "Closed"] as const).map((status) => (
            <FilterChip
              key={status}
              label={status === "All" ? "Tous" : status}
              active={statusFilter === status}
              onClick={() => setStatusFilter(status)}
            />
          ))}
        </div>

        <label className="flex items-center gap-2 text-xs text-text-muted">
          Trier par :
          <select
            value={sort}
            onChange={(event) => setSort(event.target.value)}
            className="rounded-lg border border-border bg-surface px-2 py-1.5 text-text"
          >
            <option value="default">Ordre initial</option>
            <option value="submitted">Soumissions décroissantes</option>
            <option value="accepted">Acceptations décroissantes</option>
          </select>
        </label>

        {(statusFilter !== "All" || yearFilter !== "All" || sort !== "default") && (
          <button
            onClick={() => {
              setStatusFilter("All");
              setYearFilter("All");
              setSort("default");
            }}
            className="flex text-xs items-center font-sans gap-1 cursor-pointer text-link border border-primary/20 px-3 py-1 rounded-2xl bg-primary/10 transition-colors no-underline hover:bg-primary/20 hover:text-link"
          >
            Réinitialiser
          </button>
        )}

        <div className="ml-auto">
          <a
            href={`https://developers.events/#/${currentYear}/cfp`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex text-xs items-center gap-1 text-link px-3 py-1.5 rounded-2xl transition-colors no-underline hover:bg-primary/10 hover:text-link"
          >
            developers.events <ExternalLinkIcon size={14} />
          </a>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredConferences.map(
          (conference: ConferenceData, index: number) => {
            const cfpOpeningDateJs = conference.cfpOpeningDate
              ? dayjs(conference.cfpOpeningDate)
              : null;
            const cfpClosingDateJs = conference.cfpClosingDate
              ? dayjs(conference.cfpClosingDate)
              : null;
            const isClosedByDate =
              cfpClosingDateJs && cfpClosingDateJs.isValid()
                ? dayjs().isAfter(cfpClosingDateJs, "day")
                : false;
            const isNotYetOpenByDate =
              cfpOpeningDateJs && cfpOpeningDateJs.isValid()
                ? dayjs().isBefore(cfpOpeningDateJs, "day")
                : false;

            return (
              <Surface
                key={`${conference.id}-${statusFilter}-${yearFilter}`}
                className="p-6 flex flex-col gap-2 animate-fade-in-up transition-shadow hover:shadow-md"
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <div className="flex flex-wrap justify-between items-start gap-4">
                  <div className="flex min-w-0 flex-1 flex-col gap-2">
                    <span className="font-bold text-text wrap-anywhere">
                      {conference.name}
                    </span>
                    <div className="flex flex-col gap-1.5 text-xs text-text-muted">
                      <span className="flex items-center gap-1.5">
                        <MapPin
                          size={14}
                          className="text-text-muted shrink-0"
                        />
                        {formatLocation(conference.location)}
                      </span>
                      <span className="flex items-center gap-1.5">
                        <Calendar
                          size={14}
                          className="text-text-muted shrink-0"
                        />
                        {formatConferenceDate(conference.date)}
                      </span>
                      {conference.type && (
                        <div className="mt-0.5">
                          <TypeTag type={conference.type} small />
                          <span className="text-[10px]">
                            {" "}
                            · {conference.reach}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1 shrink-0">
                    <StatusTag status={conference.cfpStatus} />
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 items-center justify-between text-xs mt-2 text-text-muted">
                  <span>
                    {conference.submittedTalksAmount} talk
                    {conference.submittedTalksAmount !== 1 ? "s" : ""} soumis
                  </span>
                  <span>
                    {conference.acceptedTalksAmount ?? 0} talk
                    {conference.acceptedTalksAmount !== 1 ? "s" : ""} accepté
                    {conference.acceptedTalksAmount !== 1 ? "s" : ""}
                  </span>
                  {conference.cfpLink && conference.cfpStatus !== "None" && (
                    <>
                      <a
                        href={conference.cfpLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={`flex items-center gap-1 text-link no-underline hover:underline ${isClosedByDate ? "text-text-muted hover:no-underline" : ""}`}
                      >
                        {conference.cfpStatus === "Open"
                          ? isNotYetOpenByDate
                            ? `CFP ouvre le ${cfpOpeningDateJs!.format("DD/MM/YYYY")}`
                            : cfpClosingDateJs && cfpClosingDateJs.isValid()
                              ? `CFP ouvert jusqu'au ${cfpClosingDateJs.format("DD/MM/YYYY")}`
                              : "CFP ouvert"
                          : "CFP fermé"}{" "}
                        <ExternalLinkIcon size={14} />
                      </a>
                    </>
                  )}
                </div>
              </Surface>
            );
          },
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
