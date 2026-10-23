import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { UserProfile } from "@/entities/user";
import { apiFetch, queryKeys } from "@/shared/api";
import { AiPhotoSection } from "./AiPhotoSection";

vi.mock("@/shared/api", async (original) => ({
  ...await original<typeof import("@/shared/api")>(), apiFetch: vi.fn(),
}));

const profile: UserProfile = {
  name: "Jane", email: "jane@zenika.com", office: "", slackAvailable: false,
  notificationPreferences: { email: false, slack: false }, aiPhotoAllowed: false, customPhoto: false,
};

beforeEach(() => vi.clearAllMocks());

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  client.setQueryData(queryKeys.profile.me(), profile);
  render(<QueryClientProvider client={client}><AiPhotoSection profile={profile} /></QueryClientProvider>);
  return client;
}

it("starts denied and saves only the current person's choice", async () => {
  vi.mocked(apiFetch).mockResolvedValue(new Response(null, { status: 204 }));
  const client = setup();
  const toggle = screen.getByRole("switch");
  expect(toggle).not.toBeChecked();
  fireEvent.click(toggle);
  await waitFor(() => expect(client.getQueryData<UserProfile>(queryKeys.profile.me())?.aiPhotoAllowed).toBe(true));
  expect(apiFetch).toHaveBeenCalledWith("/api/me/profile/ai-photo", expect.objectContaining({
    method: "PUT", body: JSON.stringify({ allowed: true }),
  }));
});

it("retains the recorded choice and reports a failed save", async () => {
  vi.mocked(apiFetch).mockResolvedValue(new Response(null, { status: 500 }));
  const client = setup();
  fireEvent.click(screen.getByRole("switch"));
  expect(await screen.findByRole("alert")).toHaveTextContent("n’a pas pu être enregistrée");
  expect(client.getQueryData<UserProfile>(queryKeys.profile.me())?.aiPhotoAllowed).toBe(false);
});
