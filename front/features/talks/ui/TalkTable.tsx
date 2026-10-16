"use client";

import { useState } from "react";
import { ContributionFilters, PaginationBar } from "@/shared/ui";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import { Eye } from "lucide-react";
import { TalkStatus } from "@/shared/api";
import { agencyLabels, talkStatusConfig } from "@/entities/talk";
import { SpeakerChip } from "@/entities/user";
import dynamic from "next/dynamic";
import { useTalks } from "@/features/talks/model";
import { usePaginationUrlParams } from "@/shared/lib";
import { StatusTag, VisibilityTag } from "./TalkTags";

const TalkDetailsDialog = dynamic(
  () => import("./TalkDetailsDialog").then((m) => ({ default: m.TalkDetailsDialog })),
  { ssr: false }
);

export function TalkTable() {
  const [selectedTalkId, setSelectedTalkId] = useState<string | null>(null);
  const {
    page,
    pageSize,
    status,
    personal,
    setStatus,
    setPersonal,
    setPage,
    setPageSize,
    resetFilters,
    apiParams,
  } = usePaginationUrlParams(TalkStatus);

  const { talks, total, totalPages, updateTalk, deleteTalk } = useTalks(apiParams);

  const selectedTalk = talks.find((talkItem) => talkItem.id === selectedTalkId) ?? null;

  const handleUpdate = updateTalk;
  const handleDelete = deleteTalk;

  return (
    <div className="flex flex-col flex-1">
      <div className="mb-4 shrink-0">
        <ContributionFilters
          statuses={TalkStatus}
          labels={talkStatusConfig}
          status={status}
          onStatusChange={setStatus}
          personalLabel="Mes talks"
          personalOnly={personal}
          onPersonalChange={setPersonal}
          onReset={resetFilters}
        />
      </div>

      <TableContainer
        component={Paper}
        variant="outlined"
        className="border! border-primary! rounded-2xl! overflow-hidden! bg-surface! mb-4"
      >
        <Table>
          <TableHead className="bg-surface-muted!">
            <TableRow>
              <TableCell><strong>Titre</strong></TableCell>
              <TableCell><strong>Speaker</strong></TableCell>
              <TableCell><strong>Agence</strong></TableCell>
              <TableCell><strong>Conférence</strong></TableCell>
              <TableCell><strong>Statut</strong></TableCell>
              <TableCell align="center"><strong>Visibilité</strong></TableCell>
              <TableCell align="center"><strong>Actions</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {talks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" className="py-8! text-text-muted!">
                  Aucun talk ne correspond à ces critères.
                </TableCell>
              </TableRow>
            ) : (
              talks.map((talk) => (
                <TableRow key={talk.id} hover>
                  <TableCell>
                    <span
                      role="button"
                      onClick={() => setSelectedTalkId(talk.id)}
                      className="text-primary underline font-medium hover:text-primary-dark text-sm cursor-pointer"
                    >
                      {talk.title}
                    </span>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {talk.speakers && talk.speakers.length > 0 ? (
                        talk.speakers.map((speaker, index) => (
                          <SpeakerChip
                            key={index}
                            name={speaker.name}
                            email={speaker.email}
                            size="small"
                            className="text-xs h-6"
                          />
                        ))
                      ) : (
                        <span className="text-text-muted">—</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>{agencyLabels[talk.office] || "—"}</TableCell>
                  <TableCell>{talk.conference?.name || "—"}</TableCell>
                  <TableCell><StatusTag status={talk.status} /></TableCell>
                  <TableCell align="center"><VisibilityTag visibility={talk.visibility} /></TableCell>
                  <TableCell align="center">
                    {(talk.status === "ACCEPTED" || talk.status === "DONE") && (
                      <button
                        onClick={() => setSelectedTalkId(talk.id)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded text-xs
                          border border-border text-text bg-surface font-sans
                          hover:bg-primary/10 hover:text-primary transition-colors cursor-pointer"
                      >
                        <Eye size={12} />
                        <span>Détail</span>
                      </button>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <PaginationBar
        page={page}
        pageSize={pageSize}
        total={total}
        totalPages={totalPages}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        label="Talks par page :"
      />

      <TalkDetailsDialog
        talk={selectedTalk}
        open={!!selectedTalkId}
        onClose={() => setSelectedTalkId(null)}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
      />
    </div>
  );
}
