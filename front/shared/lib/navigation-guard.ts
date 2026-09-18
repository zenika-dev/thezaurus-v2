"use client";

import { useRouter } from "next/navigation";

type NavigationGuard = (href: string) => boolean;
const guards = new Set<NavigationGuard>();

/** Guards belong to mounted dirty forms, not to a particular navigation widget. */
export function registerNavigationGuard(guard: NavigationGuard) {
  guards.add(guard);
  return () => { guards.delete(guard); };
}

/** Check before handing navigation to Next, which may render before pushState. */
export function useGuardedPush() {
  const router = useRouter();
  return (href: string) => {
    for (const guard of guards) {
      if (!guard(href)) return;
    }
    router.push(href);
  };
}
