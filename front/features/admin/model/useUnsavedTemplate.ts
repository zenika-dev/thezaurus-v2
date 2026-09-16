"use client";

import { useEffect } from "react";

export const unsavedTemplateMessage = "Des modifications du modèle ne sont pas enregistrées. Voulez-vous les abandonner ?";

type TemplateNavigateEvent = Event & { hashChange: boolean; destination: { sameDocument: boolean; url?: string } };
type NavigationEvents = {
  addEventListener(type: "navigate", listener: (event: TemplateNavigateEvent) => void): void;
  removeEventListener(type: "navigate", listener: (event: TemplateNavigateEvent) => void): void;
};

/** Protect reload/close, Next links and browser history while a draft is open. */
export function useUnsavedTemplate(dirty: boolean) {
  useEffect(() => {
    if (!dirty) return;
    let confirmedUrl: string | null = null;
    let leaving = false;
    let confirmationTimer: ReturnType<typeof setTimeout> | undefined;
    function beforeUnload(event: BeforeUnloadEvent) {
      if (confirmedUrl) return;
      event.preventDefault();
      event.returnValue = "";
    }
    function click(event: MouseEvent) {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
      if (!(link instanceof HTMLAnchorElement) || link.target === "_blank" || link.hasAttribute("download") || link.href === window.location.href) return;
      if (!window.confirm(unsavedTemplateMessage)) {
        event.preventDefault();
        event.stopPropagation();
      } else {
        leaving = true;
        confirmedUrl = link.href;
        clearTimeout(confirmationTimer);
        confirmationTimer = setTimeout(() => { confirmedUrl = null; leaving = false; }, 1000);
      }
    }
    window.addEventListener("beforeunload", beforeUnload);

    // Navigation API cancels traversals before Next unmounts the editor.
    const navigation = (window as Window & { navigation?: NavigationEvents }).navigation;
    function navigate(event: TemplateNavigateEvent) {
      if (event.hashChange || !event.cancelable || !event.destination.sameDocument) return;
      if (event.destination.url === confirmedUrl) return;
      if (!window.confirm(unsavedTemplateMessage)) event.preventDefault();
      else leaving = true;
    }
    if (navigation) navigation.addEventListener("navigate", navigate);
    // Next may navigate via pushState, which does not dispatch `navigate`.
    document.addEventListener("click", click, true);

    // A same-URL entry protects older browsers without cancellable navigation.
    const url = window.location.href;
    let sentinel = false;
    function popstate(event: PopStateEvent) {
      if (!sentinel) return;
      event.stopImmediatePropagation();
      if (window.confirm(unsavedTemplateMessage)) {
        sentinel = false;
        leaving = true;
        window.history.back();
      } else {
        window.history.pushState(window.history.state, "", url);
      }
    }
    if (!navigation) {
      window.history.pushState(window.history.state, "", url);
      sentinel = true;
      window.addEventListener("popstate", popstate, true);
    }
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
      clearTimeout(confirmationTimer);
      document.removeEventListener("click", click, true);
      navigation?.removeEventListener("navigate", navigate);
      window.removeEventListener("popstate", popstate, true);
      // Remove the duplicate after saving or switching tabs, never after departure.
      if (sentinel && !leaving && window.location.href === url) window.history.back();
    };
  }, [dirty]);
}
