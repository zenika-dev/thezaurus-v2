<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your
training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code.
Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

---

# AGENTS.md — Thezaurus Frontend

## Project Overview

**Thezaurus** is an internal Zenika tool for tracking talks, blog posts, and conferences.

| Property | Value |
|---|---|
| Framework | Next.js 16.2.7 (App Router, `output: "standalone"`) |
| Language | TypeScript 5 — strict mode, target ES2017 |
| UI libraries | MUI v9 + Tailwind CSS v4 + Emotion |
| Data fetching | TanStack React Query v5 |
| Forms | React Hook Form v7 + Zod v4 |
| Date handling | dayjs (French locale, `customParseFormat` plugin) |
| Icons | lucide-react |
| Package manager | npm (`package-lock.json`; use `npm ci` in CI/containers) |
| Backend | Quarkus REST API — configured via `API_URL` env var (default `http://localhost:8080`) |
| Database | Google Firestore (Firestore emulator for local dev) |
| UI language | French — all user-facing strings, labels, and error messages are in French |

The project root (`thezaurus-v2/`) contains three independent sibling directories: `front/`, `api/`,
and `ui/`. There is no monorepo tooling. All commands in this document are run from inside `front/`.

---

## Architecture

The frontend follows **Feature-Sliced Design (FSD)**. Layers are listed from highest to lowest. A
layer may only import from layers **below** it.

```
app/          Route entry points (Next.js App Router pages).
              No business logic — only routing, metadata, and SSR prefetching.

widgets/      Composite layout components (SideMenu, Providers).
              Assembles features and shared UI into full page sections.

features/     Self-contained feature modules.
              Each has two sub-folders: ui/ (React components) and model/ (hooks).

entities/     Domain types, display constants, and Zod validation schemas.
              One sub-folder per entity (post, talk, …).

shared/       Cross-cutting utilities with no domain knowledge.
  shared/api/       Fetch functions + query key factory.
  shared/actions/   Next.js Server Actions (call API + revalidatePath).
  shared/lib/       QueryClient factory, MUI theme, generic utils.
  shared/ui/        Reusable primitive components (Badge, DataErrorBoundary, …).
```

### Key Patterns

**Server / Client split**
- App Router pages are Server Components by default.
- Add `"use client"` at the top of any file that uses hooks, state, or browser APIs.
- Add `"use server"` at the top of Server Action files (`shared/actions/`).

**SSR prefetch + hydration** (used in every page):
```tsx
async function BlogPostsLoader() {
  const queryClient = getQueryClient();
  await queryClient.prefetchQuery({ queryKey: queryKeys.posts.lists(), queryFn: postApi.getPosts });
  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <BlogPosts />
    </HydrationBoundary>
  );
}
```

**Suspense + Error Boundary** — wrap every data section independently:
```tsx
<Suspense fallback={<BlogPostsListSkeleton />}>
  <DataErrorBoundary>
    <BlogPostsList />
  </DataErrorBoundary>
</Suspense>
```

**Optimistic mutations** — every mutation follows this three-hook pattern:
```ts
useMutation({
  mutationFn: ...,
  onMutate: async (input) => {
    await queryClient.cancelQueries({ queryKey });
    const previous = queryClient.getQueryData(queryKey);
    queryClient.setQueryData(queryKey, /* optimistic update */);
    return { previous };
  },
  onError: (_err, _input, ctx) => {
    queryClient.setQueryData(queryKey, ctx?.previous); // rollback
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey }); // sync with server
  },
});
```

**Lazy-loaded dialogs** — all Dialog components are loaded client-side only:
```ts
const CreateBlogPostDialog = dynamic(
  () => import("./CreateBlogPostDialog").then((m) => ({ default: m.CreateBlogPostDialog })),
  { ssr: false }
);
```

---

## Development Commands

All commands run from `front/`.

| Task | Command |
|---|---|
| Dev server | `npm run dev` |
| Production build | `npm run build` |
| Start production server | `npm run start` |
| Lint | `npm run lint` |
| Bundle analysis | `ANALYZE=true npm run build` |

**Full local stack** (from repo root `thezaurus-v2/`):
```bash
docker compose up
```
This starts: Firestore emulator → Quarkus API on port 8080 → Next.js frontend on port 3000.

**Environment variables:**

| Variable | Default | Purpose |
|---|---|---|
| `API_URL` | `http://localhost:8080` | Backend base URL |
| `ANALYZE` | `false` | Enable `@next/bundle-analyzer` |
| `NODE_ENV` | — | `development` shows React Query Devtools |

No `.env.example` file detected. Set `API_URL` via shell or Docker build arg.

---

## Coding Standards

### TypeScript
- Strict mode is **on** (`"strict": true` in `tsconfig.json`). No `any`, no `@ts-ignore`.
- Always infer types from Zod schemas using `z.infer<typeof schema>` — do not duplicate types.
- Entity domain types live in `entities/*/model.ts`; form schemas live in `entities/*/schema.ts`.

