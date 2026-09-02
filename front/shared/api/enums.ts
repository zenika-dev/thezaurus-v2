// Généré par `npm run generate:api` depuis api/openapi.json — NE PAS ÉDITER À LA MAIN.
//
// Le pendant runtime de `schema.d.ts` : les mêmes enums, sous forme de valeurs utilisables à
// l'exécution (options de formulaire, `z.enum`). Les types restent dans `contract.ts`.

export const enumValues = {
  BlogPostStatus: ["IDEA", "DRAFT", "REVIEW", "PUBLISHED"],
  ConferenceReach: ["Locale", "Régionale", "Nationale"],
  ConferenceType: ["Marketing / business", "Technique stratégique", "Technique généraliste", "Technique", "Hors scope"],
  MonthLabel: ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"],
  Role: ["ADMIN", "DT", "CONSULTANT"],
  TalkStatus: ["DRAFT", "PLANNED", "SUBMITTED", "ACCEPTED", "REJECTED", "DONE"],
  Visibility: ["PUBLIC", "PRIVATE"],
} as const;
