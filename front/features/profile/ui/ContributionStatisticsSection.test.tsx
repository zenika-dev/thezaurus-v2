import { beforeEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiFetch, type BackendContributionStatistics } from "@/shared/api";
import { useTalksMutations } from "@/features/talks/model/useTalksMutations";
import { usePostsMutations } from "@/features/blog-posts/model/usePostsMutations";
import { ContributionStatisticsSection } from "./ContributionStatisticsSection";

vi.mock("@/shared/api", async (original) => ({
  ...await original<typeof import("@/shared/api")>(), apiFetch: vi.fn(),
}));
vi.mock("@/entities/talk/actions", () => ({
  createTalkAction: vi.fn(), updateTalkAction: vi.fn(), deleteTalkAction: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("@/entities/post/actions", () => ({
  createPostAction: vi.fn(), updatePostAction: vi.fn(), deletePostAction: vi.fn().mockResolvedValue(undefined),
}));

const statistics: BackendContributionStatistics = {
  currentYear: 2026,
  years: [
    { year: 2027, publishedArticles: 0, submittedCandidatures: 3, acceptedCandidatures: 1 },
    { year: 2026, publishedArticles: 0, submittedCandidatures: 0, acceptedCandidatures: 0 },
    { year: 2025, publishedArticles: 2, submittedCandidatures: 4, acceptedCandidatures: 2 },
  ],
  undatedCandidatures: 2,
  undatedPublishedArticles: 1,
};

beforeEach(() => vi.clearAllMocks());

function ContributionEditor() {
  const { deleteTalk } = useTalksMutations();
  const { deletePost } = usePostsMutations();
  return <>
    <button onClick={() => void deleteTalk("talk")}>Supprimer le talk</button>
    <button onClick={() => void deletePost("post")}>Supprimer l’article</button>
  </>;
}

function setup(withEditor = false) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  render(<QueryClientProvider client={client}>
    <ContributionStatisticsSection />
    {withEditor && <ContributionEditor />}
  </QueryClientProvider>);
  return client;
}

function valueOf(label: string) {
  const definition = screen.getByText(label).parentElement;
  if (!definition) throw new Error("Missing metric");
  return within(definition).getByRole("definition").textContent;
}

it("defaults to the current year, switches years, and keeps the global exclusion warning", async () => {
  vi.mocked(apiFetch).mockResolvedValue(Response.json(statistics));
  setup();
  expect(await screen.findByRole("combobox", { name: "Année" })).toHaveTextContent("2026");
  expect(valueOf("Articles publiés")).toBe("0");
  expect(screen.getByRole("alert")).toHaveTextContent("Toutes années confondues : 2 Talks et 1 article publié");
  fireEvent.mouseDown(screen.getByRole("combobox", { name: "Année" }));
  fireEvent.click(await screen.findByRole("option", { name: "2025" }));
  expect(valueOf("Articles publiés")).toBe("2");
  expect(valueOf("Talks soumis")).toBe("4");
  expect(screen.getByRole("alert")).toBeInTheDocument();
  expect(apiFetch).toHaveBeenCalledWith("/api/me/profile/contributions");
});

it("shows loading instead of zeros, then an error with a working retry", async () => {
  let finish: (response: Response) => void = () => {};
  vi.mocked(apiFetch).mockImplementationOnce(() => new Promise((resolve) => { finish = resolve; }));
  setup();
  expect(screen.getByRole("status")).toHaveAccessibleName("Chargement des statistiques");
  expect(screen.queryByText("Articles publiés")).not.toBeInTheDocument();
  await act(async () => finish(new Response(null, { status: 500 })));
  expect(await screen.findByRole("alert")).toHaveTextContent("Impossible de charger les statistiques");
  vi.mocked(apiFetch).mockResolvedValue(Response.json(statistics));
  fireEvent.click(screen.getByRole("button", { name: "Réessayer" }));
  expect(await screen.findByRole("combobox", { name: "Année" })).toBeInTheDocument();
});

it.each(["Supprimer le talk", "Supprimer l’article"])("refreshes counts, years and exclusions after %s", async (button) => {
  vi.mocked(apiFetch).mockResolvedValue(Response.json(statistics));
  setup(true);
  fireEvent.mouseDown(await screen.findByRole("combobox", { name: "Année" }));
  fireEvent.click(await screen.findByRole("option", { name: "2027" }));
  expect(valueOf("Talks soumis")).toBe("3");
  vi.mocked(apiFetch).mockResolvedValue(Response.json({
    currentYear: 2026,
    years: [{ year: 2026, publishedArticles: 5, submittedCandidatures: 1, acceptedCandidatures: 1 }],
    undatedCandidatures: 0, undatedPublishedArticles: 0,
  }));
  fireEvent.click(screen.getByRole("button", { name: button }));
  await waitFor(() => expect(valueOf("Articles publiés")).toBe("5"));
  expect(screen.getByRole("combobox", { name: "Année" })).toHaveTextContent("2026");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
