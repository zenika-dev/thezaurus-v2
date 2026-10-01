import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";

export default defineConfig({
  resolve: { alias: {
    "@": fileURLToPath(new URL(".", import.meta.url)),
    "mui-tiptap": fileURLToPath(new URL("./node_modules/mui-tiptap/dist/esm/index.js", import.meta.url)),
  } },
  oxc: { jsx: { runtime: "automatic" } },
  test: {
    environment: "jsdom", restoreMocks: true,
    // Use the browser module so Tiptap and its controls share one ProseMirror instance.
    server: { deps: { inline: ["mui-tiptap"] } },
  },
});
