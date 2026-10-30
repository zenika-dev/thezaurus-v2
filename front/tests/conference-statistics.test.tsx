import { afterEach, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
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
  const conferences = await conferenceApi.getConferencesWithStatistics();
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(String(fetch.mock.calls[0]?.[0])).toContain("/conferences?includeStatistics=true");
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
