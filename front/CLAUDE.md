# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Full agent instructions, coding standards, pitfalls, and step-by-step checklists are in [AGENTS.md](./AGENTS.md). Read it before writing any code.

---

## Commands

All commands run from `front/`.

| Task | Command |
|---|---|
| Dev server | `npm run dev` |
| Production build | `npm run build` |
| Lint | `npm run lint` |
| Bundle analysis | `ANALYZE=true npm run build` |

**Full local stack** (from repo root `thezaurus-v2/`):
```bash
docker compose up --build --watch
```
Starts Firestore emulator → Quarkus API on `:8080` → Next.js frontend on `:3000`.

> Use `--build --watch`, **not** a bare `docker compose up`. The `front` service bakes source into
> its image (no bind-mount, only a `develop.watch` block), so a plain `up` runs stale code. `--watch`
> syncs host edits into the container; `--build` rebuilds the image and is **required after any
> `package.json` change** so new dependencies land in the container's `node_modules`.

There is no test suite. Validate changes manually via `npm run dev` and confirm `npm run lint` + `npm run build` pass.

---

## Architecture

**Thezaurus** is an internal Zenika tool for tracking talks, blog posts, and conferences. Stack: Next.js 16.2.7 (App Router, standalone output), TypeScript strict, MUI v9 + Tailwind v4, TanStack Query v5, React Hook Form v7 + Zod v4. Backend is a Quarkus REST API (`API_URL` env var, default `http://localhost:8080`) backed by Google Firestore.

The frontend follows **Feature-Sliced Design (FSD)**. Layers may only import downward:

```
app/        Route entry points — routing, metadata, SSR prefetch only.
widgets/    Composite layout sections (SideMenu, Providers).
features/   Self-contained feature modules: ui/ (components) + model/ (hooks).
entities/   Domain types, display constants, Zod schemas. One sub-folder per entity.
shared/     Cross-cutting utilities with no domain knowledge.
  api/        Fetch functions, query-key factory, backend↔frontend mapping.
  actions/    Next.js Server Actions ("use server" + revalidatePath).
  lib/        QueryClient factory, MUI theme, generic utils.
  ui/         Reusable primitive components.
```

### Critical patterns

**Every page** uses SSR prefetch → `HydrationBoundary` so the client gets pre-populated data:
```tsx
const queryClient = getQueryClient();
await queryClient.prefetchQuery({ queryKey: queryKeys.posts.lists(), queryFn: postApi.getPosts });
return <HydrationBoundary state={dehydrate(queryClient)}><BlogPosts /></HydrationBoundary>;
```

**Every data section** is wrapped independently:
```tsx
<Suspense fallback={<Skeleton />}><DataErrorBoundary><Component /></DataErrorBoundary></Suspense>
```

**Every mutation** implements the full optimistic pattern (`onMutate` → optimistic set, `onError` → rollback, `onSettled` → invalidate).

**All dialogs** are lazy-loaded: `next/dynamic(() => import("./Dialog"), { ssr: false })`.

### Key conventions

- `@/` path alias maps to `front/` — never use relative paths.
- Every directory exposes a single `index.ts` barrel; import from the directory, not sub-files.
- MUI: `import Button from "@mui/material/Button"` not `import { Button } from "@mui/material"`.
- All user-facing strings are in **French**.
- Types always inferred from Zod schemas (`z.infer<typeof schema>`), never duplicated.
- Date formats: UI `DD-MM-YYYY` ↔ backend ISO `YYYY-MM-DDT00:00:00`. Use conversion utils in `shared/api/`.
- Status enums: frontend PascalCase ↔ backend `SCREAMING_SNAKE_CASE`. Use `toFrontendStatus`/`toBackendStatus` in `shared/api/`.
