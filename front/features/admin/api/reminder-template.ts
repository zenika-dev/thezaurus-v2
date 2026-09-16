import {
  apiFetch,
  type BackendReminderTemplateView,
  type BackendReminderTemplateUpdate,
  type BackendReminderTemplatePreviewRequest,
  type BackendReminderTemplatePreview,
} from "@/shared/api";

export class ReminderTemplateError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

async function request<T>(suffix = "", init?: RequestInit): Promise<T> {
  const response = await apiFetch(`/api/admin/reminder-template${suffix}`, {
    ...init,
    cache: "no-store",
    headers: { "Content-Type": "application/json" },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ReminderTemplateError(
      response.status,
      typeof body?.message === "string"
        ? body.message
        : "Impossible de traiter le modèle. Veuillez réessayer.",
    );
  }
  return response.json();
}

export const reminderTemplateApi = {
  get: () => request<BackendReminderTemplateView>(),
  save: (template: BackendReminderTemplateUpdate) =>
    request<BackendReminderTemplateView>("", { method: "PUT", body: JSON.stringify(template) }),
  preview: (template: BackendReminderTemplatePreviewRequest) =>
    request<BackendReminderTemplatePreview>("/preview", { method: "POST", body: JSON.stringify(template) }),
};
