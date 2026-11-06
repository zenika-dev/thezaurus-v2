import { renderHook } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { useUnsavedChanges } from "./useUnsavedChanges";

afterEach(() => { Reflect.deleteProperty(window, "navigation"); });

it("only confirms closing a modified form and preserves it when cancelled", () => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  const { result, rerender } = renderHook(({ dirty, pending }) => useUnsavedChanges(dirty, pending), {
    initialProps: { dirty: false, pending: false },
  });
  result.current(close);
  expect(close).toHaveBeenCalledTimes(1);
  expect(confirm).not.toHaveBeenCalled();
  rerender({ dirty: true, pending: false });
  result.current(close);
  expect(close).toHaveBeenCalledTimes(1);
  confirm.mockReturnValue(true);
  result.current(close);
  expect(close).toHaveBeenCalledTimes(2);
  rerender({ dirty: true, pending: true });
  result.current(close);
  expect(close).toHaveBeenCalledTimes(2);
  expect(confirm).toHaveBeenCalledTimes(2);
});

it("protects navigation and document unload until the form is clean", () => {
  const navigation = new EventTarget();
  Object.defineProperty(window, "navigation", { configurable: true, value: navigation });
  vi.spyOn(window, "confirm").mockReturnValue(false);
  const { rerender } = renderHook(({ dirty }) => useUnsavedChanges(dirty), { initialProps: { dirty: true } });
  const traverse = () => Object.assign(new Event("navigate", { cancelable: true }), {
    navigationType: "traverse", destination: { sameDocument: true }, hashChange: false,
  });
  const back = traverse(); navigation.dispatchEvent(back);
  expect(back.defaultPrevented).toBe(true);
  const unload = new Event("beforeunload", { cancelable: true }); window.dispatchEvent(unload);
  expect(unload.defaultPrevented).toBe(true);
  rerender({ dirty: false });
  const clean = traverse(); navigation.dispatchEvent(clean);
  expect(clean.defaultPrevented).toBe(false);
});

it("blocks links during saving without asking to discard, then allows clean navigation", () => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
  const { rerender } = renderHook(({ pending }) => useUnsavedChanges(false, pending), { initialProps: { pending: true } });
  const link = document.createElement("a");
  link.href = "/talks";
  document.body.appendChild(link);
  const blocked = new MouseEvent("click", { bubbles: true, cancelable: true });
  link.dispatchEvent(blocked);
  expect(blocked.defaultPrevented).toBe(true);
  expect(confirm).not.toHaveBeenCalled();
  rerender({ pending: false });
  let guarded = true;
  link.addEventListener("click", event => { guarded = event.defaultPrevented; event.preventDefault(); });
  link.dispatchEvent(new MouseEvent("click", { bubbles: true, cancelable: true }));
  expect(guarded).toBe(false);
  link.remove();
});
