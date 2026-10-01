export function normalizeText(text: string): string {
  return text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

export function areContributorsEqual(
  first: { name: string; email?: string | null },
  second: { name: string; email?: string | null },
): boolean {
  if (
    first.email &&
    second.email &&
    first.email.trim().length > 0 &&
    second.email.trim().length > 0
  ) {
    return first.email.trim().toLowerCase() === second.email.trim().toLowerCase();
  }
  return first.name.trim().toLowerCase() === second.name.trim().toLowerCase();
}
