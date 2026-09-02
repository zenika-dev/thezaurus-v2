"use client";

import { useState } from "react";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import { Eye, Funnel } from "lucide-react";
import type { TalkData, TalkStatus } from "@/entities/talk";
import { agencyLabels, talkStatusConfig, TALK_STATUSES } from "@/entities/talk";
import dynamic from "next/dynamic";
import { useTalks } from "@/features/talks/model";
import { StatusTag, VisibilityTag } from "./TalkTags";

const TalkDetailsDialog = dynamic(
  () => import("./TalkDetailsDialog").then((m) => ({ default: m.TalkDetailsDialog })),
  { ssr: false }
);

export function TalkTable() {
  const [selectedTalkId, setSelectedTalkId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<"All" | TalkStatus>("All");
  const { talks, updateTalk, deleteTalk } = useTalks();

  const selectedTalk = talks.find((t) => t.id === selectedTalkId) ?? null;

  const handleUpdate = async (updated: TalkData) => {
    try { await updateTalk(updated); }
    catch { alert("Erreur lors de la mise à jour du talk"); }
  };

  const handleDelete = async (id: string) => {
    try { await deleteTalk(id); }
    catch { alert("Erreur lors de la suppression du talk"); }
  };

  const filteredTalks = talks.filter((talk) => {
    if (statusFilter !== "All" && talk.status !== statusFilter) {
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
    <>
      <div className="flex flex-col gap-6 mb-6">
        <div className="flex items-center gap-6 flex-wrap">
          <div className="flex items-center gap-2 flex-wrap">
            <Funnel size={14} className="text-text-muted shrink-0" />
            <span className="text-xs text-text-muted mr-2">Statut :</span>
            {(["All", ...TALK_STATUSES] as const).map((status) => (
              <FilterBadge
                key={status}
                label={status === "All" ? "Tous" : talkStatusConfig[status].label}
                active={statusFilter === status}
                onClick={() => setStatusFilter(status)}
              />
            ))}
          </div>

          {statusFilter !== "All" && (
            <button
              onClick={() => setStatusFilter("All")}
              className="flex text-xs items-center font-sans gap-1 cursor-pointer text-primary border border-primary/20 px-3 py-1 rounded-2xl bg-primary/10 transition-colors no-underline hover:bg-primary/20 hover:text-primary"
            >
              Réinitialiser
            </button>
          )}
        </div>
      </div>

      <TableContainer
        component={Paper}
        variant="outlined"
        className="border! border-primary! rounded-2xl! overflow-hidden! bg-surface!"
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
            {filteredTalks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" className="py-8! text-text-muted!">
                  Aucun talk ne correspond à ces critères.
                </TableCell>
              </TableRow>
            ) : (
              filteredTalks.map((talk) => (
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
                  <TableCell>{talk.speakers[0]?.name ?? "—"}</TableCell>
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

      <TalkDetailsDialog
        talk={selectedTalk}
        open={!!selectedTalkId}
        onClose={() => setSelectedTalkId(null)}
        onUpdate={handleUpdate}
        onDelete={handleDelete}
      />
    </>
  );
}
