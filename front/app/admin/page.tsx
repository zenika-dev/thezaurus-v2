import { Suspense } from "react";
import type { Metadata } from "next";
import { HydrationBoundary, dehydrate } from "@tanstack/react-query";
import { adminUserApi } from "@/entities/user";
import { queryKeys } from "@/shared/api";
import { getQueryClient } from "@/shared/lib";
import { ProtectedRoute } from "@/features/auth";
import { AdminDashboard, AdminSkeleton } from "@/features/admin";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Administration",
};

export default function AdminPage() {
  return (
    <ProtectedRoute allowedRoles={["ADMIN"]}>
      <Suspense fallback={<AdminSkeleton />}>
        <AdminLoader />
      </Suspense>
    </ProtectedRoute>
  );
}

async function AdminLoader() {
  const queryClient = getQueryClient();
  try {
    await queryClient.prefetchQuery({
      queryKey: queryKeys.admin.users(),
      queryFn: () => adminUserApi.getAdminUsers(),
    });
  } catch (err) {
    console.error("Erreur lors du préchargement des utilisateurs administrateurs :", err);
  }

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <AdminDashboard />
    </HydrationBoundary>
  );
}
