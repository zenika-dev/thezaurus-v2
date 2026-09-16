import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
  {
    files: ["app/**/page.tsx", "app/**/layout.tsx"],
    rules: {
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "ImportSpecifier[imported.name=/^use[A-Z]/], ImportDefaultSpecifier[local.name=/^use[A-Z]/]",
          message:
            "Il est interdit d'importer un hook React ('use...') dans un Server Component.",
        },
      ],
    },
  },
]);

export default eslintConfig;
