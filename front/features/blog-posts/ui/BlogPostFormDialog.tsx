"use client";

import { useEffect, useId, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, InputAdornment, MenuItem, TextField } from "@mui/material";
import { ExternalLink } from "lucide-react";
import { DatePicker } from "@mui/x-date-pickers";
import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";
import { DatePickerProvider } from "@/shared/ui";
import { isValidUrl } from "@/shared/lib";
import { agencyLabels } from "@/shared/lib/offices";
import { ContributorAutocomplete, type ContributorFormData } from "@/entities/user";
import { BlogPostStatus, Office } from "@/shared/api";
import { blogPostFormSchema, blogPostStatusConfig, blogPostTags, blogPostToday, type BlogPostData, type BlogPostFormData } from "@/entities/post";

dayjs.extend(customParseFormat);

interface BlogPostFormDialogProps {
  post?: BlogPostData;
  defaultAuthor?: ContributorFormData;
  defaultOffice?: Office | "";
  onClose: () => void;
  onSave: (post: BlogPostData) => void | Promise<unknown>;
  onDelete?: (id: string) => void | Promise<unknown>;
}

export function BlogPostFormDialog({ post, defaultAuthor, defaultOffice, onClose, onSave, onDelete }: BlogPostFormDialogProps) {
  const formId = useId();
  const titleId = useId();
  const [saveError, setSaveError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [writersEdited, setWritersEdited] = useState(false);
  const [officeEdited, setOfficeEdited] = useState(false);
  const { control, register, handleSubmit, setValue, formState: { errors, isSubmitting } } = useForm<BlogPostFormData>({
    resolver: zodResolver(blogPostFormSchema),
    defaultValues: {
      title: post?.title ?? "",
      writers: post?.writers ?? (defaultAuthor ? [defaultAuthor] : []),
      office: post?.office ?? defaultOffice ?? "",
      tags: post?.tags ?? [],
      status: post?.status ?? BlogPostStatus.IDEA,
      creationDate: post ? post.creationDate : blogPostToday(),
      publicationDate: post?.publicationDate ?? "",
      link: post?.link ?? "",
      googleDocDraftLink: post?.googleDocDraftLink ?? "",
      actualPublicationDate: post?.actualPublicationDate ?? "",
    },
  });
  const pending = isSubmitting || isDeleting;
  const status = useWatch({ control, name: "status" });

  useEffect(() => {
    if (post) return;
    if (defaultAuthor && !writersEdited) {
      setValue("writers", [defaultAuthor]);
    }
  }, [post, defaultAuthor, writersEdited, setValue]);

  useEffect(() => {
    if (post) return;
    if (defaultOffice && !officeEdited) {
      setValue("office", defaultOffice);
    }
  }, [post, defaultOffice, officeEdited, setValue]);

  const save = handleSubmit(async (values) => {
    setSaveError(null);
    try {
      await onSave({ ...post, ...values, id: post?.id ?? crypto.randomUUID(),
        writers: values.writers.map(writer => ({ name: writer.name, email: writer.email?.trim() || undefined })),
      });
      onClose();
    } catch {
      setSaveError("L’enregistrement a échoué. Vos saisies sont conservées ; vous pouvez réessayer.");
    }
  });

  return <DatePickerProvider>
    <Dialog open onClose={pending ? undefined : onClose} maxWidth="md" fullWidth aria-labelledby={titleId}>
      <DialogTitle id={titleId}>{post ? "Détails de l’article de blog" : "Nouvel article de blog"}</DialogTitle>
      <DialogContent>
        <form id={formId} onSubmit={save} noValidate className="flex flex-col gap-4 pt-2">
          <Controller name="status" control={control} render={({ field }) =>
            <TextField {...field} select label="Statut" disabled={pending}>
              {BlogPostStatus.map(status => <MenuItem key={status} value={status}>{blogPostStatusConfig[status].label}</MenuItem>)}
            </TextField>} />
          <TextField {...register("title")} label="Titre du post" required fullWidth disabled={pending}
            error={!!errors.title} helperText={errors.title?.message} />
          <Controller name="writers" control={control} render={({ field }) =>
            <ContributorAutocomplete label="Auteurs" required value={field.value}
              onChange={value => { setWritersEdited(true); field.onChange(value); }} disabled={pending}
              placeholder="Rechercher ou saisir un auteur..." addAnotherPlaceholder="Ajouter un autre auteur..."
              externalLabel="— Auteur sans email (texte libre)" error={!!errors.writers}
              helperText={errors.writers?.message ?? errors.writers?.root?.message} />} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Controller name="office" control={control} render={({ field }) =>
              <TextField {...field} select label="Agence" disabled={pending}
                onChange={event => { setOfficeEdited(true); field.onChange(event); }}>
                <MenuItem value="">Non renseignée</MenuItem>
                {Office.map(office => <MenuItem key={office} value={office}>{agencyLabels[office]}</MenuItem>)}
              </TextField>} />
            <Controller name="tags" control={control} render={({ field }) =>
              <TextField select label="Tags" value={field.value} disabled={pending}
                onChange={event => field.onChange(typeof event.target.value === "string" ? event.target.value.split(",") : event.target.value)}
                slotProps={{ select: { multiple: true } }}>
                {Object.entries(blogPostTags).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}
              </TextField>} />
            {([ ["creationDate", "Date de création"], ["publicationDate", "Date de publication prévue"], ["actualPublicationDate", "Date de publication réelle"] ] as const).map(([name, label]) =>
              <Controller key={name} name={name} control={control} render={({ field }) =>
                <DatePicker label={label} value={field.value ? dayjs(field.value, "DD-MM-YYYY", true) : null}
                  onChange={date => field.onChange(date ? date.format("DD-MM-YYYY") : "")}
                  format="DD/MM/YYYY" disabled={pending}
                  slotProps={{ field: { clearable: true }, textField: { fullWidth: true, required: name === "actualPublicationDate" && status === "PUBLISHED", error: !!errors[name], helperText: errors[name]?.message } }} />} />)}
            {([
              ["link", "URL publique", status === "PUBLISHED"],
              ["googleDocDraftLink", "Lien du texte à relire", status === "REVIEW" || status === "READY_TO_PUBLISH"],
            ] as const).map(([name, label, required]) => <Controller key={name} name={name} control={control} render={({ field }) =>
              <TextField {...field} label={label} fullWidth disabled={pending} required={required}
                error={!!errors[name]} helperText={errors[name]?.message}
                slotProps={{ input: { endAdornment: isValidUrl(field.value) && <InputAdornment position="end">
                  <IconButton component="a" href={field.value} target="_blank" rel="noopener noreferrer"
                    aria-label={`Ouvrir : ${label}`} size="small"><ExternalLink size={16} /></IconButton>
                </InputAdornment> } }} />} />)}
          </div>
          {saveError && <Alert severity="error">{saveError}</Alert>}
        </form>
      </DialogContent>
      <DialogActions className="px-6! pb-6!">
        {post && onDelete && <Button color="error" disabled={pending} onClick={async () => {
          if (!window.confirm("Êtes-vous sûr de vouloir supprimer ce post ?")) return;
          setSaveError(null);
          setIsDeleting(true);
          try { await onDelete(post.id); onClose(); }
          catch { setSaveError("La suppression a échoué. Vous pouvez réessayer."); }
          finally { setIsDeleting(false); }
        }}>Supprimer</Button>}
        <div className="flex-1" />
        <Button onClick={onClose} disabled={pending}>Annuler</Button>
        <Button type="submit" form={formId} variant="contained" disabled={pending}>{post ? "Enregistrer" : "Créer le post"}</Button>
      </DialogActions>
    </Dialog>
  </DatePickerProvider>;
}
