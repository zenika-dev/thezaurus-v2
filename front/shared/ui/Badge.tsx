interface BadgeProps {
  color: string;
  bg: string;
  children: React.ReactNode;
}

export function Badge({ color, bg, children }: BadgeProps) {
  return (
    <span
      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-2xl text-xs font-bold border"
      style={{ color, backgroundColor: bg, borderColor: bg }}
    >
      {children}
    </span>
  );
}
