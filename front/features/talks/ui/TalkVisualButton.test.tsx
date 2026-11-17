import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import type { TalkData } from "@/entities/talk";
import { TalkVisualButton } from "./TalkVisualButton";

const talk = { id: "talk-1", speakers: [{ name: "Alice" }], conference: { name: "Devoxx" } } as TalkData;

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

it.each([1, 2, 3])("downloads the generated PNG for %i speakers", async (count) => {
  const fetchMock = vi.fn().mockResolvedValue({ ok: true, blob: async () => new Blob(["png"], { type: "image/png" }) });
  vi.stubGlobal("fetch", fetchMock);
  const createUrl = vi.fn().mockReturnValue("blob:visual");
  vi.stubGlobal("URL", { createObjectURL: createUrl, revokeObjectURL: vi.fn() });
  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  render(<TalkVisualButton talk={{ ...talk, speakers: Array.from({ length: count }, (_, i) => ({ name: `Speaker ${i}` })) }} />);
  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));
  await waitFor(() => expect(click).toHaveBeenCalledOnce());
  expect(fetchMock).toHaveBeenCalledWith("/api/backend/talks/talk-1/visual", { method: "POST" });
  expect(createUrl).toHaveBeenCalledWith(expect.any(Blob));
});

it("explains access failures and allows retrying", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 403 }));
  render(<TalkVisualButton talk={talk} />);
  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("pas accès à la génération");
  expect(screen.getByRole("button", { name: "Télécharger le visuel" })).toBeEnabled();
});

it.each([0, 4])("disables generation for %i speakers and without a conference", (count) => {
  const { rerender } = render(<TalkVisualButton talk={{ ...talk, speakers: Array.from({ length: count }, () => talk.speakers[0]) }} />);
  expect(screen.getByRole("button")).toBeDisabled();
  rerender(<TalkVisualButton talk={{ ...talk, conference: null }} />);
  expect(screen.getByRole("button")).toBeDisabled();
});

it("explains the participant limit when hovering over the disabled list action", async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  render(<TalkVisualButton talk={{ ...talk, title: "Trop de personnes", speakers: Array.from({ length: 4 }, () => talk.speakers[0]) }} iconOnly />);
  const button = screen.getByRole("button", { name: "Télécharger le visuel du talk : Trop de personnes" });
  expect(button).toBeDisabled();
  await userEvent.hover(button.parentElement!);
  expect(await screen.findByRole("tooltip")).toHaveTextContent("3 intervenants maximum");
  expect(button.parentElement).toHaveAttribute("tabindex", "0");
  expect(fetchMock).not.toHaveBeenCalled();
});
