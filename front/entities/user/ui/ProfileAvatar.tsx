"use client";

import { useEffect, useState } from "react";
import Avatar from "@mui/material/Avatar";
import { getInitials } from "../lib/initials";

export function ProfileAvatar({ email, name, size = 64 }: { email: string; name: string; size?: number }) {
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const refresh = () => setRevision((value) => value + 1);
    window.addEventListener("profile-photo-changed", refresh);
    return () => window.removeEventListener("profile-photo-changed", refresh);
  }, []);
  return (
    <Avatar
      src={`/api/backend/api/users/${encodeURIComponent(email)}/photo?v=${revision}`}
      alt={name}
      sx={{ width: size, height: size, fontSize: size > 32 ? 24 : 11 }}
    >
      {getInitials(name)}
    </Avatar>
  );
}
