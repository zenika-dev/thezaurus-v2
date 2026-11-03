import { afterEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { ConferenceAnnualStatistics } from "@/features/conferences/ui/ConferenceAnnualStatistics";
import { ConferenceStatistics } from "@/features/conferences/ui/ConferenceStatistics";
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

function annualConference(id: string, start: string, end: string, submitted: number, accepted?: number) {
  return mapBackendToFrontend({
    id, name: id, cfpStatus: "Open", date: { start, end, precision: "DAY" },
    submittedTalksAmount: submitted, acceptedTalksAmount: accepted,
  });
}

it("cumule par année de début, conserve les années vides et les conférences sans date", () => {
  const conferences = [
    annualConference("historique", "2025-03-01", "2025-03-01", 2, 1),
    annualConference("première", "2026-04-01", "2026-04-02", 4, 2),
    annualConference("deuxième", "2026-12-31", "2027-01-01", 3, 1),
    annualConference("future", "2027-05-01", "2027-05-01", 0, 0),
    annualConference("sans date", "", "", 1),
  ];
  render(<ConferenceAnnualStatistics conferences={conferences} />);
  const region = within(screen.getByRole("region", { name: "Cumuls par année" }));
  const cells = (name: RegExp) => within(region.getByRole("row", { name }))
    .getAllByRole("cell").map(cell => cell.textContent);
  expect(cells(/^2026 /)).toEqual(["2", "7", "3"]);
  expect(cells(/^2025 /)).toEqual(["1", "2", "1"]);
  expect(cells(/^2027 /)).toEqual(["1", "0", "0"]);
  expect(cells(/^Année non renseignée /)).toEqual(["1", "1", "0"]);
  expect(cells(/^Total /)).toEqual(["5", "10", "4"]);
  expect(region.getAllByRole("rowheader").map(cell => cell.textContent))
    .toEqual(["2027", "2026", "2025", "Année non renseignée", "Total"]);
});

it("affiche un total nul quand aucune conférence n'est disponible", () => {
  render(<ConferenceAnnualStatistics conferences={[]} />);
  const row = screen.getByRole("row", { name: /^Total / });
  expect(within(row).getAllByRole("cell").map(cell => cell.textContent)).toEqual(["0", "0", "0"]);
});

it("conserve les filtres historiques sans afficher le bilan sur la liste ni refaire de requête", () => {
  const fetch = vi.fn<typeof globalThis.fetch>();
  vi.stubGlobal("fetch", fetch);
  state.conferences = [
    annualConference("Historique", "2023-03-01", "2023-03-01", 2, 1),
    annualConference("Future", "2027-05-01", "2027-05-01", 3, 2),
  ];
  render(<ConferencesList />);
  fireEvent.click(screen.getByRole("button", { name: "2023" }));
  expect(screen.getByText("Historique")).toBeInTheDocument();
  expect(screen.queryByText("Future")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: /^Closed$/ }));
  expect(screen.queryByText("Historique")).not.toBeInTheDocument();
  expect(screen.queryByRole("region", { name: "Cumuls par année" })).not.toBeInTheDocument();
  expect(fetch).not.toHaveBeenCalled();
});

it("affiche le bilan sur la page dédiée avec un retour vers la liste", () => {
  state.conferences = [annualConference("Conférence", "2026-05-01", "2026-05-01", 4, 2)];
  render(<ConferenceStatistics />);
  expect(screen.getByRole("heading", { level: 1, name: "Statistiques des conférences" })).toBeInTheDocument();
  expect(screen.getByRole("region", { name: "Cumuls par année" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Retour aux conférences" })).toHaveAttribute("href", "/conferences");
  expect(within(screen.getByRole("row", { name: /^Total / })).getAllByRole("cell").map(cell => cell.textContent))
    .toEqual(["1", "4", "2"]);
});
