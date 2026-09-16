import { afterEach, expect, it, vi } from "vitest";
import { cleanup, renderHook } from "@testing-library/react";
import { useUnsavedTemplate } from "./useUnsavedTemplate";

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it("cancels a browser traversal before unmount when the user keeps their draft", () => {
  const navigation = new EventTarget();
  Object.defineProperty(window, "navigation", { value: navigation, configurable: true });
  vi.spyOn(window, "confirm").mockReturnValue(false);
  const { rerender } = renderHook(({ dirty }) => useUnsavedTemplate(dirty), { initialProps: { dirty: true } });
  const event = Object.assign(new Event("navigate", { cancelable: true }), { destination: { sameDocument: true }, hashChange: false });
  navigation.dispatchEvent(event);
  expect(event.defaultPrevented).toBe(true);
  rerender({ dirty: false });
  const clean = Object.assign(new Event("navigate", { cancelable: true }), { destination: { sameDocument: true }, hashChange: false });
  navigation.dispatchEvent(clean);
  expect(clean.defaultPrevented).toBe(false);
  Reflect.deleteProperty(window, "navigation");
});

it("blocks ordinary links in browsers without Navigation API and allows confirmed departures", () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  renderHook(() => useUnsavedTemplate(true));
  const link = document.createElement("a");
  link.href = "/talks";
  document.body.appendChild(link);
  const click = new MouseEvent("click", { bubbles: true, cancelable: true });
  link.dispatchEvent(click);
  expect(click.defaultPrevented).toBe(true);
  confirm.mockReturnValue(true);
  const allowed = new MouseEvent("click", { bubbles: true, cancelable: true });
  // Prevent jsdom's actual document navigation after recording the guard result.
  let preventedByGuard = true;
  link.addEventListener("click", (event) => { preventedByGuard = event.defaultPrevented; event.preventDefault(); });
  link.dispatchEvent(allowed);
  expect(preventedByGuard).toBe(false);
  link.remove();
});
