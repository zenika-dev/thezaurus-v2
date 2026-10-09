import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NotificationPreferencesSection } from "./NotificationPreferencesSection";
import type { UserProfile } from "@/entities/user";

const mockMutate = vi.fn();
vi.mock("../model/useProfileMutations", () => ({
  useProfileMutations: () => ({
    updateNotificationPreferences: {
      mutate: mockMutate,
      isError: false,
    },
  }),
}));

const baseProfile: UserProfile = {
  name: "Jane Doe",
  email: "jane@zenika.com",
  office: "nantes",
  slackAvailable: true,
  notificationPreferences: {
    email: true,
    slack: false,
  },
};

describe("NotificationPreferencesSection", () => {
  it("enables Slack toggle when Slack is active on the backend", () => {
    render(<NotificationPreferencesSection profile={baseProfile} />);

    const slackSwitch = screen.getByRole("switch", { name: "Me notifier sur Slack" });
    expect(slackSwitch).not.toBeDisabled();
    expect(slackSwitch).not.toBeChecked();

    fireEvent.click(slackSwitch);
    expect(mockMutate).toHaveBeenCalledWith({ email: true, slack: true });
  });

  it("disables Slack toggle and displays message when Slack integration is disabled", () => {
    const disabledProfile: UserProfile = {
      ...baseProfile,
      slackAvailable: false,
    };

    render(<NotificationPreferencesSection profile={disabledProfile} />);

    const slackSwitch = screen.getByRole("switch", { name: "Me notifier sur Slack" });
    expect(slackSwitch).toBeDisabled();
    expect(screen.getByText("L'intégration Slack est désactivée sur cet environnement.")).toBeInTheDocument();
  });
});
