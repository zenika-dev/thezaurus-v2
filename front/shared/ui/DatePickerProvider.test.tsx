import { DateCalendar } from "@mui/x-date-pickers";
import { render, screen, within } from "@testing-library/react";
import dayjs from "dayjs";
import { expect, it } from "vitest";
import { DatePickerProvider } from "./DatePickerProvider";

it("uses the account locale for the week order of mounted calendars", () => {
  const calendar = <DateCalendar defaultValue={dayjs("2026-11-24")} />;
  const { rerender } = render(<DatePickerProvider locale="fr">{calendar}</DatePickerProvider>);

  const weekdayNames = () => within(screen.getByRole("grid")).getAllByRole("columnheader")
    .map((header) => header.getAttribute("aria-label"));

  expect(weekdayNames()).toEqual(["lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"]);

  rerender(<DatePickerProvider locale="en">{calendar}</DatePickerProvider>);

  expect(weekdayNames()).toEqual(["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]);
});
