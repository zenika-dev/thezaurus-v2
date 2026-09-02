// @ts-check
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Génère les *valeurs* des enums du contrat OpenAPI.
 *
 * `openapi-typescript` ne produit que des types, effacés à la compilation : impossible d'en tirer
 * les listes dont l'application a besoin à l'exécution (options des listes déroulantes, `z.enum`).
 * Ce script complète donc `schema.d.ts` en émettant les mêmes enums sous forme de tableaux
 * `as const`, depuis la même source de vérité — de sorte qu'aucune valeur du back ne soit recopiée
 * à la main nulle part.
 */
const here = dirname(fileURLToPath(import.meta.url));
const SPEC = resolve(here, "../../api/openapi.json");
const OUT = resolve(here, "../shared/api/enums.ts");

const spec = JSON.parse(readFileSync(SPEC, "utf8"));
const schemas = spec.components?.schemas ?? {};

const enums = Object.entries(schemas)
  .filter(([, s]) => s.type === "string" && Array.isArray(s.enum))
  .sort(([a], [b]) => a.localeCompare(b));

if (enums.length === 0) {
  throw new Error(`Aucune enum trouvée dans ${SPEC} — le contrat est-il à jour ?`);
}

const body = enums
  .map(([name, s]) => `  ${name}: [${s.enum.map((v) => JSON.stringify(v)).join(", ")}],`)
  .join("\n");

const content = `// Généré par \`npm run generate:api\` depuis api/openapi.json — NE PAS ÉDITER À LA MAIN.
//
// Le pendant runtime de \`schema.d.ts\` : les mêmes enums, sous forme de valeurs utilisables à
// l'exécution (options de formulaire, \`z.enum\`). Les types restent dans \`contract.ts\`.

export const enumValues = {
${body}
} as const;
`;

writeFileSync(OUT, content, "utf8");
console.log(`✨ ${enums.length} enums → shared/api/enums.ts`);
