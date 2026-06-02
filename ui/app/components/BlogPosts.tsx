import React from "react";
import {
  Box,
  Dialog,
  Typography,
  Button,
  Paper,
  Stack,
  useTheme,
  DialogTitle,
  DialogContent,
  Grid,
  TextField,
  DialogActions,
  Snackbar,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  InputAdornment,
  IconButton,
} from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers";
import { ExternalLinkIcon, FileText, Library, PenLine } from "lucide-react";
import { isValidUrl } from "~/lib/utils";
import { type BlogPostData, blogPostTags } from "~/types/post";
import { usePosts } from "~/hooks/usePosts";
import type { Dayjs } from "dayjs";
import dayjs from "dayjs";
import "dayjs/locale/fr";
import { BlogPostDetailsDialog } from "./BlogPostDetailsDialog";

const statusConfig: Record<
  "Draft" | "Published",
  { text: string; bg: string }
> = {
  Draft: { text: "#757575", bg: "rgba(117, 117, 117, 0.12)" },
  Published: { text: "#21c45d", bg: "rgba(33, 196, 93, 0.12)" },
};

function StatusTag({ status }: { status: "Draft" | "Published" }) {
  const config = statusConfig[status];
  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.5,
        borderRadius: 1,
        display: "inline-block",
        fontSize: "0.75rem",
        fontWeight: "bold",
        border: `1px solid ${config.bg}`,
        color: config.text,
        backgroundColor: config.bg,
      }}
    >
      {status}
    </Box>
  );
}

interface CreateBlogPostProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (post: BlogPostData) => void;
}

