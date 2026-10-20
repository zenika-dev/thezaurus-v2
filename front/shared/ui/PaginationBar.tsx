"use client";

import MenuItem from "@mui/material/MenuItem";
import Pagination from "@mui/material/Pagination";
import Select from "@mui/material/Select";

export interface PaginationBarProps {
  page: number; // 1-indexed
  pageSize: number;
  total: number;
  totalPages?: number;
  onPageChange: (newPage: number) => void;
  onPageSizeChange: (newPageSize: number) => void;
  pageSizeOptions?: number[];
  label?: string;
  ariaLabel?: string;
  className?: string;
}

export function PaginationBar({
  page,
  pageSize,
  total,
  totalPages: customTotalPages,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 25, 50, 100],
  label = "Éléments par page :",
  ariaLabel,
  className = "",
}: PaginationBarProps) {
  const calculatedTotalPages = Math.ceil(total / pageSize);
  const totalPages =
    customTotalPages !== undefined && customTotalPages >= 0
      ? customTotalPages
      : calculatedTotalPages;
  const startItem = total > 0 ? (page - 1) * pageSize + 1 : 0;
  const endItem = total > 0 ? Math.min(page * pageSize, total) : 0;

  return (
    <div
      className={`sticky bottom-0 bg-surface/95 backdrop-blur-xs py-3 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4 mt-auto -mx-8 px-8 z-10 shrink-0 ${className}`}
    >
      <div className="flex items-center gap-2 text-xs text-text-muted">
        <span>{label}</span>
        <Select
          size="small"
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
          className="text-xs bg-surface"
          inputProps={{
            "aria-label": ariaLabel ?? label.replace(":", "").trim(),
          }}
          sx={{
            "& .MuiSelect-select": {
              py: "4px",
              px: "8px",
              fontSize: "0.75rem",
            },
          }}
        >
          {pageSizeOptions.map((option) => (
            <MenuItem key={option} value={option} className="text-xs">
              {option}
            </MenuItem>
          ))}
        </Select>
        <span className="ml-2">
          {total > 0 ? `${startItem}–${endItem} sur ${total}` : "0 sur 0"}
        </span>
      </div>

      <Pagination
        count={Math.max(1, totalPages)}
        page={page}
        onChange={(_, newPage) => onPageChange(newPage)}
        color="primary"
        shape="rounded"
        showFirstButton
        showLastButton
        disabled={totalPages <= 1}
      />
    </div>
  );
}
