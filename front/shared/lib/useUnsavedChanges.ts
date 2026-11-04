"use client";
import { useUnsavedNavigation } from "./useUnsavedNavigation";
export const unsavedChangesMessage = "Des modifications ne sont pas enregistrées. Voulez-vous les abandonner ?";
/** Successful saves close directly; only user departures go through this guard. */
export function useUnsavedChanges(dirty: boolean, pending = false) {
  useUnsavedNavigation(dirty, unsavedChangesMessage, pending);
  return (close: () => void) => {
    if (pending) return;
    if (!dirty || window.confirm(unsavedChangesMessage)) close();
  };
}
