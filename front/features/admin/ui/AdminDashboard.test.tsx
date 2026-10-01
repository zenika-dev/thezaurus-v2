import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { AdminDashboard } from "./AdminDashboard";

vi.mock("./UserRolesTable", () => ({ UserRolesTable: () => <p>Liste des utilisateurs</p> }));
vi.mock("@/shared/ui", () => ({ DataErrorBoundary: ({ children }: { children: React.ReactNode }) => children }));
vi.mock("@/features/admin", () => ({
  MessageTemplatesSection: ({ onDirtyChange }: { onDirtyChange: (dirty: boolean) => void }) =>
    <input aria-label="Modèle" onChange={() => onDirtyChange(true)} />,
}));
afterEach(cleanup);

it("warns before changing admin tabs and retains the mounted editor when cancelled", () => {
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  render(<AdminDashboard />);
  fireEvent.click(screen.getByRole("button", { name: "Templates de messages" }));
  fireEvent.change(screen.getByRole("textbox", { name: "Modèle" }), { target: { value: "Mon texte" } });
  fireEvent.click(screen.getByRole("button", { name: "Rôles utilisateurs" }));
  expect(confirm).toHaveBeenCalledOnce();
  expect((screen.getByRole("textbox", { name: "Modèle" }) as HTMLInputElement).value).toBe("Mon texte");
  confirm.mockReturnValue(true);
  fireEvent.click(screen.getByRole("button", { name: "Rôles utilisateurs" }));
  expect(screen.queryByRole("textbox", { name: "Modèle" })).toBeNull();
  expect(screen.getByText("Liste des utilisateurs")).toBeTruthy();
});
