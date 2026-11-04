import { describe, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api";
import { eventApi } from "@/entities/event/api";
vi.mock("@/shared/api", () => ({ apiFetch: vi.fn(), MonthLabel: ["Jan"] }));
describe("events dashboard API", () => {
  it("loads independent yearly responses from the backend", async () => {
    for (const year of [2025, 2026]) {
      const data = { year, totals: { internal: year - 2025, external: 2 }, monthly: [], eventTypes: [] };
      vi.mocked(apiFetch).mockResolvedValueOnce(new Response(JSON.stringify(data)));
      expect(await eventApi.getEventsDashboard(year)).toEqual(data);
      expect(apiFetch).toHaveBeenLastCalledWith(`/events/dashboard?year=${year}`, { cache: "no-store" });
    }
  });
  it("does not substitute fake data when the backend fails", async () => {
    vi.mocked(apiFetch).mockResolvedValueOnce(new Response(null, { status: 500 }));
    await expect(eventApi.getEventsDashboard(2026)).rejects.toThrow();
  });
});
