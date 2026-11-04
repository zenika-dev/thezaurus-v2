import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { queryKeys } from "@/shared/api";
import { CreateTalkDialog } from "@/features/talks/ui/CreateTalkDialog";
import { TalkDetailsDialog } from "@/features/talks/ui/TalkDetailsDialog";
import { CreateConferenceDialog } from "@/features/conferences/ui/CreateConferenceDialog";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { BlogPostFormDialog } from "@/features/blog-posts/ui/BlogPostFormDialog";

beforeEach(() => Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() }));
afterEach(() => { Reflect.deleteProperty(window, "navigation"); });

it.each(["Annuler", "Fermer", "Escape", "backdrop"])("keeps an edited article after refusing %s", async (action) => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  render(<QueryClientProvider client={new QueryClient()}>
    <BlogPostFormDialog onClose={close} onSave={vi.fn()} />
  </QueryClientProvider>);
  fireEvent.change(screen.getByRole("textbox", { name: /Titre/ }), { target: { value: "Mon article" } });
  if (action === "Escape") fireEvent.keyDown(screen.getByRole("dialog"), { key: "Escape", code: "Escape" });
  else if (action === "backdrop") {
    const backdrop = document.querySelector(".MuiDialog-container")!;
    fireEvent.mouseDown(backdrop); fireEvent.click(backdrop);
  } else fireEvent.click(screen.getByRole("button", { name: action }));
  expect(close).not.toHaveBeenCalled();
  expect(window.confirm).toHaveBeenCalled();
  expect(screen.getByRole("textbox", { name: /Titre/ })).toHaveValue("Mon article");
  Reflect.deleteProperty(window, "navigation");
});

it("guards a conference before submission and clears its discarded title", () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  render(<CreateConferenceDialog open onClose={close} onSubmit={vi.fn()} />);
  fireEvent.change(screen.getByRole("textbox", { name: /Titre de la conférence/ }), { target: { value: "Conférence locale" } });
  fireEvent.click(screen.getByRole("button", { name: "Annuler" }));
  expect(close).not.toHaveBeenCalled();
  expect(confirm).toHaveBeenCalledOnce();
  confirm.mockReturnValue(true);
  fireEvent.click(screen.getByRole("button", { name: "Fermer" }));
  expect(close).toHaveBeenCalledOnce();
  expect(screen.getByRole("textbox", { name: /Titre de la conférence/ })).toHaveValue("");
});

it("protects a new talk after editing, but not its asynchronously prefilled office", async () => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } });
  client.setQueryData(queryKeys.profile.me(), { name: "Alice", office: "nantes" });
  render(<SessionProvider session={{ user: { name: "Alice", email: "alice@example.test", roles: ["CONSULTANT"] }, expires: "2099-01-01" }}>
    <QueryClientProvider client={client}><CreateTalkDialog open onClose={close} onSubmit={vi.fn()} /></QueryClientProvider>
  </SessionProvider>);
  fireEvent.click(screen.getByRole("button", { name: "Annuler" }));
  expect(close).toHaveBeenCalledOnce();
  expect(confirm).not.toHaveBeenCalled();
  fireEvent.change(screen.getByRole("textbox", { name: /Titre du talk/ }), { target: { value: "Talk local" } });
  fireEvent.click(screen.getByRole("button", { name: "Fermer" }));
  expect(close).toHaveBeenCalledOnce();
  expect(confirm).toHaveBeenCalledOnce();
  Reflect.deleteProperty(window, "navigation");
});

it("blocks dismissal during article saving and closes directly after success", async () => {
  Object.defineProperty(window, "navigation", { configurable: true, value: new EventTarget() });
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  let complete!: () => void;
  const save = vi.fn(() => new Promise<void>(resolve => { complete = resolve; }));
  render(<QueryClientProvider client={new QueryClient()}>
    <BlogPostFormDialog defaultAuthor={{ name: "Alice" }} onClose={close} onSave={save} />
  </QueryClientProvider>);
  fireEvent.change(screen.getByRole("textbox", { name: /Titre/ }), { target: { value: "Article local" } });
  fireEvent.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(save).toHaveBeenCalledOnce());
  expect(screen.getByRole("button", { name: "Annuler" })).toBeDisabled();
  fireEvent.keyDown(screen.getByRole("dialog"), { key: "Escape" });
  expect(close).not.toHaveBeenCalled();
  await act(async () => { complete(); });
  expect(close).toHaveBeenCalledOnce();
  expect(confirm).not.toHaveBeenCalled();
  Reflect.deleteProperty(window, "navigation");
});

it("compares edited talk details with the saved talk, including reverted edits", () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  render(<SessionProvider session={{ user: { name: "Alice", roles: ["ADMIN"] }, expires: "2099-01-01" }}>
    <QueryClientProvider client={new QueryClient()}><TalkDetailsDialog open onClose={close} onUpdate={vi.fn()} onDelete={vi.fn()}
      talk={{ id: "local", title: "Titre enregistré", description: "Description", speakers: [{ name: "Alice" }], office: "nantes",
        format: "conference", visibility: "PRIVATE", language: "francais", notes: "", status: "DRAFT", conference: null, date: "" }} />
    </QueryClientProvider>
  </SessionProvider>);
  const title = screen.getByRole("textbox", { name: /Titre du talk/ });
  fireEvent.change(title, { target: { value: "Modification locale" } });
  fireEvent.click(screen.getByRole("button", { name: "Annuler" }));
  expect(close).not.toHaveBeenCalled();
  expect(confirm).toHaveBeenCalledOnce();
  confirm.mockClear();
  fireEvent.change(title, { target: { value: "Titre enregistré" } });
  fireEvent.click(screen.getByRole("button", { name: "Fermer" }));
  expect(close).toHaveBeenCalledOnce();
  expect(confirm).not.toHaveBeenCalled();
  const slides = screen.getByRole("textbox", { name: "Slides" });
  fireEvent.change(slides, { target: { value: "https://example.test/slides" } });
  fireEvent.change(slides, { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Fermer" }));
  expect(close).toHaveBeenCalledTimes(2);
  expect(confirm).not.toHaveBeenCalled();
});

it("closes an unchanged article without asking, including after reverting an edit", () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  const close = vi.fn();
  render(<QueryClientProvider client={new QueryClient()}>
    <BlogPostFormDialog onClose={close} onSave={vi.fn()} />
  </QueryClientProvider>);
  const title = screen.getByRole("textbox", { name: /Titre/ });
  fireEvent.change(title, { target: { value: "Mon article" } });
  fireEvent.change(title, { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Annuler" }));
  expect(close).toHaveBeenCalledOnce();
  expect(confirm).not.toHaveBeenCalled();
});
