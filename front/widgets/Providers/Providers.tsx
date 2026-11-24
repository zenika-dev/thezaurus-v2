"use client";

import { useState } from "react";
import { QueryClientProvider, useQuery } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { getQueryClient, MuiThemeProvider } from "@/shared/lib";
import { ThemeProvider as NextThemeProvider } from "next-themes";
import { SessionProvider } from "@/features/auth";
import { useSession } from "next-auth/react";
import { defaultCalendarLocale, profileApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import { DatePickerProvider } from "@/shared/ui";

function AccountDatePickerProvider({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession();
  const accountEmail = status === "authenticated" && !session?.error
    ? session?.user?.email?.toLowerCase()
    : undefined;
  const { data: profile } = useQuery({
    queryKey: queryKeys.profile.me(),
    queryFn: profileApi.getProfile,
    enabled: Boolean(accountEmail),
  });
  const locale = profile && profile.email.toLowerCase() === accountEmail
    ? profile.calendarLocale
    : defaultCalendarLocale;

  return <DatePickerProvider locale={locale}>{children}</DatePickerProvider>;
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => getQueryClient());

  return (
    <SessionProvider>
      <NextThemeProvider attribute="class" defaultTheme="system" enableSystem>
        <QueryClientProvider client={queryClient}>
          <MuiThemeProvider>
            <AccountDatePickerProvider>{children}</AccountDatePickerProvider>
          </MuiThemeProvider>
          {process.env.NODE_ENV === "development" && (
            <ReactQueryDevtools initialIsOpen={false} />
          )}
        </QueryClientProvider>
      </NextThemeProvider>
    </SessionProvider>
  );
}

export default Providers;
