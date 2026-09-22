import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MessageTemplatesSection } from "./MessageTemplatesSection";
import { reminderTemplateApi, ReminderTemplateError } from "../api/reminder-template";

vi.mock("../api/reminder-template", async (original) => {
  const apiModule = await original<typeof import("../api/reminder-template")>();
  return { ...apiModule, reminderTemplateApi: { list: vi.fn(), contexts: vi.fn(), get: vi.fn(), save: vi.fn(), preview: vi.fn() } };
});
// Rich editor round trips are tested separately with the real Tiptap editor.
vi.mock("./ReminderBodyEditor", () => ({
  ReminderBodyEditor: ({ initialHtml, onChange, disabled }: { initialHtml: string; onChange: (html: string) => void; disabled: boolean }) =>
    <textarea aria-label="Corps du modèle" defaultValue={initialHtml} disabled={disabled} onChange={(event) => onChange(event.target.value)} />,
}));

function mount() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<QueryClientProvider client={client}><MessageTemplatesSection /></QueryClientProvider>);
}

beforeEach(() => {
  vi.mocked(reminderTemplateApi.contexts).mockReset().mockResolvedValue({ options: [{ id: 'talk-1', label: 'Qute en pratique — 2026-09-16' }] });
  vi.mocked(reminderTemplateApi.list).mockReset().mockResolvedValue([{ id: 'test', label: 'Modèle de test', apiPath: '/api/admin/test-template', variables: [], conditions: [], links: [], previewContext: { label: "Contexte fourni par le backend", optionsPath: "/api/admin/test-contexts" } }]);
  vi.mocked(reminderTemplateApi.get).mockReset().mockResolvedValue({ subject: "", bodyHtml: "", revision: 0 });
  vi.mocked(reminderTemplateApi.save).mockReset().mockImplementation(async (_path, data) => ({ subject: data.subject ?? "", bodyHtml: data.bodyHtml ?? "", revision: 1 }));
  vi.mocked(reminderTemplateApi.preview).mockReset();
});
afterEach(cleanup);

it("loads another context page without losing the selected option", async () => {
  vi.mocked(reminderTemplateApi.contexts)
    .mockResolvedValueOnce({ options: [{ id: "first", label: "Premier" }], nextCursor: "first" })
    .mockResolvedValueOnce({ options: [{ id: "second", label: "Deuxième" }] });
  mount();
  await edit();
  const selector = screen.getByRole("combobox", { name: "Contexte fourni par le backend" });
  fireEvent.mouseDown(selector);
  fireEvent.click(await screen.findByRole("option", { name: "Premier" }));
  fireEvent.click(await screen.findByRole("button", { name: "Charger plus d’éléments" }));
  await waitFor(() => expect(reminderTemplateApi.contexts).toHaveBeenCalledWith("/api/admin/test-contexts", "first"));
  await waitFor(() => expect(screen.queryByRole("button", { name: "Charger plus d’éléments" })).toBeNull());
  expect((selector as HTMLInputElement).value).toBe("Premier");
  fireEvent.mouseDown(selector);
  expect(await screen.findByRole("option", { name: "Deuxième" })).toBeTruthy();
});

it("previews a non-talk context described entirely by the backend", async () => {
  vi.mocked(reminderTemplateApi.list).mockResolvedValue([{ id: "conference", label: "Invitation", apiPath: "/api/admin/invitation", previewContext: { label: "Conférence", optionsPath: "/api/admin/conference-options" } }]);
  vi.mocked(reminderTemplateApi.contexts).mockResolvedValue({ options: [{ id: "conf-2", label: "DevFest" }] });
  vi.mocked(reminderTemplateApi.preview).mockResolvedValue({ subject: "Invitation", bodyHtml: "<p>Bienvenue</p>", to: [] });
  mount();
  await edit();
  fireEvent.mouseDown(screen.getByRole("combobox", { name: "Conférence" }));
  fireEvent.click(await screen.findByRole("option", { name: "DevFest" }));
  fireEvent.click(screen.getByRole("button", { name: "Générer l’aperçu" }));
  await screen.findByTitle("Corps de l’email");
  expect(reminderTemplateApi.contexts).toHaveBeenCalledWith("/api/admin/conference-options", undefined);
  expect(reminderTemplateApi.preview).toHaveBeenCalledWith("/api/admin/invitation", expect.objectContaining({ contextId: "conf-2" }));
});

it("previews without a selector when the backend declares no context", async () => {
  vi.mocked(reminderTemplateApi.list).mockResolvedValue([{ id: "generic", label: "Information", apiPath: "/api/admin/information" }]);
  vi.mocked(reminderTemplateApi.preview).mockResolvedValue({ subject: "Information", bodyHtml: "<p>Bonjour</p>", to: [] });
  mount();
  await edit();
  expect(screen.getAllByRole("combobox")).toHaveLength(1);
  expect(reminderTemplateApi.contexts).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Générer l’aperçu" }));
  await screen.findByTitle("Corps de l’email");
  expect(reminderTemplateApi.preview).toHaveBeenCalledWith("/api/admin/information", expect.objectContaining({ contextId: "" }));
});

