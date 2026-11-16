import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import type { TalkData } from "@/entities/talk";
import { TalkVisualButton } from "./TalkVisualButton";

const talk = { id: "talk-1", speakers: [{ name: "Alice" }], conference: { name: "Devoxx" } } as TalkData;

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

it("downloads the generated PNG from the saved talk", async () => {
  const fetchMock = vi.fn().mockResolvedValue({ ok: true, blob: async () => new Blob(["png"], { type: "image/png" }) });
  vi.stubGlobal("fetch", fetchMock);
  const createUrl = vi.fn().mockReturnValue("blob:visual");
  vi.stubGlobal("URL", { createObjectURL: createUrl, revokeObjectURL: vi.fn() });
  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  render(<TalkVisualButton talk={talk} />);
  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));
  await waitFor(() => expect(click).toHaveBeenCalledOnce());
  expect(fetchMock).toHaveBeenCalledWith("/api/backend/talks/talk-1/visual", { method: "POST" });
  expect(createUrl).toHaveBeenCalledWith(expect.any(Blob));
});

it("explains photo consent failures and allows retrying", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 403 }));
  render(<TalkVisualButton talk={talk} />);
  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("autoriser l’utilisation de sa photo par l’IA");
  expect(screen.getByRole("button", { name: "Télécharger le visuel" })).toBeEnabled();
});

it("only offers generation for one speaker with a conference", () => {
  const { rerender } = render(<TalkVisualButton talk={{ ...talk, speakers: [...talk.speakers, ...talk.speakers] }} />);
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
  rerender(<TalkVisualButton talk={{ ...talk, conference: null }} />);
  expect(screen.getByRole("button")).toBeDisabled();
});
