import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it, vi } from "vitest";
import { EventsDashboard } from "@/features/events/ui/EventsDashboard";

const currentYear = new Date().getFullYear();

vi.mock("@/features/events/ui/EventsCounters", () => ({
  EventsCounters: ({ year }: { year: number }) => {
    if (year !== new Date().getFullYear()) throw new Error("Indisponible");
    return <div>Compteurs disponibles</div>;
  },
  EventsCountersSkeleton: () => null,
}));
vi.mock("@/features/events/ui/EventsContent", () => ({
  EventsContent: ({ year }: { year: number }) => {
    if (year !== new Date().getFullYear()) throw new Error("Indisponible");
    return <div>Événements disponibles</div>;
  },
}));

it("récupère les compteurs et le contenu en changeant d’année après une erreur", async () => {
  vi.spyOn(console, "error").mockImplementation(() => {});
  const user = userEvent.setup();
  render(<EventsDashboard />);
  await user.selectOptions(screen.getByLabelText("Année"), String(currentYear - 1));
  expect(await screen.findAllByText("Erreur lors du chargement des données.")).toHaveLength(2);
  await user.selectOptions(screen.getByLabelText("Année"), String(currentYear));
  await waitFor(() => expect(screen.queryByText("Erreur lors du chargement des données.")).not.toBeInTheDocument());
  expect(screen.getByText("Compteurs disponibles")).toBeInTheDocument();
  expect(screen.getByText("Événements disponibles")).toBeInTheDocument();
});
