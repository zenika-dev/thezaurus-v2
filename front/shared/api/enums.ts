// Généré par `npm run generate:api` depuis api/openapi.json — NE PAS ÉDITER À LA MAIN.
//
// Le pendant runtime de `schema.d.ts` : les mêmes enums, sous forme de valeurs utilisables à
// l'exécution (options de formulaire, `z.enum`). Les types sont dans `contract.ts`.

export const BlogPostStatus = ["IDEA", "DRAFT", "REVIEW", "PUBLISHED"] as const;
export type BlogPostStatus = (typeof BlogPostStatus)[number];

export const ConferenceReach = ["Locale", "Régionale", "Nationale"] as const;
export type ConferenceReach = (typeof ConferenceReach)[number];

export const ConferenceType = ["Marketing / business", "Technique stratégique", "Technique généraliste", "Technique", "Hors scope"] as const;
export type ConferenceType = (typeof ConferenceType)[number];

export const DatePrecision = ["DAY", "MONTH"] as const;
export type DatePrecision = (typeof DatePrecision)[number];

export const MonthLabel = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"] as const;
export type MonthLabel = (typeof MonthLabel)[number];

export const Role = ["ADMIN", "DT", "CONSULTANT"] as const;
export type Role = (typeof Role)[number];

export const TalkStatus = ["DRAFT", "PLANNED", "SUBMITTED", "ACCEPTED", "REJECTED", "DONE"] as const;
export type TalkStatus = (typeof TalkStatus)[number];

export const Visibility = ["PUBLIC", "PRIVATE"] as const;
export type Visibility = (typeof Visibility)[number];
