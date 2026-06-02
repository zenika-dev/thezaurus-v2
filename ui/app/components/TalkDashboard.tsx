import * as React from "react";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import { useTheme } from "@mui/material/styles";
import { Plus, Eye } from "lucide-react";

import { type TalkData, agencyLabels } from "../types/talk";
import { useTalks } from "../hooks/useTalks";
import { StatusTag, VisibilityTag } from "./TalkTags";
import { CreateTalkDialog } from "./CreateTalkDialog";
import { TalkDetailsDialog } from "./TalkDetailsDialog";

export default function TalkDashboard() {
  const [open, setOpen] = React.useState(false);
  const [selectedTalkId, setSelectedTalkId] = React.useState<string | null>(
    null,
  );

  const { talks, loading, createTalk, updateTalk, deleteTalk } = useTalks();

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const handleSubmit = async (talk: TalkData) => {
    try {
      await createTalk(talk);
    } catch (error) {
      alert("Erreur lors de la création du talk");
    }
  };

  const handleUpdateTalk = async (updatedTalk: TalkData) => {
    try {
      await updateTalk(updatedTalk);
    } catch (error) {
      alert("Erreur lors de la mise à jour du talk");
    }
  };

  const handleDeleteTalk = async (id: string) => {
    try {
      await deleteTalk(id);
    } catch (error) {
      alert("Erreur lors de la suppression du talk");
    }
  };

  const selectedTalk = talks.find((t) => t.id === selectedTalkId) || null;
  const theme = useTheme();

  return (
    <Box sx={{ p: 4 }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Box>
          <Typography variant="h4" sx={{ fontWeight: "bold" }}>
            Talks
          </Typography>
          <Typography
            variant="body2"
            sx={{ color: theme.palette.text.secondary }}
          >
            Manage the lifecycle of talks from idea to replay.
          </Typography>
        </Box>
        <Button
          variant="contained"
          onClick={handleOpen}
          sx={{
            fontWeight: "bold",
            py: 1,
            gap: 1,
            boxShadow: "none",
            borderRadius: "20px",
            background: "linear-gradient(135deg, #ed213c 0%, #BF1D67 100%)",
            transition:
              "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
            "&:hover": {
              boxShadow: "none",
              transform: "translateY(-2px)",
            },
          }}
        >
          <Plus size={16} />
          New Talk
        </Button>
      </Box>

      <TableContainer
        component={Paper}
        variant="outlined"
        sx={{
          border: "1px solid #ed213c",
          overflow: "hidden",
          borderRadius: 1,
        }}
      >
        <Table>
          <TableHead sx={{ backgroundColor: "#ececec" }}>
            <TableRow>
              <TableCell>
                <strong>Titre</strong>
              </TableCell>
              <TableCell>
                <strong>Speaker</strong>
              </TableCell>
              <TableCell>
                <strong>Agence</strong>
              </TableCell>
              <TableCell>
                <strong>Conférence</strong>
              </TableCell>
              <TableCell>
                <strong>Statut</strong>
              </TableCell>
              <TableCell align="center">
                <strong>Visibilité</strong>
              </TableCell>
              <TableCell align="center">
                <strong>Actions</strong>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody sx={{ "& tr:last-child td": { borderBottom: 0 } }}>
            {talks.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={9}
                  align="center"
                  sx={{ py: 4, color: "text.secondary" }}
                >
                  Aucun talk pour le moment. Créez-en-un avec "New Talk" !
                </TableCell>
              </TableRow>
            ) : (
              talks.map((talk) => (
                <TableRow key={talk.id} hover>
                  <TableCell>
                    <Typography
                      variant="body2"
                      sx={{
                        cursor: "pointer",
                        color: "primary.main",
                        textDecoration: "underline",
                        fontWeight: "medium",
                        "&:hover": { color: "primary.dark" },
                      }}
                      onClick={() => setSelectedTalkId(talk.id)}
                    >
                      {talk.title}
                    </Typography>
                  </TableCell>
                  <TableCell>{talk.speaker}</TableCell>
                  <TableCell>{agencyLabels[talk.agency] || "—"}</TableCell>
                  <TableCell>{talk.conference || "—"}</TableCell>
                  <TableCell>
                    <StatusTag status={talk.status} />
                  </TableCell>
                  <TableCell align="center">
                    <VisibilityTag visibility={talk.visibility} />
                  </TableCell>
                  <TableCell align="center">
                    {(talk.status === "Accepted" ||
                      talk.status === "Replayed") && (
                      <Box
                        onClick={() => setSelectedTalkId(talk.id)}
                        sx={{
                          px: 1.5,
                          py: 0.8,
                          borderRadius: 0.5,
                          display: "inline-flex",
                          alignItems: "center",
                          gap: 0.5,
                          fontSize: "0.75rem",
                          lineHeight: 1,
                          color: "text.primary",
                          backgroundColor: "common.white",
                          border: "1px solid",
                          borderColor: "divider",
                          cursor: "pointer",
                          "&:hover": {
                            backgroundColor: "rgba(237, 33, 60, 0.12)",
                            color: "primary.main",
                          },
                        }}
                      >
                        <Eye size={12} />
                        <span>Détail</span>
                      </Box>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <CreateTalkDialog
        open={open}
        onClose={handleClose}
        onSubmit={handleSubmit}
      />

      <TalkDetailsDialog
        talk={selectedTalk}
        open={!!selectedTalkId}
        onClose={() => setSelectedTalkId(null)}
        onUpdate={handleUpdateTalk}
        onDelete={handleDeleteTalk}
      />
    </Box>
  );
}

