"use client";

import { useEffect } from "react";
import { registerNavigationGuard } from "@/shared/lib/navigation-guard";

export const unsavedTemplateMessage = "Des modifications du modèle ne sont pas enregistrées. Voulez-vous les abandonner ?";

type TemplateNavigateEvent = Event & { navigationType: string; hashChange: boolean; destination: { sameDocument: boolean } };
type NavigationEvents = {
  addEventListener(type: "navigate", listener: (event: TemplateNavigateEvent) => void): void;
  removeEventListener(type: "navigate", listener: (event: TemplateNavigateEvent) => void): void;
};

/** Protect reload/close, Next links and browser history while a draft is open. */
export function useUnsavedTemplate(dirty: boolean) {
  useEffect(() => {
    if (!dirty) return;
    let leaving = false;
    let allowDocumentUnload = false;
    let unloadTimer: ReturnType<typeof setTimeout> | undefined;
    function confirmDeparture(href: string) {
      const destination = new URL(href, window.location.href);
      const current = new URL(window.location.href);
      if (destination.origin === current.origin && destination.pathname === current.pathname && destination.search === current.search) return true;
      leaving = false;
      if (!window.confirm(unsavedTemplateMessage)) return false;
      leaving = true;
      return true;
    }
    const unregisterGuard = registerNavigationGuard(confirmDeparture);
    function beforeUnload(event: BeforeUnloadEvent) {
      if (allowDocumentUnload) return;
      event.preventDefault();
      event.returnValue = "";
    }
    function click(event: MouseEvent) {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
      if (!(link instanceof HTMLAnchorElement) || link.target === "_blank" || link.hasAttribute("download") || link.href === window.location.href) return;
      if (!confirmDeparture(link.href)) {
        event.preventDefault();
        event.stopPropagation();
      } else {
        // An ordinary anchor unloads in this turn. Asynchronous Next pushes
        // are already checked before dispatch and do not prompt again.
        allowDocumentUnload = true;
        clearTimeout(unloadTimer);
        unloadTimer = setTimeout(() => { allowDocumentUnload = false; }, 0);
      }
    }
    window.addEventListener("beforeunload", beforeUnload);

    // Only browser history traversals are guarded here. Application pushes
    // are checked before dispatch; cancelling their navigate event is too late.
    const navigation = (window as Window & { navigation?: NavigationEvents }).navigation;
    function navigate(event: TemplateNavigateEvent) {
      if (event.navigationType !== "traverse" || event.hashChange || !event.cancelable || !event.destination.sameDocument) return;
      if (!window.confirm(unsavedTemplateMessage)) event.preventDefault();
      else leaving = true;
    }
    if (navigation) navigation.addEventListener("navigate", navigate);
    // Ask before Next handles a Link. Waiting for its navigate/pushState event
    // can be too late if React has already started replacing the dirty form.
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
      unregisterGuard();
      clearTimeout(unloadTimer);
      document.removeEventListener("click", click, true);
      navigation?.removeEventListener("navigate", navigate);
      window.removeEventListener("popstate", popstate, true);
      // Remove the duplicate after saving or switching tabs, never after departure.
      if (sentinel && !leaving && window.location.href === url) window.history.back();
    };
  }, [dirty]);
}
