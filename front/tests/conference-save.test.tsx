import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import dayjs, { type Dayjs } from "dayjs";
import { CreateConferenceDialog } from "@/features/conferences/ui/CreateConferenceDialog";

// Exercise the form lifecycle independently of the calendar's segmented input UI.
vi.mock("@mui/x-date-pickers", async importOriginal => ({
  ...await importOriginal<typeof import("@mui/x-date-pickers")>(),
  DatePicker: ({ label, value, onChange, disabled }: {
    label: string; value: Dayjs | null; onChange: (value: Dayjs | null) => void; disabled: boolean;
  }) => <label>{label}<input aria-label={label} disabled={disabled} value={value?.format("YYYY-MM-DD") ?? ""}
    onChange={event => onChange(event.target.value ? dayjs(event.target.value) : null)} /></label>,
}));

afterEach(() => vi.unstubAllGlobals());

it.each(["single", "range", "month"] as const)("conserve toute la saisie après un échec (%s), puis ferme seulement après réussite", async mode => {
  vi.stubGlobal("fetch", vi.fn(async () => Response.json([])));
  const user = userEvent.setup();
  let reject!: (error: Error) => void;
  let resolve!: () => void;
  const onSubmit = vi.fn()
    .mockImplementationOnce(() => new Promise<void>((_resolve, fail) => { reject = fail; }))
    .mockImplementationOnce(() => new Promise<void>(done => { resolve = done; }));
  const onClose = vi.fn();
  render(<CreateConferenceDialog open onSubmit={onSubmit} onClose={onClose} />);

  await user.type(screen.getByRole("textbox", { name: /Titre de la conférence/ }), "Conférence à conserver");
  await user.type(screen.getByRole("combobox", { name: /Lieu/ }), "Paris, France");
  await user.type(screen.getByRole("textbox", { name: "Lien CFP" }), "https://example.com/cfp");
  await user.click(screen.getByRole("combobox", { name: "Type" }));
  await user.click(screen.getByRole("option", { name: "Technique" }));
  await user.click(screen.getByRole("combobox", { name: "Portée" }));
  await user.click(screen.getByRole("option", { name: "Nationale" }));
  if (mode === "range") await user.click(screen.getByRole("button", { name: /Ajouter une date de fin/ }));
  if (mode === "month") await user.click(screen.getByRole("button", { name: "Mois uniquement" }));
  const dateLabel = mode === "single" ? "Date" : mode === "range" ? "Date de début" : "Mois";
  fireEvent.change(screen.getByLabelText(dateLabel, { exact: true }), { target: { value: "2030-06-10" } });
  if (mode === "range") fireEvent.change(screen.getByLabelText("Date de fin", { exact: true }), { target: { value: "2030-06-12" } });
  fireEvent.change(screen.getByLabelText("Date de début du CFP"), { target: { value: "2030-01-01" } });
  fireEvent.change(screen.getByLabelText("Date de fin du CFP"), { target: { value: "2030-05-01" } });

  await user.click(screen.getByRole("button", { name: "Créer la conférence" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  expect(onClose).not.toHaveBeenCalled();
  expect(screen.getByRole("button", { name: "Création…" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Annuler" })).toBeDisabled();
  expect(screen.queryByRole("button", { name: "Fermer" })).not.toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: /Titre de la conférence/ })).toBeDisabled();
  expect(screen.getByLabelText(dateLabel, { exact: true })).toBeDisabled();
  await user.keyboard("{Escape}");
  fireEvent.submit(document.getElementById("create-conference-form")!);
  expect(onClose).not.toHaveBeenCalled();
  expect(onSubmit).toHaveBeenCalledTimes(1);

  await act(async () => reject(new Error("Serveur indisponible")));
  expect(await screen.findByRole("alert")).toHaveTextContent("Vos saisies sont conservées");
  expect(onClose).not.toHaveBeenCalled();
  expect(screen.getByRole("textbox", { name: /Titre de la conférence/ })).toHaveValue("Conférence à conserver");
  expect(screen.getByRole("combobox", { name: /Lieu/ })).toHaveValue("Paris, France");
  expect(screen.getByLabelText(dateLabel, { exact: true })).toHaveValue("2030-06-10");
  await user.click(screen.getByRole("button", { name: "Créer la conférence" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(2));
  const first = onSubmit.mock.calls[0][0];
  expect(first).toMatchObject({ name: "Conférence à conserver", type: "Technique", reach: "Nationale",
    cfpLink: "https://example.com/cfp", cfpOpeningDate: "2030-01-01", cfpClosingDate: "2030-05-01",
    date: mode === "month" ? { start: "2030-06-01", end: "2030-06-30", precision: "MONTH" }
      : { start: "2030-06-10", end: mode === "range" ? "2030-06-12" : "2030-06-10", precision: "DAY" },
  });
  expect(onSubmit.mock.calls[1][0]).toEqual({ ...first, id: expect.any(String) });
  expect(onClose).not.toHaveBeenCalled();
  await act(async () => resolve());
  await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
  expect(screen.getByRole("textbox", { name: /Titre de la conférence/ })).toHaveValue("");
  expect(screen.getByRole("combobox", { name: /Lieu/ })).toHaveValue("");
});
