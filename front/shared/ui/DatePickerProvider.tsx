"use client";

import { LocalizationProvider } from "@mui/x-date-pickers";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { enUS, frFR } from "@mui/x-date-pickers/locales";
import "dayjs/locale/fr";

const pickerTranslations = {
  en: enUS.components.MuiLocalizationProvider.defaultProps.localeText,
  fr: frFR.components.MuiLocalizationProvider.defaultProps.localeText,
};

export function DatePickerProvider({
  children,
  locale,
}: { children: React.ReactNode; locale: keyof typeof pickerTranslations }) {
  return (
    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale={locale} localeText={pickerTranslations[locale]}>
      {children}
    </LocalizationProvider>
  );
}
