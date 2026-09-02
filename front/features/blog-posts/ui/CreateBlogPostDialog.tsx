"use client";

import { useState } from "react";
import { useTheme } from "next-themes";
import { DatePickerProvider } from "@/shared/ui";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import FormControl from "@mui/material/FormControl";
import FormHelperText from "@mui/material/FormHelperText";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import InputAdornment from "@mui/material/InputAdornment";
import IconButton from "@mui/material/IconButton";
import Box from "@mui/material/Box";
import { DatePicker } from "@mui/x-date-pickers";
import { ExternalLinkIcon, FileText, Library } from "lucide-react";
import dayjs, { type Dayjs } from "dayjs";
import "dayjs/locale/fr";
import type { BlogPostData } from "@/entities/post";
import { blogPostTags, blogPostStatusConfig, BLOG_POST_STATUSES } from "@/entities/post";
import { blogPostFormSchema, type BlogPostFormData } from "@/entities/post";
import { isValidUrl } from "@/shared/lib";

dayjs.locale("fr");

const DIALOG_TITLE_ID = "create-post-dialog-title";

interface CreateBlogPostDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (post: BlogPostData) => void;
}

export function CreateBlogPostDialog({ open, onClose, onSubmit }: CreateBlogPostDialogProps) {
  const [creationDate, setCreationDate] = useState<Dayjs | null>(null);
  const [expectedPublicationDate, setExpectedPublicationDate] = useState<Dayjs | null>(null);
  const [creationDateError, setCreationDateError] = useState<string | null>(null);
  const [pubDateError, setPubDateError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<BlogPostFormData>({
    resolver: zodResolver(blogPostFormSchema),
    defaultValues: {
      title: "", author: "", tags: [],
      status: "IDEA", link: "", googleDocDraftLink: "",
    },
  });

  // eslint-disable-next-line react-hooks/incompatible-library
  const link = watch("link");
  const googleDocDraftLink = watch("googleDocDraftLink");
  const currentStatus = watch("status");
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === "dark";
  const { text: statusColor, darkText: statusDarkColor } = blogPostStatusConfig[currentStatus];
  const appliedStatusColor = isDark ? statusDarkColor : statusColor;

  const handleClose = () => {
    reset();
    setCreationDate(null);
    setExpectedPublicationDate(null);
    setCreationDateError(null);
    setPubDateError(null);
    onClose();
  };

  const validateDates = (): boolean => {
    let valid = true;
    if (!creationDate) {
      setCreationDateError("La date de création est requise");
      valid = false;
    } else if (creationDate.isAfter(dayjs())) {
      setCreationDateError("La date de création ne peut pas être après aujourd'hui");
      valid = false;
    } else {
      setCreationDateError(null);
    }
    if (expectedPublicationDate && creationDate && expectedPublicationDate.isBefore(creationDate)) {
      setPubDateError("Doit être après la date de création");
      valid = false;
    } else {
      setPubDateError(null);
    }
    return valid;
  };

  const onSave = ({ author, ...data }: BlogPostFormData) => {
    if (!validateDates()) return;
    onSubmit({
      id: crypto.randomUUID(),
      ...data,
      writers: [author],
      creationDate: creationDate!.format("DD-MM-YYYY"),
      publicationDate: expectedPublicationDate?.format("DD-MM-YYYY") ?? "",
    });
    handleClose();
  };

  return (
    <DatePickerProvider>
    <Dialog
      onClose={handleClose}
      open={open}
      maxWidth="md"
      fullWidth
      aria-labelledby={DIALOG_TITLE_ID}
      slotProps={{ paper: { className: "dark:bg-slate-900 dark:bg-none" } }}
    >
      <DialogTitle
        id={DIALOG_TITLE_ID}
        className="pb-0! flex! justify-between! items-center!"
      >
        Nouvel article de blog
        <Controller
          name="status"
          control={control}
          render={({ field }) => (
            <FormControl size="small" className="min-w-30">
              <Select
                {...field}
                style={{ fontWeight: "bold", color: appliedStatusColor }}
                inputProps={{ "aria-label": "Statut de l'article" }}
              >
                {BLOG_POST_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {blogPostStatusConfig[s].label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        />
      </DialogTitle>

      <DialogContent>
        <p className="text-sm text-text-muted mb-6">Saisir un article de blog</p>

        <Box
          component="form"
          id="create-post-form"
          onSubmit={handleSubmit(onSave)}
          noValidate
          sx={{ display: "flex", flexDirection: "column", gap: 2 }}
        >
          <TextField
            {...register("title")}
            id="post-title"
            label="Titre du post"
            required
            fullWidth
            placeholder="Ex: Building Resilient Microservices"
            error={!!errors.title}
            helperText={errors.title?.message}
          />

          <div className="grid grid-cols-2 gap-4">
            <TextField
              {...register("author")}
              id="post-author"
              label="Auteur"
              required
              fullWidth
              placeholder="Prénom Nom"
              error={!!errors.author}
              helperText={errors.author?.message}
            />

            <Controller
              name="tags"
              control={control}
              render={({ field }) => (
                <FormControl fullWidth required error={!!errors.tags}>
                  <InputLabel id="create-post-tags-label">Tags</InputLabel>
                  <Select
                    {...field}
                    labelId="create-post-tags-label"
                    id="post-tags"
                    multiple
                    label="Tags"
                    renderValue={(sel) =>
                      (sel as string[]).map((v) => blogPostTags[v] ?? v).join(", ")
                    }
                  >
                    {Object.entries(blogPostTags).map(([v, l]) => (
                      <MenuItem key={v} value={v}>{l}</MenuItem>
                    ))}
                  </Select>
                  {errors.tags && (
                    <FormHelperText>{errors.tags.message}</FormHelperText>
                  )}
                </FormControl>
              )}
            />

            <DatePicker
              label="Date de création"
              value={creationDate}
              onChange={(val) => { setCreationDate(val); setCreationDateError(null); }}
              views={["year", "month", "day"]}
              format="DD/MM/YYYY"
              slotProps={{
                textField: {
                  fullWidth: true,
                  required: true,
                  id: "post-creation-date",
                  error: !!creationDateError,
                  helperText: creationDateError ?? undefined,
                },
              }}
            />

            <DatePicker
              label="Date de publication souhaitée"
              value={expectedPublicationDate}
              onChange={(val) => { setExpectedPublicationDate(val); setPubDateError(null); }}
              views={["year", "month", "day"]}
              format="DD/MM/YYYY"
              slotProps={{
                textField: {
                  fullWidth: true,
                  id: "post-publication-date",
                  error: !!pubDateError,
                  helperText: pubDateError ?? undefined,
                },
              }}
            />

            <TextField
              {...register("link")}
              id="post-zenika-link"
              label="Lien blog Zenika"
              placeholder="https://..."
              fullWidth
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <Library size={16} aria-hidden="true" />
                    </InputAdornment>
                  ),
                  endAdornment: link && isValidUrl(link) ? (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        component="a"
                        href={link}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label="Ouvrir le lien blog Zenika"
                        className="text-primary! p-0.5!"
                      >
                        <ExternalLinkIcon size={14} aria-hidden="true" />
                      </IconButton>
                    </InputAdornment>
                  ) : null,
                },
              }}
            />

            <TextField
              {...register("googleDocDraftLink")}
              id="post-gdoc-link"
              label="Lien draft Google Doc"
              placeholder="https://..."
              fullWidth
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <FileText size={16} aria-hidden="true" />
                    </InputAdornment>
                  ),
                  endAdornment: googleDocDraftLink && isValidUrl(googleDocDraftLink) ? (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        component="a"
                        href={googleDocDraftLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label="Ouvrir le lien Google Doc"
                        className="text-primary! p-0.5!"
                      >
                        <ExternalLinkIcon size={14} aria-hidden="true" />
                      </IconButton>
                    </InputAdornment>
                  ) : null,
                },
              }}
            />
          </div>
        </Box>
      </DialogContent>

      <DialogActions className="px-6! pb-6! pt-5!">
        <Button variant="outlined" onClick={handleClose}>Annuler</Button>
        <div className="flex-1" />
        <Button variant="contained" type="submit" form="create-post-form">
          Créer le post
        </Button>
      </DialogActions>
    </Dialog>
    </DatePickerProvider>
  );
}