### Path Aliases
Use `@/` for all internal imports (maps to `front/`):
```ts
import { postApi } from "@/shared/api";       // correct
import { postApi } from "../../../shared/api"; // wrong
```

### Barrel Exports
Every directory exposes a single `index.ts`. Consumers import from the directory:
```ts
import { BlogPosts } from "@/features/blog-posts";              // correct
import { BlogPosts } from "@/features/blog-posts/ui/BlogPosts"; // wrong
```

### MUI Imports
Import MUI components individually to preserve tree-shaking:
```ts
import Button from "@mui/material/Button"; // correct
import { Button } from "@mui/material";    // wrong — barrel import
```

### Naming Conventions
| Item | Convention | Example |
|---|---|---|
| React components | PascalCase | `BlogPostsList` |
| Custom hooks | camelCase, `use` prefix | `usePosts`, `usePostsMutations` |
| Component files | PascalCase | `BlogPosts.tsx` |
| Hook / util files | camelCase | `usePosts.ts`, `utils.ts` |
| Route directories | kebab-case | `blog-posts/`, `app/blog-posts/` |
| Types / interfaces | PascalCase | `BlogPostData`, `BackendPost` |

### Date Handling
- UI display format: `DD-MM-YYYY` (e.g. `"15-06-2025"`).
- Backend format: ISO `YYYY-MM-DDT00:00:00` (e.g. `"2025-06-15T00:00:00"`).
- Conversion utilities live in `shared/api/posts.ts` — reuse them, do not reimplement.
- Always import dayjs with `customParseFormat` plugin and French locale where needed.

### Status Enums
- Backend uses `SCREAMING_SNAKE_CASE` (e.g. `"DRAFT"`, `"PUBLISHED"`).
- Frontend uses PascalCase (e.g. `"Draft"`, `"Published"`).
- Mapping lives in `shared/api/*.ts` (`toFrontendStatus` / `toBackendStatus`). Do not add
  ad-hoc conversion logic elsewhere.

### Forms
- Use React Hook Form with `zodResolver` for all forms.
- Define the Zod schema in `entities/*/schema.ts` and infer the form type from it.
- Validate dates separately (dayjs-based) since MUI DatePicker values are `Dayjs | null`, not
  strings tracked by RHF.

### Comments
Write comments only when the **why** is non-obvious. Do not describe what the code does.

---

## Testing Standards

**Testing tools**: Not detected. No test files, no Jest/Vitest/Playwright/Cypress configuration
found in `front/`.

Until a testing framework is set up:
- Validate new features manually via `npm run dev` before marking work complete.
- Run `npm run lint` and `npm run build` to catch type/lint errors.

---

## Git Workflow

**CI/CD**: Not detected.

Observed branch naming from git history:
```
feat/<description>   feature work
```

Conventional commit prefixes inferred from git history:
```
feat:   new feature
fix:    bug fix
```

---

## PR Requirements

**Formal PR requirements file**: Not detected.

Baseline expectations inferred from the codebase:

- `npm run lint` passes with zero errors.
- `npm run build` succeeds (TypeScript compilation + Next.js build).
- All user-visible strings are in **French**.
- New features follow the FSD layer rules (no upward imports).
- Every new data-fetching component is wrapped in `<Suspense>` + `<DataErrorBoundary>`.
- New mutations implement the full `onMutate` / `onError` / `onSettled` pattern.

---

## Security Rules

- **No secrets in source**. `API_URL` is the only env var consumed by the frontend. Never embed
  API keys, tokens, or credentials in client-side code.
- External links (user-provided URLs) must use `target="_blank" rel="noopener noreferrer"`. See
  `CreateBlogPostDialog.tsx` for the established pattern.
- Validate URLs before rendering them as links — use `isValidUrl` from `@/shared/lib`.
- Server Actions (`shared/actions/`) run on the server — they are called only from React Query
  mutations via `mutationFn`, not exposed as public endpoints.
- The Dockerfile mounts Google Application Default Credentials at runtime (volume mount). Never
  bake credentials into the image.

---

## Performance Guidelines

- **Bundle analysis**: run `ANALYZE=true npm run build` to inspect bundle composition.
- **Lazy-load all dialogs** with `next/dynamic(..., { ssr: false })`. Dialogs are not needed on
  first paint and should not bloat the initial bundle.
- **Server-side prefetch** data on every page using `queryClient.prefetchQuery` so the client
  receives a fully populated hydration boundary — no loading spinner on initial navigation.
- **React Query stale time** is set to `60_000` ms globally (`shared/lib/query-client.ts`). Do not
  lower this without a strong reason; it controls refetch frequency.
- **Fonts**: Nunito is loaded via `next/font/google` with `preload: true` and `display: "swap"`.
  Follow this pattern for any additional fonts.
- **Images**: use `next/image` (no images detected yet; this is the required API in Next.js).

---

## Accessibility Guidelines

