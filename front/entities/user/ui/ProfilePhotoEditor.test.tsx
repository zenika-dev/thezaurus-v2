import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { apiFetch } from "@/shared/api";
import { ProfilePhotoEditor } from "./ProfilePhotoEditor";

vi.mock("@/shared/api", async (original) => ({
  ...await original<typeof import("@/shared/api")>(), apiFetch: vi.fn(),
}));
beforeEach(() => vi.clearAllMocks());
function setup(admin = false) {
  render(<QueryClientProvider client={new QueryClient()}><ProfilePhotoEditor email="jane@zenika.com" admin={admin} /></QueryClientProvider>);
}

it("uploads binary photo data to the admin endpoint without changing consent", async () => {
  vi.mocked(apiFetch).mockResolvedValue(new Response(null, { status: 204 }));
  setup(true);
  const file = new File([new Uint8Array([1, 2, 255])], "photo.webp", { type: "image/webp" });
  fireEvent.change(screen.getByLabelText("Charger la photo de jane@zenika.com"), { target: { files: [file] } });
  await screen.findByText("Photo enregistrée.");
  expect(apiFetch).toHaveBeenCalledExactlyOnceWith("/api/admin/users/jane%40zenika.com/photo", expect.objectContaining({
    method: "PUT", body: file, headers: { "Content-Type": "image/webp" },
  }));
});

it("rejects invalid files before sending and lets the person reset to Google", async () => {
  vi.mocked(apiFetch).mockResolvedValue(new Response(null, { status: 204 }));
  setup();
  const file = new File(["bad"], "photo.svg", { type: "image/svg+xml" });
  fireEvent.change(screen.getByLabelText("Charger la photo de jane@zenika.com"), { target: { files: [file] } });
  expect(await screen.findByRole("alert")).toHaveTextContent("5 Mo maximum");
  expect(apiFetch).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Utiliser ma photo Google" }));
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/me/profile/photo", expect.objectContaining({ method: "DELETE" })));
  await screen.findByText("Photo Google rétablie.");
});
