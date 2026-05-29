import React, { useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  InputAdornment,
  IconButton,
  Typography,
  Box,
  Snackbar,
  Alert,
} from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers";
import { ExternalLinkIcon, FileText, Library, Trash2 } from "lucide-react";
import dayjs, { type Dayjs } from "dayjs";
import { type BlogPostData, blogPostTags } from "~/types/post";
import { isValidUrl } from "~/lib/utils";

interface BlogPostDetailsDialogProps {
  post: BlogPostData | null;
  open: boolean;
  onClose: () => void;
  onUpdate: (post: BlogPostData) => void;
  onDelete: (id: string) => void;
}

export function BlogPostDetailsDialog({
  post,
  open,
  onClose,
  onUpdate,
  onDelete,
}: BlogPostDetailsDialogProps) {
  const [title, setTitle] = React.useState("");
  const [author, setAuthor] = React.useState("");
  const [creationDate, setCreationDate] = React.useState<Dayjs | null>(null);
  const [expectedPublicationDate, setExpectedPublicationDate] =
    React.useState<Dayjs | null>(null);
  const [tags, setTags] = React.useState<string[]>([]);
  const [status, setStatus] = React.useState<"Draft" | "Published">("Draft");
  const [zenikaBlogLink, setZenikaBlogLink] = React.useState("");
  const [googleDocDraftLink, setGoogleDocDraftLink] = React.useState("");
  const [toastOpen, setToastOpen] = React.useState(false);

  useEffect(() => {
    if (post && open) {
      setTitle(post.title || "");
      setAuthor(post.author || "");
      setCreationDate(
        post.creationDate ? dayjs(post.creationDate, "DD-MM-YYYY") : null,
      );
      setExpectedPublicationDate(
        post.expectedPublicationDate
          ? dayjs(post.expectedPublicationDate, "DD-MM-YYYY")
          : null,
      );
      setTags(post.tags || []);
      setStatus(post.status || "Draft");
      setZenikaBlogLink(post.zenikaBlogLink || "");
      setGoogleDocDraftLink(post.googleDocDraftLink || "");
    }
  }, [post, open]);

  const handleSave = () => {
    const requiredFieldsMissing =
      !title.trim() ||
      !author.trim() ||
      !creationDate ||
      tags.length === 0 ||
      !status;

    if (requiredFieldsMissing) {
      setToastOpen(true);
      return;
    }

    if (post) {
      onUpdate({
        ...post,
        title: title.trim(),
        author: author.trim(),
        creationDate: creationDate ? creationDate.format("DD-MM-YYYY") : "",
        expectedPublicationDate: expectedPublicationDate
          ? expectedPublicationDate.format("DD-MM-YYYY")
          : "",
        tags,
        status,
        zenikaBlogLink,
        googleDocDraftLink,
      });
      onClose();
    }
  };

  const handleDelete = () => {
    if (post) {
      if (window.confirm("Êtes-vous sûr de vouloir supprimer ce post ?")) {
        onDelete(post.id);
        onClose();
      }
    }
  };

  const handleToastClose = (
    _event?: React.SyntheticEvent | Event,
    reason?: string,
  ) => {
    if (reason === "clickaway") return;
    setToastOpen(false);
  };

  if (!post) return null;

  return (
    <Dialog onClose={onClose} open={open} maxWidth="md" fullWidth>
      <DialogTitle
        sx={{
          pb: 0,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        Détails de l'article de blog
        <Box>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <Select
              value={status}
              onChange={(e) =>
                setStatus(e.target.value as "Draft" | "Published")
              }
              sx={{
                fontWeight: "bold",
                color: status === "Draft" ? "#757575" : "#21c45d",
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor:
                    status === "Draft"
                      ? "rgba(117, 117, 117, 0.5)"
                      : "rgba(33, 196, 93, 0.5)",
                },
              }}
            >
              <MenuItem value="Draft">Draft</MenuItem>
              <MenuItem value="Published">Published</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Modifier les détails de l'article
        </Typography>
        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField
              label="Titre du post"
              required
              fullWidth
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Auteur"
              required
              fullWidth
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <FormControl fullWidth required>
              <InputLabel id="edit-select-tags-label">Tags</InputLabel>
              <Select
                labelId="edit-select-tags-label"
                multiple
                fullWidth
                value={tags}
                label="Tags"
                onChange={(e) => {
                  const value = e.target.value;
                  setTags(
                    typeof value === "string"
                      ? value.split(",")
                      : (value as string[]),
                  );
                }}
                renderValue={(selected) =>
                  selected.map((val) => blogPostTags[val] || val).join(", ")
                }
              >
                {Object.entries(blogPostTags).map(([value, label]) => (
                  <MenuItem key={value} value={value}>
                    {label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid size={6}>
            <DatePicker
              label="Date"
              value={creationDate}
              onChange={(newValue) => setCreationDate(newValue)}
              views={["year", "month", "day"]}
              format="DD/MM/YYYY"
              slotProps={{ textField: { fullWidth: true, required: true } }}
            />
          </Grid>

          <Grid size={6}>
            <DatePicker
              label="Date de publication souhaitée"
              value={expectedPublicationDate}
              onChange={(newValue) => setExpectedPublicationDate(newValue)}
              views={["year", "month", "day"]}
              format="DD/MM/YYYY"
              slotProps={{ textField: { fullWidth: true } }}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Lien blog Zenika"
              placeholder="https://..."
              value={zenikaBlogLink}
              onChange={(e) => setZenikaBlogLink(e.target.value)}
              fullWidth
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <Library size={16} />
                    </InputAdornment>
                  ),
                  endAdornment:
                    zenikaBlogLink && isValidUrl(zenikaBlogLink) ? (
                      <InputAdornment position="end">
                        <IconButton
                          size="small"
                          href={zenikaBlogLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          sx={{ color: "primary.main", p: 0.5 }}
                        >
                          <ExternalLinkIcon size={14} />
                        </IconButton>
                      </InputAdornment>
                    ) : null,
                },
              }}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Lien draft Google Doc"
              placeholder="https://..."
              value={googleDocDraftLink}
              onChange={(e) => setGoogleDocDraftLink(e.target.value)}
              fullWidth
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <FileText size={16} />
                    </InputAdornment>
                  ),
                  endAdornment:
                    googleDocDraftLink && isValidUrl(googleDocDraftLink) ? (
                      <InputAdornment position="end">
                        <IconButton
                          size="small"
                          href={googleDocDraftLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          sx={{ color: "primary.main", p: 0.5 }}
                        >
                          <ExternalLinkIcon size={14} />
                        </IconButton>
                      </InputAdornment>
                    ) : null,
                },
              }}
            />
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button
          variant="outlined"
          color="error"
          onClick={handleDelete}
          startIcon={<Trash2 size={16} />}
        >
          Supprimer
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="outlined" onClick={onClose}>
          Annuler
        </Button>
        <Button variant="contained" onClick={handleSave}>
          Enregistrer
        </Button>
      </DialogActions>

      <Snackbar
        open={toastOpen}
        autoHideDuration={4000}
        onClose={handleToastClose}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert onClose={handleToastClose} severity="error" variant="filled">
          {"Merci de remplir tous les champs obligatoires."}
        </Alert>
      </Snackbar>
    </Dialog>
  );
}
