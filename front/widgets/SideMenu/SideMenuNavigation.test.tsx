import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { SideMenuClient } from "./SideMenuClient";
import { useUnsavedTemplate } from "@/features/admin/model/useUnsavedTemplate";

const router = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router, usePathname: () => window.location.pathname }));
vi.mock("next-auth/react", () => ({ useSession: () => ({ data: { user: { roles: ["ADMIN"] } } }) }));
vi.mock("next-themes", () => ({ useTheme: () => ({ resolvedTheme: "light", setTheme: vi.fn() }) }));

const links = [{ label: "Talks", path: "/talks" }];

function DirtyFormAndRealMenu() {
  const [dirty, setDirty] = useState(false);
  useUnsavedTemplate(dirty);
  return <>
    <input aria-label="Brouillon" onChange={() => setDirty(true)} />
    <button onClick={() => setDirty(false)}>Enregistrer le brouillon</button>
    <SideMenuClient navLinks={links} />
  </>;
}

beforeEach(() => {
  router.push.mockReset();
  window.history.replaceState({}, "", "/start");
});
afterEach(() => {
  cleanup();
  Reflect.deleteProperty(window, "navigation");
});

describe.each([false, true])("real menu with Navigation API: %s", (modern) => {
  it.each([
    ["Talks", "/talks"],
    ["Mon profil", "/profile"],
    ["Administration", "/admin"],
  ])("checks %s before Next receives the navigation, and confirms only once", (label, path) => {
    const navigation = new EventTarget();
    if (modern) Object.defineProperty(window, "navigation", { configurable: true, value: navigation });
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    router.push.mockImplementation(() => {
      const event = Object.assign(new Event("navigate", { cancelable: true }), {
        navigationType: "push", destination: { sameDocument: true }, hashChange: false,
      });
      navigation.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(false);
    });
    render(<DirtyFormAndRealMenu />);
    fireEvent.change(screen.getByRole("textbox", { name: "Brouillon" }), { target: { value: "Texte à conserver" } });
    fireEvent.click(screen.getByRole("button", { name: label }));
    expect(confirm).toHaveBeenCalledOnce();
    expect(router.push).not.toHaveBeenCalled();
    expect((screen.getByRole("textbox", { name: "Brouillon" }) as HTMLInputElement).value).toBe("Texte à conserver");

    confirm.mockReturnValue(true);
    fireEvent.click(screen.getByRole("button", { name: label }));
    expect(router.push).toHaveBeenCalledExactlyOnceWith(path);
    expect(confirm).toHaveBeenCalledTimes(2);
  });
});

it("removes the guard after save and unmount without blocking a clean transition", () => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const form = render(<DirtyFormAndRealMenu />);
  fireEvent.change(screen.getByRole("textbox", { name: "Brouillon" }), { target: { value: "Texte" } });
  const dirtyUnload = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(dirtyUnload);
  expect(dirtyUnload.defaultPrevented).toBe(true);
  fireEvent.click(screen.getByRole("button", { name: "Enregistrer le brouillon" }));
  const cleanUnload = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(cleanUnload);
  expect(cleanUnload.defaultPrevented).toBe(false);
  fireEvent.click(screen.getByRole("button", { name: "Talks" }));
  expect(router.push).toHaveBeenCalledExactlyOnceWith("/talks");
  expect(confirm).not.toHaveBeenCalled();

  fireEvent.change(screen.getByRole("textbox", { name: "Brouillon" }), { target: { value: "Autre texte" } });
  form.unmount();
  router.push.mockClear();
  render(<SideMenuClient navLinks={links} />);
  fireEvent.click(screen.getByRole("button", { name: "Mon profil" }));
  expect(router.push).toHaveBeenCalledExactlyOnceWith("/profile");
  expect(confirm).not.toHaveBeenCalled();
});