export function CreateBlogPostDialog({
  open,
  onClose,
  onSubmit,
}: CreateBlogPostProps) {
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

  const theme = useTheme();

  const resetForm = () => {
    setTitle("");
    setAuthor("");
    setCreationDate(null);
    setExpectedPublicationDate(null);
    setTags([]);
    setStatus("Draft");
  };

  const handleSave = (status: "Draft" | "Published") => {
    const requiredFieldsMissing =
      !title.trim() ||
      !author.trim() ||
      !creationDate ||
      tags.length === 0 ||
      !status;

    // TODO RENAME / refacto
    const dateProblem =
      expectedPublicationDate && expectedPublicationDate.isBefore(creationDate);

    const creationDateCannotBeAfterToday =
      creationDate && creationDate.isAfter(dayjs());

    if (
      requiredFieldsMissing ||
      dateProblem ||
      creationDateCannotBeAfterToday
    ) {
      setToastOpen(true);
      return;
    }

    onSubmit({
      id: crypto.randomUUID(),
      title: title.trim(),
      author: author.trim(),
      creationDate: creationDate ? creationDate.format("DD-MM-YYYY") : "",
      expectedPublicationDate: expectedPublicationDate
        ? expectedPublicationDate.format("DD-MM-YYYY")
        : "",
      tags,
      zenikaBlogLink,
      googleDocDraftLink,
      status: status,
    });
    resetForm();
    onClose();
  };

  const handleSubmit = () => handleSave(status);
  const handleCancel = () => {
    resetForm();
    onClose();
  };

  const handleToastClose = (
    _event?: React.SyntheticEvent | Event,
    reason?: string,
  ) => {
    if (reason === "clickaway") return;
    setToastOpen(false);
  };

  return (
    <Dialog onClose={handleCancel} open={open} maxWidth="md" fullWidth>
      <DialogTitle
        sx={{
          pb: 0,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        Nouvel article de blog
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
      <DialogContent sx={{ pt: 2 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Saisir un article de blog
        </Typography>

        <Grid container spacing={2}>
          <Grid size={12}>
            <TextField
              label="Titre du post"
              id="post-title"
              required
              fullWidth
              placeholder="Ex: Building Resilient Microservices"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <TextField
              label="Auteur"
              id="author"
              required
              fullWidth
              placeholder="Prénom Nom"
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
            />
          </Grid>

          <Grid size={6}>
            <FormControl fullWidth required>
              <InputLabel id="select-tags-label">Tags</InputLabel>
              <Select
                labelId="select-tags-label"
                id="select-tags"
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
          {/* </Grid>

        <Grid container spacing={2}> */}
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
                          sx={{
                            color: "primary.main",
                            p: 0.5,
                            "&:hover": {
                              backgroundColor: "rgba(237, 33, 60, 0.08)",
                            },
                          }}
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
                          sx={{
                            color: "primary.main",
                            p: 0.5,
                            "&:hover": {
                              backgroundColor: "rgba(237, 33, 60, 0.08)",
                            },
                          }}
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
        <Button variant="outlined" onClick={handleCancel}>
          Annuler
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="contained" onClick={handleSubmit}>
          Créer le post
        </Button>
      </DialogActions>

      <Snackbar
        open={toastOpen}
        autoHideDuration={4000}
        onClose={handleToastClose}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert onClose={handleToastClose} severity="error" variant="filled">
          {expectedPublicationDate &&
          expectedPublicationDate.isBefore(creationDate)
            ? "La date de publication doit être supérieure à la date de création."
            : creationDate && creationDate.isAfter(dayjs())
              ? "La date de création ne peut pas être après aujourd'hui."
              : "Merci de remplir tous les champs obligatoires."}
        </Alert>
      </Snackbar>
    </Dialog>
  );
}

//
//
//
//
//

const BlogPosts: React.FC = () => {
  const [open, setOpen] = React.useState(false);
  const [selectedPostId, setSelectedPostId] = React.useState<string | null>(
    null,
  );

  const theme = useTheme();

  const { posts, loading, createPost, updatePost, deletePost } = usePosts();

  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  const handleSubmit = async (post: BlogPostData) => {
    try {
      await createPost(post);
    } catch (error) {
      alert("Erreur lors de la création du post");
    }
  };

  const handleUpdateBlogPost = async (updatedPost: BlogPostData) => {
    try {
      await updatePost(updatedPost);
    } catch (error) {
      alert("Erreur lors de la mise à jour du post");
    }
  };

  const handleDeleteBlogPost = async (id: string) => {
    try {
      await deletePost(id);
    } catch (error) {
      alert("Erreur lors de la suppression du post");
    }
  };

  const selectedPost = posts.find((p) => p.id === selectedPostId) || null;

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
            Blog Posts
          </Typography>
          <Typography
            variant="body2"
            sx={{ color: theme.palette.text.secondary }}
          >
            Manage Zenika blog articles and publications.
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
          <PenLine size={16} />
          New Post
        </Button>
      </Box>

      <Stack spacing={2}>
        {posts.map((post) => (
          <Paper
            key={post.id}
            variant="outlined"
            sx={{
              p: 2,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              border: "1px solid",
              borderColor: "#ed213c",
              borderRadius: 1,
              transition: "border-color 0.2s ease-in-out",
              cursor: "pointer",
            }}
            onClick={() => setSelectedPostId(post.id)}
          >
            <Box sx={{ display: "flex", flexDirection: "column" }}>
              <Typography
                variant="subtitle1"
                sx={{ fontWeight: "bold", color: "#1e293b" }}
              >
                {post.title}
              </Typography>
              <Typography variant="body2" sx={{ color: "#64748b" }}>
                {post.author} • {post.creationDate}
              </Typography>
              <Box sx={{ display: "flex", gap: 1, mt: 0.5 }}>
                {post.tags.slice(0, 5).map((tag) => (
                  <Box
                    key={tag}
                    sx={{
                      px: 1,
                      py: 0.25,
                      borderRadius: 1,
                      fontSize: "0.7rem",
                      backgroundColor: "#f1f5f9",
                      color: "#475569",
                      fontWeight: "medium",
                    }}
                  >
                    {tag}
                  </Box>
                ))}
              </Box>
            </Box>
            <Box>
              <StatusTag status={post.status} />
            </Box>
          </Paper>
        ))}
        {posts.length === 0 && (
          <Paper
            variant="outlined"
            sx={{
              p: 4,
              textAlign: "center",
              color: "text.secondary",
              borderRadius: 1,
            }}
          >
            Aucun article de blog pour le moment. Créez-en un avec "New Post" !
          </Paper>
        )}
      </Stack>
      <CreateBlogPostDialog
        open={open}
        onClose={handleClose}
        onSubmit={handleSubmit}
      />

      <BlogPostDetailsDialog
        post={selectedPost}
        open={!!selectedPostId}
        onClose={() => setSelectedPostId(null)}
        onUpdate={handleUpdateBlogPost}
        onDelete={handleDeleteBlogPost}
      />
    </Box>
  );
};

export default BlogPosts;
