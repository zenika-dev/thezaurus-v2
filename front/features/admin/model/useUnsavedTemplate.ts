"use client";
import { useUnsavedNavigation } from "@/shared/lib/useUnsavedNavigation";
export const unsavedTemplateMessage = "Des modifications du modèle ne sont pas enregistrées. Voulez-vous les abandonner ?";
export function useUnsavedTemplate(dirty: boolean) {
  useUnsavedNavigation(dirty, unsavedTemplateMessage);
}