Patterns observed in the codebase:

- **Dialog titles**: use `aria-labelledby` on `<Dialog>` pointing to the `<DialogTitle id>`.
- **Icon-only buttons**: include an `aria-label` in French describing the action.
- **Decorative icons** (used alongside visible text): set `aria-hidden="true"`.
- **Form inputs**: use MUI's built-in `id` + `label` pairing. Supply `id` on every `TextField`
  and `Select` so MUI generates the correct `<label for>` association.
- **Language**: the root `<html>` element has `lang="fr"`.

Apply these patterns to all new components. Do not render interactive elements without accessible
labels.

---

## Agent Instructions

### Before Writing Any Code
1. Read the Next.js 16 docs at `node_modules/next/dist/docs/` for any API you are about to use —
   this version has breaking changes.
2. Identify which FSD layer your change belongs to before creating any file.
3. Check `shared/lib/utils.ts`, `shared/api/`, and `shared/actions/` for existing utilities before
   writing new helper code.

### Adding a New Entity
1. Create `entities/<name>/model.ts` — TypeScript types + display constants.
2. Create `entities/<name>/schema.ts` — Zod form schema + inferred type.
3. Create `entities/<name>/index.ts` — re-export everything from model and schema.

### Adding a New Feature
1. Create `features/<name>/ui/<FeatureName>.tsx` — main Client Component.
2. Create `features/<name>/model/use<FeatureName>.ts` — `useSuspenseQuery` hook.
3. Create `features/<name>/model/use<FeatureName>Mutations.ts` — optimistic mutations hook.
4. Create `features/<name>/model/index.ts` and `features/<name>/ui/index.ts` barrel files.
5. Create `features/<name>/index.ts` top-level barrel.
6. Add a Server Action file at `shared/actions/<name>.ts` with `"use server"` + `revalidatePath`.
7. Add API functions to `shared/api/<name>.ts` with `mapBackendToFrontend` / `mapFrontendToBackend`.
8. Register query keys in `shared/api/query-keys.ts`.

### Adding a New Page
1. Create `app/<route>/page.tsx` (Server Component).
2. Export `metadata` for the page title and description.
3. Implement the SSR prefetch + `HydrationBoundary` pattern (see Architecture section).
4. Create `app/<route>/loading.tsx` — skeleton shown during Suspense.
5. Create `app/<route>/error.tsx` — error boundary for route-level errors.
6. Add the route to `widgets/SideMenu/SideMenu.tsx` `navLinks` array.

### Adding a Dialog
- Load it with `next/dynamic(..., { ssr: false })`.
- Provide `aria-labelledby` on `<Dialog>` pointing to the `<DialogTitle id>`.
- Reset all local state (form, date pickers, errors) when the dialog closes.

---

## Common Pitfalls

| Pitfall | Correct Approach |
|---|---|
| `import { X } from "@mui/material"` | `import X from "@mui/material/X"` |
| Importing from a sub-file directly | Import from the layer's `index.ts` barrel |
| Importing from a higher FSD layer | Restructure — only import downward |
| Relative paths (`../../shared/api`) | Path alias `@/shared/api` |
| Hooks/state without `"use client"` | Add `"use client"` as the first line |
| Server Actions without `"use server"` | Add `"use server"` as the first line |
| Inline date format conversion | Reuse `toFrontendDate` / `toLocalDateTime` in `shared/api/posts.ts` |
| Inline status enum conversion | Reuse `toFrontendStatus` / `toBackendStatus` in `shared/api/posts.ts` |
| Mutations without rollback | Implement all three: `onMutate`, `onError`, `onSettled` |
| Dialogs rendered eagerly (SSR) | Wrap with `next/dynamic(..., { ssr: false })` |
| User-facing text in English | All UI strings must be in French |
| External link without `rel="noopener noreferrer"` | Always add both attributes |
| Rendering user-provided URLs without validation | Call `isValidUrl` from `@/shared/lib` first |

---

## Definition of Done

A change is complete when all of the following are true:

- [ ] `npm run lint` — zero errors.
- [ ] `npm run build` — TypeScript compiles and Next.js build succeeds.
- [ ] The feature works end-to-end via `npm run dev` (or `docker compose up`).
- [ ] FSD layer boundaries respected (no upward imports, correct layer placement).
- [ ] All user-visible text is in French.
- [ ] Every new data section is wrapped in `<Suspense>` + `<DataErrorBoundary>`.
- [ ] Mutations implement optimistic update + rollback + invalidation.
- [ ] New dialogs are lazy-loaded with `next/dynamic(..., { ssr: false })`.
- [ ] New external links include `rel="noopener noreferrer"` and pass `isValidUrl` validation.
- [ ] New interactive elements without visible labels have `aria-label` attributes.
- [ ] New entities have `model.ts`, `schema.ts`, and `index.ts` following the established pattern.
- [ ] All barrel `index.ts` files are updated to export new public symbols.
