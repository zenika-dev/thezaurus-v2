import {
  apiFetch,
  type BackendReminderTemplateView,
  type BackendReminderTemplateUpdate,
  type BackendReminderTemplatePreviewRequest,
  type BackendReminderTemplatePreview,
  type BackendMessageTemplateDefinition,
} from "@/shared/api";

export class ReminderTemplateError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, {
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
  list: () => request<BackendMessageTemplateDefinition[]>("/api/admin/message-templates"),
  get: (path: string) => request<BackendReminderTemplateView>(path),
  save: (path: string, template: BackendReminderTemplateUpdate) =>
    request<BackendReminderTemplateView>(path, { method: "PUT", body: JSON.stringify(template) }),
  preview: (path: string, template: BackendReminderTemplatePreviewRequest) =>
    request<BackendReminderTemplatePreview>(`${path}/preview`, { method: "POST", body: JSON.stringify(template) }),
};
