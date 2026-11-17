import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { signOut } from "next-auth/react";
import { DownloadVisualButton } from "./DownloadVisualButton";

vi.mock("next-auth/react", () => ({ signOut: vi.fn().mockResolvedValue(undefined) }));
afterEach(() => { vi.unstubAllGlobals(); vi.clearAllMocks(); });

it("uses the shared session handler on 401 without reporting an agent configuration error", async () => {
  const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 401 });
  vi.stubGlobal("fetch", fetchMock);
  render(<DownloadVisualButton endpoint="/talks/id/visual" filename="talk.png"
    forbiddenError="Accès refusé" missingError="Introuvable" />);
  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));
  await waitFor(() => expect(signOut).toHaveBeenCalledWith({ callbackUrl: "/" }));
  expect(fetchMock).toHaveBeenCalledWith("/api/backend/talks/id/visual", { method: "POST" });
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
