"use client";

import Link from "next/link";

import { useDetailUrl } from "@/shared/lib/useDetailUrl";
import { DetailUrlFeedback } from "@/shared/ui/DetailUrlFeedback";
import { CopyDetailLinkButton } from "@/shared/ui/CopyDetailLinkButton";
import { talkApi } from "@/entities/talk";
import { ContributionFilters, PaginationBar } from "@/shared/ui";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import useMediaQuery from "@mui/material/useMediaQuery";
import { Surface } from "@/shared/ui";
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
  const mobile = useMediaQuery("(max-width: 767px)");
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

  const detail = useDetailUrl("talk", "talks", talks, talkApi.getTalk);
  const { selected: selectedTalk, selectedId: selectedTalkId, select: setSelectedTalkId } = detail;

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

      {mobile ? <div className="mb-4">
        {talks.length === 0 ? (
          <Surface className="p-4 text-center text-text-muted">
            Aucun talk ne correspond à ces critères.
          </Surface>
        ) : (
          <ul aria-label="Talks" className="list-none m-0 p-0 flex flex-col gap-3">
            {talks.map((talk) => (
              <Surface component="li" key={talk.id} className="p-4 min-w-0">
                <Link
                  href={detail.href(talk.id)}
                  scroll={false}
                  className="text-link underline font-medium cursor-pointer text-left bg-transparent border-0 p-0 wrap-anywhere"
                >
                  {talk.title}
                </Link>
                <div className="flex flex-wrap gap-2 mt-3">
                  <StatusTag status={talk.status} />
                  <VisibilityTag visibility={talk.visibility} />
                </div>
                <dl className="m-0 mt-3 flex flex-col gap-3 text-sm">
                  <div>
                    <dt className="text-text-muted">Speakers</dt>
                    <dd className="m-0 mt-1 flex flex-wrap gap-1">
                      {talk.speakers?.length ? talk.speakers.map((speaker, index) => (
                        <SpeakerChip key={index} name={speaker.name} email={speaker.email} size="small" className="max-w-full" />
                      )) : "—"}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-text-muted">Agence</dt>
                    <dd className="m-0">{agencyLabels[talk.office] || "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-text-muted">Conférence</dt>
                    <dd className="m-0 wrap-anywhere">{talk.conference?.name || "—"}</dd>
                  </div>
                </dl>
              </Surface>
            ))}
          </ul>
        )}
      </div> : <TableContainer
        component={Surface}
        variant="outlined"
        className="overflow-x-auto mb-4"
        tabIndex={0}
        role="region"
        aria-label="Tableau des talks, défilement horizontal"
      >
        <Table sx={{ minWidth: 760 }}>
          <TableHead className="bg-surface-muted">
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
                <TableCell colSpan={7} align="center" className="py-8 text-text-muted">
                  Aucun talk ne correspond à ces critères.
                </TableCell>
              </TableRow>
            ) : (
              talks.map((talk) => (
                <TableRow key={talk.id} hover>
                  <TableCell>
                    <Link
                      href={detail.href(talk.id)}
                      scroll={false}
                      className="text-link underline font-medium hover:text-link text-sm cursor-pointer text-left bg-transparent border-0 p-0"
                    >
                      {talk.title}
                    </Link>
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
                    <CopyDetailLinkButton path="/talks" parameter="talk" id={talk.id} iconOnly label={`Copier le lien vers le talk : ${talk.title}`} />
                    {(talk.status === "ACCEPTED" || talk.status === "DONE") && (
                      <Link
                        href={detail.href(talk.id)}
                        scroll={false}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded text-xs
                          border border-border text-text bg-surface font-sans
                          hover:bg-primary/10 hover:text-link transition-colors cursor-pointer"
                      >
                        <Eye size={12} />
                        <span>Détail</span>
                      </Link>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>}

      <PaginationBar
        className="max-sm:-mx-4 max-sm:px-4 sm:max-lg:-mx-6 sm:max-lg:px-6"
        page={page}
        pageSize={pageSize}
        total={total}
        totalPages={totalPages}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        label="Talks par page :"
      />

      <DetailUrlFeedback loading={detail.loading} error={detail.error} onClose={detail.close} onRetry={detail.retry} />
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
