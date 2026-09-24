"use client";

import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "@/entities/user";
import { queryKeys, type BackendConference, type BackendTalkReviewResponse, type TalkStatus } from "@/shared/api";
import { useSession } from "next-auth/react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button } from "@mui/material";
import dayjs from "dayjs";
import { type TalkData, talkFormSchema, type TalkFormData, type SpeakerFormData, reviewTalkAction } from "@/entities/talk";
import { TalkAssistantDialog } from "./TalkAssistantDialog";
import { TalkFormDialog } from "./TalkFormDialog";

interface CreateTalkDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (talk: TalkData) => Promise<TalkData>;
}

export function CreateTalkDialog({ open, onClose, onSubmit }: CreateTalkDialogProps) {
  const { data: session } = useSession();
  const { data: profile } = useQuery({ queryKey: queryKeys.profile.me(), queryFn: profileApi.getProfile, enabled: open });
  const [selectedConference, setSelectedConference] = useState<BackendConference | null>(null);
  const [officeEdited, setOfficeEdited] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [date, setDate] = useState("");

  const [assistantDialogOpen, setAssistantDialogOpen] = useState(false);
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [assistantResult, setAssistantResult] = useState<BackendTalkReviewResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const defaultSpeakers: SpeakerFormData[] = session?.user?.name
    ? [{ name: session.user.name, email: session.user.email ?? "" }]
    : [];

  const {
    handleSubmit,
    control,
    trigger,
    getValues,
    setValue,
    reset,
    clearErrors,
    setError: setFieldError,
    formState: { errors, isSubmitted },
  } = useForm<TalkFormData>({
    resolver: zodResolver(talkFormSchema),
    defaultValues: {
      title: "",
      speakers: defaultSpeakers,
      office: profile?.office ?? "",
      description: "",
      format: "",
      visibility: "PRIVATE",
      language: "francais",
      conference: "",
      notes: "",
    },
  });

  useEffect(() => {
    if (open && session?.user?.name) {
      const current = getValues("speakers");
      if (!current || current.length === 0) {
        setValue("speakers", [{ name: session.user.name, email: session.user.email ?? "" }]);
      }
    }
  }, [open, session?.user?.name, session?.user?.email, setValue, getValues]);

  useEffect(() => {
    if (open && !officeEdited) setValue("office", profile?.office ?? "");
  }, [open, profile?.office, officeEdited, setValue]);

  const buildTalkData = (
    { speakers, conference, ...data }: TalkFormData,
    status: TalkStatus,
  ): TalkData => ({
    id: crypto.randomUUID(),
    ...data,
    speakers: speakers.map((speaker) => ({
      name: speaker.name,
      email: speaker.email?.trim() || undefined,
    })),
    conference: selectedConference ?? (conference.trim() ? { name: conference.trim() } : null),
    date,
    status,
    slides: "",
    replay: "",
  });

  const resetFormState = () => {
    reset({
      title: "",
      speakers: session?.user?.name
        ? [{ name: session.user.name, email: session.user.email ?? "" }]
        : [],
      office: profile?.office ?? "",
      description: "",
      format: "",
      visibility: "PRIVATE",
      language: "francais",
      conference: "",
      notes: "",
    });
    setDate("");
    setSelectedConference(null);
    setOfficeEdited(false);
    setAssistantResult(null);
    setError(null);
  };

  const handleCancel = () => {
    resetFormState();
    setSaveError(null);
    onClose();
  };

  const submit = async (data: TalkFormData, status: TalkStatus) => {
    if (date && !dayjs(date).isValid()) {
      setFieldError("root", { message: "Indiquez une date valide" });
      return;
    }
    setSaving(true); setSaveError(null);
    try {
      await onSubmit(buildTalkData(data, status));
      resetFormState(); onClose();
    } catch { setSaveError("La création a échoué. Vos saisies sont conservées ; vous pouvez réessayer."); }
    finally { setSaving(false); }
  };

  const handleSaveDraft = async () => {
    if (await trigger("title")) await submit(getValues(), "DRAFT");
  };
  const onCreateTalk = (data: TalkFormData) => submit(data, "PLANNED");

  const handleTriggerAssistantReview = async () => {
    const values = getValues();
    setError(null);
    setAssistantResult(null);
    setAssistantDialogOpen(true);
    if (!values.title.trim()) {
      setError("Veuillez saisir un titre avant de demander une relecture.");
      return;
    }
    setAssistantLoading(true);
    try {
      const res = await reviewTalkAction({
        title: values.title,
        abstract: values.description,
      });
      setAssistantResult(res);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Erreur de communication avec l'assistant IA.";
      setError(message);
    } finally {
      setAssistantLoading(false);
    }
  };

  const handleApplyAssistantSuggestions = (
    suggestedTitle: string,
    suggestedDescription: string,
  ) => {
    setValue("title", suggestedTitle, { shouldValidate: true, shouldDirty: true });
    setValue("description", suggestedDescription, { shouldValidate: true, shouldDirty: true });
  };

  const values = useWatch({ control });
  const formValue: TalkData = {
    id: "", status: "DRAFT", title: values.title ?? "", description: values.description ?? "",
    office: values.office ?? "", format: values.format ?? "", visibility: values.visibility ?? "PRIVATE",
    language: values.language ?? "", notes: values.notes ?? "",
    speakers: (values.speakers ?? []).map(speaker => ({ name: speaker.name ?? "", email: speaker.email ?? "" })),
    conference: selectedConference, date,
  };
  const change = <K extends keyof TalkData>(field: K, value: TalkData[K]) => {
    if (saving) return;
    const next = { ...formValue, [field]: value };
    if (field === "office") setOfficeEdited(true);
    for (const key of ["title", "description", "office", "format", "visibility", "language", "notes"] as const) {
      if (field === key) setValue(key, next[key], { shouldDirty: true, shouldValidate: isSubmitted });
    }
    if (field === "speakers") setValue("speakers", next.speakers.map(speaker => ({ name: speaker.name, email: speaker.email ?? "" })),
      { shouldDirty: true, shouldValidate: isSubmitted });
    if (field === "conference") {
      setSelectedConference(next.conference);
      setValue("conference", next.conference?.name ?? "");
    }
    if (field === "date") { setDate(next.date ?? ""); clearErrors("root"); }
  };
  return <>
    <TalkFormDialog open={open} title="Nouveau Talk"
      description="Déclarez une nouvelle idée de talk ou soumission à une conférence."
      value={formValue} onChange={change} onClose={handleCancel} onSubmit={handleSubmit(onCreateTalk)}
      onReview={handleTriggerAssistantReview} assistantLoading={assistantLoading}
      pending={saving} disabled={saving} submitLabel="Créer le talk"
      errors={{ ...Object.fromEntries(Object.entries(errors).map(([key, error]) => [key, error?.message])),
        speakers: errors.speakers?.message ?? errors.speakers?.root?.message, date: errors.root?.message }}
      feedback={saveError && <Alert severity="error">{saveError}</Alert>}
      secondaryActions={<Button variant="outlined" disabled={saving} onClick={handleSaveDraft}
        className="text-[#bbb]! border-[#ddd]!">Sauvegarder en brouillon</Button>} />
    <TalkAssistantDialog open={assistantDialogOpen} loading={assistantLoading} error={error}
      title={getValues().title} abstract={getValues().description} assistantReviewResult={assistantResult}
      onClose={() => setAssistantDialogOpen(false)} onApply={handleApplyAssistantSuggestions} />
  </>;
}
