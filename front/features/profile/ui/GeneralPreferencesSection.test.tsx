import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it, vi } from "vitest";
import { updateCalendarLocaleAction } from "@/entities/user/actions";
import type { UserProfile } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import { GeneralPreferencesSection } from "./GeneralPreferencesSection";

vi.mock("@/entities/user/actions", () => ({
  updateCalendarLocaleAction: vi.fn(),
  updateOfficeAction: vi.fn(),
}));

const profile: UserProfile = {
  name: "Alice",
  email: "alice@example.test",
  office: "",
  calendarLocale: "fr",
  notificationPreferences: { email: false, slack: false },
  slackAvailable: false,
  aiPhotoAllowed: false,
  customPhoto: false,
};

function renderPreferences() {
  const client = new QueryClient();
  client.setQueryData(queryKeys.profile.me(), profile);
  render(
    <QueryClientProvider client={client}>
      <GeneralPreferencesSection profile={profile} />
    </QueryClientProvider>,
  );
  return client;
}

it("saves the language on selection and updates the shared profile", async () => {
  vi.mocked(updateCalendarLocaleAction).mockResolvedValueOnce(undefined);
  const client = renderPreferences();

  expect(screen.queryByRole("button", { name: "Enregistrer la langue" })).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("radio", { name: "English" }));

  expect(updateCalendarLocaleAction).toHaveBeenCalledWith("en");
  await waitFor(() => expect(client.getQueryData<UserProfile>(queryKeys.profile.me())?.calendarLocale).toBe("en"));
});

it("restores the saved language if the request fails", async () => {
  vi.mocked(updateCalendarLocaleAction).mockRejectedValueOnce(new Error("Request failed"));
  renderPreferences();

  await userEvent.click(screen.getByRole("radio", { name: "English" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("La langue des calendriers n’a pas été enregistrée");
  expect(screen.getByRole("radio", { name: "Français" })).toBeChecked();
});
