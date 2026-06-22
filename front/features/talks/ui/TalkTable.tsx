"use client";

import { useState } from "react";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import { Eye } from "lucide-react";
import type { TalkData } from "@/entities/talk";
import { agencyLabels } from "@/entities/talk";
import dynamic from "next/dynamic";
import { useTalks } from "@/features/talks/model";
import { StatusTag, VisibilityTag } from "./TalkTags";

const TalkDetailsDialog = dynamic(
  () => import("./TalkDetailsDialog").then((m) => ({ default: m.TalkDetailsDialog })),
  { ssr: false }
);

export function TalkTable() {
  const [selectedTalkId, setSelectedTalkId] = useState<string | null>(null);
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

  return (
    <>
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
            {talks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" className="py-8! text-text-muted!">
                  Aucun talk pour le moment. Créez-en un avec &quot;New Talk&quot; !
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
                  <TableCell>{talk.speaker}</TableCell>
                  <TableCell>{agencyLabels[talk.agency] || "—"}</TableCell>
                  <TableCell>{talk.conference || "—"}</TableCell>
                  <TableCell><StatusTag status={talk.status} /></TableCell>
                  <TableCell align="center"><VisibilityTag visibility={talk.visibility} /></TableCell>
                  <TableCell align="center">
                    {(talk.status === "Accepted" || talk.status === "Replayed") && (
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
