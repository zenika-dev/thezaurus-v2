// @ts-check
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Génère les artefacts TypeScript du contrat OpenAPI :
 * 1. shared/api/enums.ts : les valeurs runtime des enums pour les formulaires et schémas Zod.
 * 2. shared/api/contract.ts : les alias lisibles BackendXxx sur les schémas de schema.d.ts.
 */
const here = dirname(fileURLToPath(import.meta.url));
const SPEC = resolve(here, "../../api/openapi.json");
const OUT_ENUMS = resolve(here, "../shared/api/enums.ts");
const OUT_CONTRACT = resolve(here, "../shared/api/contract.ts");

const spec = JSON.parse(readFileSync(SPEC, "utf8"));
const schemas = spec.components?.schemas ?? {};

// 1. Enums runtime
const enums = Object.entries(schemas)
  .filter(([, s]) => Array.isArray(s.enum))
  .sort(([a], [b]) => a.localeCompare(b));

if (enums.length === 0) {
  throw new Error(`Aucune enum trouvée dans ${SPEC} — le contrat est-il à jour ?`);
}

const enumDeclarations = enums
  .map(
    ([name, s]) =>
      `export const ${name} = [${s.enum.map((v) => JSON.stringify(v)).join(", ")}] as const;\nexport type ${name} = (typeof ${name})[number];`
  )
  .join("\n\n");

const enumContent = `// Généré par \`npm run generate:api\` depuis api/openapi.json — NE PAS ÉDITER À LA MAIN.
//
// Le pendant runtime de \`schema.d.ts\` : les mêmes enums, sous forme de valeurs utilisables à
// l'exécution (options de formulaire, \`z.enum\`). Les types sont dans \`contract.ts\`.

${enumDeclarations}
`;

writeFileSync(OUT_ENUMS, enumContent, "utf8");
console.log(`✨ ${enums.length} enums → shared/api/enums.ts`);

// 2. Types aliases (contract.ts)
const schemaNames = Object.keys(schemas).sort((a, b) => a.localeCompare(b));

if (schemaNames.length === 0) {
  throw new Error(`Aucun schéma trouvé dans ${SPEC} — le contrat est-il à jour ?`);
}

const typeAliases = schemaNames
  .map((name) => `export type Backend${name} = Schemas["${name}"];`)
  .join("\n");

const contractContent = `// Généré par \`npm run generate:api\` depuis api/openapi.json — NE PAS ÉDITER À LA MAIN.
//
// Alias lisibles sur les schémas générés (\`components["schemas"]["X"]\` est exact mais verbeux).
// Régénération : \`npm run generate:api\`.

import type { components } from "./schema";

type Schemas = components["schemas"];

${typeAliases}
`;

writeFileSync(OUT_CONTRACT, contractContent, "utf8");
console.log(`✨ ${schemaNames.length} types → shared/api/contract.ts`);
