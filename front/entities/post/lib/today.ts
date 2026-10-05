/** Même jour de référence que l’horloge de l’API, indépendamment du fuseau du navigateur. */
export function blogPostToday(): string {
  return new Intl.DateTimeFormat("fr-FR", {
    timeZone: "Europe/Paris", year: "numeric", month: "2-digit", day: "2-digit",
  }).format(new Date()).replaceAll("/", "-");
}
