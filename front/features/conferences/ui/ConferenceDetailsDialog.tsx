"use client";

import { FormDialog } from "@/shared/ui";
import { CopyDetailLinkButton } from "@/shared/ui/CopyDetailLinkButton";
import { type ConferenceData, formatConferenceDate, formatLocation } from "@/entities/conference";
import { StatusTag, TypeTag } from "./ConferenceTags";
import { Calendar, ExternalLink, MapPin } from "lucide-react";

export function ConferenceDetailsDialog({ conference, onClose }: {
  conference: ConferenceData; onClose: () => void;
}) {
  return <FormDialog open onClose={onClose} title={conference.name} cancelLabel="Fermer"
    headerAction={<StatusTag status={conference.cfpStatus} />}
    actions={<CopyDetailLinkButton path="/conferences" parameter="conference" id={conference.id} />}>
    <div className="flex flex-col gap-4 text-sm">
      <p className="flex items-center gap-2"><MapPin size={16} />{formatLocation(conference.location)}</p>
      <p className="flex items-center gap-2"><Calendar size={16} />{formatConferenceDate(conference.date)}</p>
      <div className="flex items-center gap-2"><TypeTag type={conference.type} small />{conference.reach}</div>
      <p>{conference.submittedTalksAmount} talk{conference.submittedTalksAmount !== 1 ? "s" : ""} soumis</p>
      {conference.cfpOpeningDate && <p>Ouverture du CFP : {new Intl.DateTimeFormat("fr-FR").format(new Date(conference.cfpOpeningDate))}</p>}
      {conference.cfpClosingDate && <p>Fermeture du CFP : {new Intl.DateTimeFormat("fr-FR").format(new Date(conference.cfpClosingDate))}</p>}
      {conference.cfpLink && conference.cfpStatus !== "None" && <a href={conference.cfpLink} target="_blank" rel="noopener noreferrer"
        className="flex items-center gap-2 text-link hover:underline">Voir le CFP <ExternalLink size={14} /></a>}
    </div>
  </FormDialog>;
}
