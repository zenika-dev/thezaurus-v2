import { render as renderComponent, type RenderOptions } from "@testing-library/react";
import type { ReactNode } from "react";
import { DatePickerProvider } from "@/shared/ui/DatePickerProvider";

export function render(ui: ReactNode, options?: Omit<RenderOptions, "wrapper">) {
  return renderComponent(ui, {
    ...options,
    wrapper: ({ children }) => <DatePickerProvider locale="fr">{children}</DatePickerProvider>,
  });
}
