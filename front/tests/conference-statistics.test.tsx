import { afterEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { mapBackendToFrontend } from "@/entities/conference/api";
import { conferenceApi, type ConferenceData } from "@/entities/conference";
import { ConferencesList } from "@/features/conferences/ui/ConferencesList";

const state = vi.hoisted(() => ({ conferences: [] as ConferenceData[] }));
vi.mock("@/features/conferences/model", () => ({ useConferences: () => state }));

afterEach(() => vi.unstubAllGlobals());

it("charge les statistiques groupées en un seul appel", async () => {
  const fetch = vi.fn<typeof globalThis.fetch>(async () => Response.json([
    { id: "a", name: "Conférence", submittedTalksAmount: 4, acceptedTalksAmount: 2 },
    { id: "b", name: "Autre", submittedTalksAmount: 0, acceptedTalksAmount: 0 },
  ]));
  vi.stubGlobal("fetch", fetch);
  const conferences = await conferenceApi.getConferences();
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(String(fetch.mock.calls[0]?.[0])).toContain("/conferences");
  expect(conferences[0]).toMatchObject({ submittedTalksAmount: 4, acceptedTalksAmount: 2 });
  state.conferences = conferences;
  render(<ConferencesList />);
  expect(screen.getByText("4 talks soumis")).toBeInTheDocument();
  expect(screen.getByText("2 talks acceptés")).toBeInTheDocument();
  expect(screen.getByText("0 talks acceptés")).toBeInTheDocument();
});

it("ne calcule pas les statistiques dans la sélection de conférence", async () => {
  const fetch = vi.fn<typeof globalThis.fetch>(async () => Response.json([]));
  vi.stubGlobal("fetch", fetch);
  await conferenceApi.getConferences();
  expect(String(fetch.mock.calls[0]?.[0])).toMatch(/\/conferences$/);
});

it("trie les compteurs sans requête, conserve les filtres et réinitialise l'ordre", () => {
  const fetch = vi.fn<typeof globalThis.fetch>();
  vi.stubGlobal("fetch", fetch);
  state.conferences = [
    { id: "a", name: "Alpha", cfpStatus: "Closed", submittedTalksAmount: 4, acceptedTalksAmount: 1 },
    { id: "b", name: "Bravo", cfpStatus: "Open", submittedTalksAmount: 3, acceptedTalksAmount: 3 },
    { id: "c", name: "Charlie", cfpStatus: "Open", submittedTalksAmount: 4, acceptedTalksAmount: 2 },
  ].map(mapBackendToFrontend);
  render(<ConferencesList />);
  const sort = screen.getByRole("combobox", { name: /Trier par/ });
  const before = (first: string, second: string) => {
    expect(screen.getByText(first).compareDocumentPosition(screen.getByText(second)) & Node.DOCUMENT_POSITION_FOLLOWING)
      .not.toBe(0);
  };
  fireEvent.change(sort, { target: { value: "submitted" } });
  before("Alpha", "Charlie"); before("Charlie", "Bravo");
  fireEvent.change(sort, { target: { value: "accepted" } });
  before("Bravo", "Charlie"); before("Charlie", "Alpha");
  fireEvent.click(screen.getByRole("button", { name: /^Open$/ }));
  expect(screen.queryByText("Alpha")).not.toBeInTheDocument();
  before("Bravo", "Charlie");
  fireEvent.click(screen.getByRole("button", { name: "Réinitialiser" }));
  expect(sort).toHaveValue("default");
  before("Alpha", "Bravo"); before("Bravo", "Charlie");
  expect(state.conferences.map(conference => conference.id)).toEqual(["a", "b", "c"]);
  expect(fetch).not.toHaveBeenCalled();
});
