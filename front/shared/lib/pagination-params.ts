import type { PaginationParams } from "@/shared/api";

export const DEFAULT_PAGE_SIZE = 10;

export function parsePaginationSearchParams(
  rawParams: { [key: string]: string | string[] | undefined },
  validStatuses?: readonly string[],
  defaultPageSize = DEFAULT_PAGE_SIZE
): PaginationParams {
  const pageStr = typeof rawParams.page === "string" ? rawParams.page : undefined;
  const pageSizeStr = typeof rawParams.pageSize === "string" ? rawParams.pageSize : undefined;
  const statusStr = typeof rawParams.status === "string" ? rawParams.status : undefined;
  const personalStr = typeof rawParams.personal === "string" ? rawParams.personal : undefined;

  const page = pageStr && !isNaN(Number(pageStr)) && Number(pageStr) >= 1 ? parseInt(pageStr, 10) : 1;
  const pageSizeNum = pageSizeStr && !isNaN(Number(pageSizeStr)) ? parseInt(pageSizeStr, 10) : defaultPageSize;
  const pageSize = [10, 25, 50, 100].includes(pageSizeNum) ? pageSizeNum : defaultPageSize;

  let status: string | undefined = undefined;
  if (statusStr && statusStr !== "All") {
    if (!validStatuses || validStatuses.includes(statusStr)) {
      status = statusStr;
    }
  }

  const personal = personalStr === "true" ? true : undefined;

  return {
    page: page - 1,
    pageSize,
    status,
    personal,
  };
}
