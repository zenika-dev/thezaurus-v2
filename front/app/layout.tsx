import type { Metadata } from "next";
import { Nunito } from "next/font/google";
import { AppRouterCacheProvider } from "@mui/material-nextjs/v16-appRouter";
import "./globals.css";
import Providers from "@/widgets/Providers";
import SideMenu from "@/widgets/SideMenu";
import { ProtectedRoute } from "@/features/auth";

const nunito = Nunito({
  subsets: ["latin"],
  variable: "--font-nunito",
  display: "swap",
  preload: true,
  weight: ["400", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: { template: "%s | Thezaurus", default: "Thezaurus" },
  description:
    "Thezaurus by Zenika – gestion des talks, blog posts et conférences",
  icons: { icon: "/favicon.ico" },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="fr"
      className={`${nunito.variable} h-full`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex w-full">
        <AppRouterCacheProvider>
          <Providers>
            <ProtectedRoute allowedRoles={["membre", "admin"]}>
              <SideMenu />
              <main className="flex-1 flex flex-col min-w-0 overflow-auto bg-surface">
                {children}
              </main>
            </ProtectedRoute>
          </Providers>
        </AppRouterCacheProvider>
      </body>
    </html>
  );
}