it("loads backend-defined models and protects a dirty draft when switching", async () => {
  vi.mocked(reminderTemplateApi.list).mockResolvedValue([
    { id: "first", label: "Premier modèle", apiPath: "/api/admin/first" },
    { id: "future", label: "Nouveau modèle", apiPath: "/api/admin/future" },
  ]);
  mount();
  await edit();
  const confirmation = vi.spyOn(window, "confirm").mockReturnValue(false);
  fireEvent.mouseDown(screen.getByRole("combobox", { name: "Modèle" }));
  fireEvent.click(await screen.findByRole("option", { name: "Nouveau modèle" }));
  expect(confirmation).toHaveBeenCalled();
  expect(reminderTemplateApi.get).not.toHaveBeenCalledWith("/api/admin/future");
  expect((screen.getByRole("textbox", { name: /Sujet/ }) as HTMLInputElement).value).toBe("Rappel {talkTitle}");
  confirmation.mockReturnValue(true);
  fireEvent.mouseDown(screen.getByRole("combobox", { name: "Modèle" }));
  fireEvent.click(await screen.findByRole("option", { name: "Nouveau modèle" }));
  await waitFor(() => expect(reminderTemplateApi.get).toHaveBeenCalledWith("/api/admin/future"));
  expect((await screen.findByRole("textbox", { name: /Sujet/ }) as HTMLInputElement).value).toBe("");
  await edit();
  fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));
  await screen.findByText("Modèle enregistré.");
  expect(reminderTemplateApi.save).toHaveBeenCalledWith("/api/admin/future", expect.objectContaining({ revision: 0 }));
});

async function edit() {
  fireEvent.change(await screen.findByRole("textbox", { name: /Sujet/ }), { target: { value: "Rappel {talkTitle}" } });
  fireEvent.change(screen.getByRole("textbox", { name: "Corps du modèle" }), { target: { value: "<p>{#if missingVideo}Ajoutez la vidéo{/if}</p>" } });
}

describe("modèle de rappel", () => {
  it("starts empty, saves explicitly with revision and clears the unload warning", async () => {
    mount();
    expect((await screen.findByRole("textbox", { name: /Sujet/ }) as HTMLInputElement).value).toBe("");
    expect((screen.getByRole("button", { name: "Enregistrer" }) as HTMLButtonElement).disabled).toBe(true);
    await edit();
    const before = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(before);
    expect(before.defaultPrevented).toBe(true);
    expect(reminderTemplateApi.save).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));
    await screen.findByText("Modèle enregistré.");
    expect(reminderTemplateApi.save).toHaveBeenCalledWith("/api/admin/test-template", { subject: "Rappel {talkTitle}", bodyHtml: "<p>{#if missingVideo}Ajoutez la vidéo{/if}</p>", revision: 0 });
    const after = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(after);
    expect(after.defaultPrevented).toBe(false);
  });

  it("preserves the local draft on conflict and requires comparison before saving with the latest revision", async () => {
    vi.mocked(reminderTemplateApi.save).mockRejectedValueOnce(new ReminderTemplateError(409, "Version obsolète"));
    mount(); await edit();
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));
    await screen.findByText("Version obsolète");
    expect((screen.getByRole("button", { name: "Enregistrer" }) as HTMLButtonElement).disabled).toBe(true);
    vi.mocked(reminderTemplateApi.get).mockResolvedValueOnce({ subject: "Autre version", bodyHtml: "<p>Autre texte</p>", revision: 7 });
    fireEvent.click(screen.getByRole("button", { name: "Recharger pour comparer" }));
    await screen.findByText("Autre version");
    expect((screen.getByRole("textbox", { name: /Sujet/ }) as HTMLInputElement).value).toBe("Rappel {talkTitle}");
    expect((screen.getByRole("textbox", { name: "Corps du modèle" }) as HTMLTextAreaElement).value).toContain("Ajoutez la vidéo");
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer ma version après comparaison" }));
    await screen.findByText("Modèle enregistré.");
    expect(reminderTemplateApi.save).toHaveBeenLastCalledWith("/api/admin/test-template", expect.objectContaining({ revision: 7, subject: "Rappel {talkTitle}" }));
  });

  it("previews the unsaved draft for the selected real talk in a sandbox without saving", async () => {
    vi.mocked(reminderTemplateApi.preview).mockResolvedValue({ subject: "Rappel Qute en pratique", bodyHtml: "<p>Ajoutez la vidéo</p>", to: ["alice@example.com", "bob@example.com"] });
    mount(); await edit();
    const talk = screen.getByRole("combobox", { name: "Contexte fourni par le backend" });
    fireEvent.mouseDown(talk);
    fireEvent.click(await screen.findByRole("option", { name: /Qute en pratique/ }));
    fireEvent.click(screen.getByRole("button", { name: "Générer l’aperçu" }));
    await screen.findByText(/alice@example.com, bob@example.com/);
    expect(reminderTemplateApi.preview).toHaveBeenCalledWith("/api/admin/test-template", expect.objectContaining({ subject: "Rappel {talkTitle}", contextId: "talk-1" }));
    expect(reminderTemplateApi.save).not.toHaveBeenCalled();
    const frame = screen.getByTitle("Corps de l’email");
    expect(frame.getAttribute("sandbox")).toBe("");
    expect(frame.getAttribute("srcdoc")).toContain("default-src 'none'");
    fireEvent.change(screen.getByRole("textbox", { name: /Sujet/ }), { target: { value: "Nouveau sujet" } });
    await screen.findByText(/Générez à nouveau l’aperçu/);
  });

  it("keeps the draft after a server validation error", async () => {
    vi.mocked(reminderTemplateApi.save).mockRejectedValueOnce(new ReminderTemplateError(400, "Variable inconnue : secret"));
    mount(); await edit();
    fireEvent.click(screen.getByRole("button", { name: "Enregistrer" }));
    await screen.findByText("Variable inconnue : secret");
    await waitFor(() => expect((screen.getByRole("button", { name: "Enregistrer" }) as HTMLButtonElement).disabled).toBe(false));
    expect((screen.getByRole("textbox", { name: /Sujet/ }) as HTMLInputElement).value).toBe("Rappel {talkTitle}");
  });
});
