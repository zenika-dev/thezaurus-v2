import "@testing-library/jest-dom/vitest";
import { cleanup, configure } from "@testing-library/react";
import { afterEach, vi } from "vitest";
import React from "react";

let currentSearchParams = new URLSearchParams();
const listeners = new Set<() => void>();

export function resetMockSearchParams() {
  currentSearchParams = new URLSearchParams();
  listeners.forEach((l) => l());
}

export function setMockSearchParams(params: string | URLSearchParams) {
  currentSearchParams = new URLSearchParams(params);
  listeners.forEach((l) => l());
}

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: (target: string) => {
      const queryIndex = target.indexOf("?");
      const query = queryIndex !== -1 ? target.slice(queryIndex + 1) : "";
      currentSearchParams = new URLSearchParams(query);
      listeners.forEach((l) => l());
    },
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
  }),
  usePathname: () => "/",
  useSearchParams: () => {
    const [, forceUpdate] = React.useReducer((x: number) => x + 1, 0);
    React.useEffect(() => {
      listeners.add(forceUpdate);
      return () => {
        listeners.delete(forceUpdate);
      };
    }, []);
    return currentSearchParams;
  },
}));

configure({ asyncUtilTimeout: 10000 });

afterEach(() => {
  cleanup();
  currentSearchParams = new URLSearchParams();
  listeners.clear();
});
