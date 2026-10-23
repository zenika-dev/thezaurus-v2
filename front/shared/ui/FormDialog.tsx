"use client";

import { useId, type ReactNode } from "react";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import CircularProgress from "@mui/material/CircularProgress";
import { X } from "lucide-react";
import { ResponsiveDialog } from "./ResponsiveDialog";

export interface FormDialogProps {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  subtitle?: ReactNode;
  headerAction?: ReactNode;
  children: ReactNode;
  actions?: ReactNode;
  maxWidth?: "xs" | "sm" | "md" | "lg" | "xl";
  fullWidth?: boolean;
  titleId?: string;
  formId?: string;
  onCancel?: () => void;
  cancelLabel?: string;
  submitLabel?: string;
  onSubmit?: () => void;
  isSubmitting?: boolean;
  disableSubmit?: boolean;
  showCloseButton?: boolean;
  customActions?: boolean;
  secondaryAction?: ReactNode;
  className?: string;
}

export function FormDialog({
  open,
  onClose,
  title,
  subtitle,
  headerAction,
  children,
  actions,
  maxWidth = "sm",
  fullWidth = true,
  titleId,
  formId,
  onCancel,
  cancelLabel = "Annuler",
  submitLabel,
  onSubmit,
  isSubmitting = false,
  disableSubmit = false,
  showCloseButton = true,
  customActions = false,
  secondaryAction,
  className = "",
}: FormDialogProps) {
  const handleCancel = onCancel ?? onClose;
  const generatedTitleId = useId();
  const dialogTitleId = titleId ?? generatedTitleId;

  return (
    <ResponsiveDialog
      open={open}
      onClose={onClose}
      maxWidth={maxWidth}
      fullWidth={fullWidth}
      aria-labelledby={dialogTitleId}
      className={className}
    >
      <DialogTitle
        id={dialogTitleId}
        className="pb-2 flex flex-wrap justify-between items-center gap-3 font-semibold tracking-[-0.75px]"
      >
        <span className="min-w-0 flex-1 flex flex-wrap items-center gap-3">
          <span className="wrap-anywhere">{title}</span>
          {headerAction}
        </span>
        {showCloseButton && (
          <IconButton
            aria-label="Fermer"
            size="small"
            onClick={onClose}
            sx={{ flexShrink: 0, color: "text.secondary" }}
          >
            <X size={20} />
          </IconButton>
        )}
      </DialogTitle>

      <DialogContent className="pb-2">
        {subtitle && (
          <p className="text-sm text-text-muted mb-4">{subtitle}</p>
        )}
        {children}
      </DialogContent>

      {!customActions && (actions || submitLabel || cancelLabel || secondaryAction) && (
        <DialogActions className="px-6 pb-6 pt-5 justify-between">
          <Button variant="outlined" onClick={handleCancel}>
            {cancelLabel}
          </Button>
          <div className="flex-1" />
          {actions}
          {secondaryAction}
          {submitLabel && (
            <Button
              variant="contained"
              type={formId ? "submit" : "button"}
              form={formId}
              onClick={!formId ? onSubmit : undefined}
              disabled={disableSubmit || isSubmitting}
              startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : undefined}
            >
              {submitLabel}
            </Button>
          )}
        </DialogActions>
      )}

      {customActions && actions && (
        <DialogActions className="px-6 pb-6 pt-5">
          {actions}
        </DialogActions>
      )}
    </ResponsiveDialog>
  );
}
