"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminUserApi } from "@/entities/user";
import { queryKeys, type BackendUserAdminView, type Role } from "@/shared/api";

export function useAdminUsers() {
  const queryClient = useQueryClient();

  const {
    data: users = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: () => adminUserApi.getAdminUsers(),
  });

  const updateRolesMutation = useMutation({
    mutationFn: ({ email, roles }: { email: string; roles: Role[] }) =>
      adminUserApi.updateUserRoles(email, roles),
    onSuccess: async (updatedUser: BackendUserAdminView) => {
      queryClient.setQueryData<BackendUserAdminView[]>(
        queryKeys.admin.users(),
        (old = []) =>
          old.map((u) => (u.email.toLowerCase() === updatedUser.email.toLowerCase() ? updatedUser : u)),
      );
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
    },
  });

  return {
    users,
    isLoading,
    isError,
    error,
    refetch,
    updateRoles: updateRolesMutation.mutateAsync,
    isUpdating: updateRolesMutation.isPending,
  };
}
