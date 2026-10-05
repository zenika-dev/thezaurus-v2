import { Office } from "@/shared/api";

export const agencyLabels: Record<string, string> = Object.fromEntries(
  Office.map(office => [office, office === "montreal" ? "Montréal" : office.charAt(0).toUpperCase() + office.slice(1)]),
);
