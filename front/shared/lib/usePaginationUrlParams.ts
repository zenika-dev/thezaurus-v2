"use client";

import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { PaginationParams } from "@/shared/api";
import { DEFAULT_PAGE_SIZE } from "./pagination-params";

export interface PaginationUrlState<TStatus extends string = string> {
  page: number; // 1-indexed for UI & URL
  pageSize: number;
  status: "All" | TStatus;
  personal: boolean;
}

export function usePaginationUrlParams<TStatus extends string>(
  validStatuses: readonly TStatus[],
  defaultPageSize = DEFAULT_PAGE_SIZE
) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const searchParamsString = searchParams ? searchParams.toString() : "";

  const state = useMemo((): PaginationUrlState<TStatus> => {
    const sp = new URLSearchParams(searchParamsString);
    const rawPage = sp.get("page");
    const numPage = rawPage ? parseInt(rawPage, 10) : 1;
    const page = isNaN(numPage) || numPage < 1 ? 1 : numPage;

    const rawSize = sp.get("pageSize");
    const numSize = rawSize ? parseInt(rawSize, 10) : defaultPageSize;
    const pageSize = [10, 25, 50, 100].includes(numSize) ? numSize : defaultPageSize;

    const rawStatus = sp.get("status");
    const status = (rawStatus && validStatuses.includes(rawStatus as TStatus)
      ? (rawStatus as TStatus)
      : ("All" as const)) as "All" | TStatus;

    const personal = sp.get("personal") === "true";

    return { page, pageSize, status, personal };
  }, [searchParamsString, validStatuses, defaultPageSize]);

  const updateUrl = useCallback(
    (updates: Partial<PaginationUrlState<TStatus>>) => {
      const nextState: PaginationUrlState<TStatus> = {
        page: updates.page !== undefined ? updates.page : state.page,
        pageSize: updates.pageSize !== undefined ? updates.pageSize : state.pageSize,
        status: updates.status !== undefined ? updates.status : state.status,
        personal: updates.personal !== undefined ? updates.personal : state.personal,
      };

      const nextParams = new URLSearchParams();

      if (nextState.page > 1) {
        nextParams.set("page", String(nextState.page));
      }

      if (nextState.pageSize !== defaultPageSize) {
        nextParams.set("pageSize", String(nextState.pageSize));
      }

      if (nextState.status !== "All") {
        nextParams.set("status", nextState.status);
      }

      if (nextState.personal) {
        nextParams.set("personal", "true");
      }

      const qs = nextParams.toString();
      const target = `${pathname || ""}${qs ? `?${qs}` : ""}`;
      try {
        router.push(target, { scroll: false });
      } catch {
        // ignore
      }
    },
    [router, pathname, defaultPageSize, state]
  );

  const setStatus = useCallback((s: "All" | TStatus) => updateUrl({ status: s, page: 1 }), [updateUrl]);
  const setPersonal = useCallback((p: boolean) => updateUrl({ personal: p, page: 1 }), [updateUrl]);
  const setPage = useCallback((p: number) => updateUrl({ page: p }), [updateUrl]);
  const setPageSize = useCallback((s: number) => updateUrl({ pageSize: s, page: 1 }), [updateUrl]);
  const resetFilters = useCallback(() => updateUrl({ status: "All", personal: false, page: 1 }), [updateUrl]);

  const apiParams: PaginationParams = useMemo(() => ({
    page: state.page - 1,
    pageSize: state.pageSize,
    status: state.status === "All" ? undefined : state.status,
    personal: state.personal ? true : undefined,
  }), [state.page, state.pageSize, state.status, state.personal]);

  return {
    page: state.page,
    pageSize: state.pageSize,
    status: state.status,
    personal: state.personal,
    setStatus,
    setPersonal,
    setPage,
    setPageSize,
    resetFilters,
    apiParams,
  };
}
