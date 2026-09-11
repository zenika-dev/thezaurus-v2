"use client";

import Avatar from "@mui/material/Avatar";
import Chip from "@mui/material/Chip";
import Tooltip from "@mui/material/Tooltip";
import { User } from "lucide-react";
import { getInitials } from "../lib/initials";

export interface SpeakerChipProps {
  name: string;
  email?: string | null;
  onDelete?: (event: React.MouseEvent) => void;
  size?: "small" | "medium";
  className?: string;
}

export function SpeakerChip({
  name,
  email,
  onDelete,
  size = "small",
  className,
}: SpeakerChipProps) {
  const isInternal = Boolean(email && email.trim().length > 0);
  const tooltipTitle = isInternal ? email : "Intervenant externe";
  const avatarSize = size === "small" ? 20 : 24;
  const iconSize = size === "small" ? 12 : 14;

  return (
    <Tooltip title={tooltipTitle} arrow>
      <Chip
        size={size}
        className={className}
        onDelete={onDelete}
        avatar={
          <Avatar
            sx={{
              width: avatarSize,
              height: avatarSize,
              fontSize: size === "small" ? "0.65rem" : "0.75rem",
              bgcolor: isInternal ? "primary.main" : "grey.500",
              color: "white",
            }}
          >
            {isInternal ? getInitials(name) : <User size={iconSize} />}
          </Avatar>
        }
        label={name}
        variant={isInternal ? "filled" : "outlined"}
      />
    </Tooltip>
  );
}
