"use client";

import { useState, useEffect } from "react";
import { useTheme } from "next-themes";
import { DatePickerProvider } from "@/shared/ui";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import Snackbar from "@mui/material/Snackbar";
import Alert from "@mui/material/Alert";
import { DatePicker } from "@mui/x-date-pickers";
import { ExternalLinkIcon, FileText, Library, Trash2 } from "lucide-react";
import dayjs, { type Dayjs } from "dayjs";
import type { BlogPostData, BlogPostStatus } from "@/entities/post";
import { blogPostTags, blogPostStatusConfig } from "@/entities/post";
import { isValidUrl } from "@/shared/lib";

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
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [creationDate, setCreationDate] = useState<Dayjs | null>(null);
  const [expectedPublicationDate, setExpectedPublicationDate] =
    useState<Dayjs | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [status, setStatus] = useState<BlogPostStatus>("Idea");
  const [zenikaBlogLink, setZenikaBlogLink] = useState("");
  const [googleDocDraftLink, setGoogleDocDraftLink] = useState("");
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState("");

  useEffect(() => {
    if (post && open) {
      /* eslint-disable react-hooks/set-state-in-effect */
      setTitle(post.title ?? "");
      setAuthor(post.author ?? "");
      setCreationDate(
        post.creationDate ? dayjs(post.creationDate, "DD-MM-YYYY") : null,
      );
      setExpectedPublicationDate(
        post.expectedPublicationDate
          ? dayjs(post.expectedPublicationDate, "DD-MM-YYYY")
          : null,
      );
      setTags(post.tags ?? []);
      setStatus(post.status ?? "Idea");
      setZenikaBlogLink(post.zenikaBlogLink ?? "");
      setGoogleDocDraftLink(post.googleDocDraftLink ?? "");
      /* eslint-enable react-hooks/set-state-in-effect */
    }
  }, [post, open]);

  const handleSave = () => {
    if (!title.trim() || !author.trim() || !creationDate || tags.length === 0) {
      setToastMsg("Merci de remplir tous les champs obligatoires.");
      setToastOpen(true);
      return;
    }
    if (expectedPublicationDate?.isBefore(creationDate)) {
      setToastMsg(
        "La date de publication doit être supérieure à la date de création.",
      );
      setToastOpen(true);
      return;
    }
    if (creationDate.isAfter(dayjs())) {
      setToastMsg("La date de création ne peut pas être après aujourd'hui.");
      setToastOpen(true);
      return;
    }
    if (post) {
      onUpdate({
        ...post,
        title: title.trim(),
        author: author.trim(),
        creationDate: creationDate.format("DD-MM-YYYY"),
        expectedPublicationDate:
          expectedPublicationDate?.format("DD-MM-YYYY") ?? "",
        tags,
        status,
        zenikaBlogLink,
        googleDocDraftLink,
      });
      onClose();
    }
  };

  const handleDelete = () => {
    if (
      post &&
      window.confirm("Êtes-vous sûr de vouloir supprimer ce post ?")
    ) {
      onDelete(post.id);
      onClose();
    }
  };

  const { resolvedTheme } = useTheme();

  if (!post) return null;

  const isDark = resolvedTheme === "dark";
  const {
    text: statusColor,
    bg: statusBg,
    darkText: statusDarkColor,
    darkBg: statusDarkBg,
  } = blogPostStatusConfig[status];
  const appliedStatusColor = isDark ? statusDarkColor : statusColor;
  const appliedStatusBg = isDark ? statusDarkBg : statusBg;

  return (
    <DatePickerProvider>
      <Dialog onClose={onClose} open={open} maxWidth="md" fullWidth>
        <DialogTitle className="pb-0! flex! justify-between! items-center!">
          Détails de l&apos;article de blog
          <FormControl size="small" className="min-w-30">
            <Select
              value={status}
              onChange={(e) => setStatus(e.target.value as BlogPostStatus)}
              style={{ fontWeight: "bold", color: appliedStatusColor }}
              slotProps={{ input: { style: { borderColor: appliedStatusBg } } }}
            >
              <MenuItem value="Idea">Idea</MenuItem>
              <MenuItem value="Draft">Draft</MenuItem>
              <MenuItem value="Review">Review</MenuItem>
              <MenuItem value="Published">Published</MenuItem>
            </Select>
          </FormControl>
        </DialogTitle>

        <DialogContent>
          <p className="text-sm text-text-muted mb-6">
            Modifier les détails de l&apos;article
          </p>

          <div className="flex flex-col gap-4">
            <TextField
              label="Titre du post"
              required
              fullWidth
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />

            <div className="grid grid-cols-2 gap-4">
              <TextField
                label="Auteur"
                required
                fullWidth
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
              />

              <FormControl fullWidth required>
                <InputLabel id="edit-post-tags-label">Tags</InputLabel>
                <Select
                  labelId="edit-post-tags-label"
                  multiple
                  value={tags}
                  label="Tags"
                  onChange={(e) => {
                    const v = e.target.value;
                    setTags(
                      typeof v === "string" ? v.split(",") : (v as string[]),
                    );
                  }}
                  renderValue={(sel) =>
                    sel.map((v) => blogPostTags[v] ?? v).join(", ")
                  }
                >
                  {Object.entries(blogPostTags).map(([v, l]) => (
                    <MenuItem key={v} value={v}>
                      {l}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <DatePicker
                label="Date"
                value={creationDate}
                onChange={setCreationDate}
                views={["year", "month", "day"]}
                format="DD/MM/YYYY"
                slotProps={{ textField: { fullWidth: true, required: true } }}
              />

              <DatePicker
                label="Date de publication souhaitée"
                value={expectedPublicationDate}
                onChange={setExpectedPublicationDate}
                views={["year", "month", "day"]}
                format="DD/MM/YYYY"
                slotProps={{ textField: { fullWidth: true } }}
              />

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
                            component="a"
                            href={zenikaBlogLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary! p-0.5!"
                          >
                            <ExternalLinkIcon size={14} />
                          </IconButton>
                        </InputAdornment>
                      ) : null,
                  },
                }}
              />

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
                            component="a"
                            href={googleDocDraftLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary! p-0.5!"
                          >
                            <ExternalLinkIcon size={14} />
                          </IconButton>
                        </InputAdornment>
                      ) : null,
                  },
                }}
              />
            </div>
          </div>
        </DialogContent>

        <DialogActions className="px-6! pb-6! pt-5!">
          <Button
            variant="outlined"
            color="error"
            onClick={handleDelete}
            startIcon={<Trash2 size={16} />}
          >
            Supprimer
          </Button>
          <div className="flex-1" />
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
          onClose={() => setToastOpen(false)}
          anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        >
          <Alert
            onClose={() => setToastOpen(false)}
            severity="error"
            variant="filled"
          >
            {toastMsg}
          </Alert>
        </Snackbar>
      </Dialog>
    </DatePickerProvider>
  );
}
