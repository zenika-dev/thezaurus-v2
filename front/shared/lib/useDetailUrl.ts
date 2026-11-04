"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";

/** Selection is independent of the list's filters and pagination. */
export function useDetailUrl<T extends { id: string }>(
  parameter: string, queryScope: string, items: T[], fetchDetail: (id: string) => Promise<T>,
) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const selectedId = searchParams.get(parameter) || null;
  const listedItem = items.find(item => item.id === selectedId);
  const detail = useQuery({
    queryKey: [queryScope, "detail", selectedId],
    queryFn: () => fetchDetail(selectedId!),
    enabled: !!selectedId && !listedItem,
    retry: false,
  });
  const href = (id: string | null) => {
    const params = new URLSearchParams(searchParams.toString());
    if (id) params.set(parameter, id); else params.delete(parameter);
    const query = params.toString();
    return `${pathname}${query ? `?${query}` : ""}`;
  };
  const select = (id: string | null) => router.push(href(id), { scroll: false });
  return {
    selected: listedItem ?? detail.data ?? null,
    selectedId,
    href,
    select,
    close: () => select(null),
    loading: !!selectedId && !listedItem && detail.isPending,
    error: !!selectedId && !listedItem && detail.isError,
    retry: () => { void detail.refetch(); },
  };
}
