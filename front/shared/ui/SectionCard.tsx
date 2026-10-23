"use client";

import type { HTMLAttributes, ReactNode } from "react";

export interface SectionCardProps extends Omit<HTMLAttributes<HTMLElement>, "title"> {
  title?: ReactNode;
  subtitle?: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  header?: ReactNode;
  titleId?: string;
  className?: string;
  children?: ReactNode;
}

export function SectionCard({
  title,
  subtitle,
  description,
  action,
  header,
  titleId,
  className = "",
  children,
  ...props
}: SectionCardProps) {
  const sub = description ?? subtitle;
  const hasHeaderContent = Boolean(title || sub || action || header);

  return (
    <section
      aria-labelledby={titleId}
      className={`rounded-2xl border border-primary bg-surface p-4 sm:p-6 shadow-card ${className}`}
      {...props}
    >
      {header}
      {!header && hasHeaderContent && (
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-4">
          <div className="min-w-0">
            {title && (
              <h2
                id={titleId}
                className="text-xl leading-relaxed font-bold text-text m-0 mb-1"
              >
                {title}
              </h2>
            )}
            {sub && (
              <p className="text-sm leading-normal text-text-muted m-0">
                {sub}
              </p>
            )}
          </div>
          {action && <div className="shrink-0 flex items-center gap-2">{action}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
