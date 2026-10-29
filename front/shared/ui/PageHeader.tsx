import type { ReactNode } from "react";

export interface PageHeaderProps {
  title: string;
  description?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  children?: ReactNode;
  className?: string;
}

export function PageHeader({
  title,
  description,
  subtitle,
  action,
  children,
  className = "",
}: PageHeaderProps) {
  const desc = description ?? subtitle;
  return (
    <header className={`flex flex-col gap-4 mb-8 sm:flex-row sm:items-center sm:justify-between ${className}`}>
      <div className="min-w-0">
        <h1 className="text-3xl sm:text-[2.125rem] font-extrabold text-text leading-tight wrap-anywhere">{title}</h1>
        {desc && <div className="text-text-muted mt-2">{desc}</div>}
        {children}
      </div>
      {action && <div className="flex flex-wrap items-center gap-2 shrink-0">{action}</div>}
    </header>
  );
}
