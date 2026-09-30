import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/shared/api";

let browserQueryClient: QueryClient | undefined;

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        retry: (failureCount, error) => {
          if (
            (error instanceof ApiError && (error.status === 401 || error.status === 403)) ||
            (error instanceof Error &&
              (error.message.includes("401") ||
                error.message.includes("403") ||
                error.message.includes("Unauthorized") ||
                error.message.includes("Not authenticated")))
          ) {
            return false;
          }
          return failureCount < 3;
        },
      },
    },
  });
}

export function getQueryClient() {
  if (typeof window === "undefined") {
    return makeQueryClient();
  }
  if (!browserQueryClient) browserQueryClient = makeQueryClient();
  return browserQueryClient;
}
