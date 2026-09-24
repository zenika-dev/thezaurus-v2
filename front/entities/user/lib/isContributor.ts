/** Un nom ou deux emails absents ne permettent jamais d'établir une appartenance. */
export function isContributor(
  contributors: readonly { email?: string | null }[] | null | undefined,
  email: string | null | undefined,
): boolean {
  const normalizedEmail = email?.trim().toLowerCase();
  return Boolean(normalizedEmail && contributors?.some(
    (person) => person.email?.trim().toLowerCase() === normalizedEmail,
  ));
}
