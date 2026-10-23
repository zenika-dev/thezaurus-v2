"use client";

import type { ZenikaButtonProps } from "./ZenikaButton";
import { ZenikaButton } from "./ZenikaButton";

/** Shared appearance for the creation actions in page headers. */
export function CreateButton({ className, ...props }: Omit<ZenikaButtonProps, "variant">) {
  return <ZenikaButton {...props} variant="primary" className={`gap-2 ${className ?? ""}`} />;
}
