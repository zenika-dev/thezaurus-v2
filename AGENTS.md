# AGENTS.md — Thezaurus

## Project Overview

**Thezaurus** is an internal Zenika tool for tracking talks, blog posts, events, and conferences.

The project root (`thezaurus-v2/`) contains two main application directories:
- `api/`: Backend REST API built with Quarkus (Java 21) and Google Firestore.
- `front/`: Frontend application built with Next.js 16 (React 19, TypeScript).

| Component | Framework / Tech | Port | Key Libraries |
|---|---|---|---|
| **Frontend** (`front/`) | Next.js 16 (App Router, `standalone`) | `3000` | TypeScript 5 (strict), MUI v9, Tailwind CSS v4, TanStack Query v5, React Hook Form v7 + Zod v4, dayjs, lucide-react |
| **Backend** (`api/`) | Quarkus 3.39.2 (Java 21) | `8080` | SmallRye OpenAPI, SmallRye JWT, Google Cloud Firestore SDK, Bolt for Java (Slack) |
| **Database** | Google Firestore | `9000` (emulator) | Firebase Emulator Suite (dev) / GCP Firestore (prod) |

---

## Development Commands

### Local Stack (Docker Compose)
Run from repository root `thezaurus-v2/`:
```bash
docker compose up --build --watch
```
- Starts Firestore emulator (port `9000`), Quarkus API (port `8080`), and Next.js frontend (port `3000`).
- Web Emulator UI available at `http://localhost:4000/firestore/local-dev/data`.
- API Swagger UI available at `http://localhost:8080/q/swagger-ui/`.

> **Note**: Always use `--build --watch`. The frontend bakes sources into the image and synchronizes via Docker Compose watch. Rebuilding (`--build`) is required whenever `package.json` or backend dependencies change.

### Frontend Commands (`front/`)
All commands run from `front/`:

| Task | Command |
|---|---|
| Dev server | `npm run dev` |
| Production build | `npm run build` |
| Start production server | `npm run start` |
| Lint | `npm run lint` |
| Regenerate API types & enums | `npm run generate:api` |
| Bundle analysis | `ANALYZE=true npm run build` |

### Backend Commands (`api/`)
All commands run from `api/`:

| Task | Command |
|---|---|
| Dev server (live reload) | `./mvnw quarkus:dev` |
| Run tests | `./mvnw test` |
| Verify (tests + spotless check) | `./mvnw verify` |
| Format code with Spotless | `./mvnw spotless:apply` |
| Check code formatting | `./mvnw spotless:check` |

---

## OpenAPI Contract & Shared Types

The communication contract between `api` and `front` is strictly derived from the backend OpenAPI definition. **Backend payload types and enum values are generated, never hand-written.**

```
JAX-RS Resources ──(mvn package)──> api/openapi.json (Git-tracked) ──(npm run generate:api)──┬─> front/shared/api/schema.d.ts (gitignored)
                                                                                             ├─> front/shared/api/enums.ts (gitignored)
                                                                                             └─> front/shared/api/contract.ts (gitignored)
```

### Lifecycle & Rules
1. **Quarkus Generates Spec**: SmallRye OpenAPI exports the OpenAPI spec during build (`target/openapi/openapi.json`).
2. **Versioned Contract**: `api/openapi.json` is the **only contract file committed in git**.
3. **Frontend Generation (Git-ignored)**: TypeScript contract files (`front/shared/api/schema.d.ts`, `enums.ts`, `contract.ts`) are **ignored by git** and generated automatically at `npm install` (`postinstall`), `npm run dev` (`predev`), and `npm run build` (`prebuild`).
4. **IDE & Generated File Markers**: Generated TS files are tagged with `@generated` / `@readonly`, locked with file-system read-only permissions (`0o444`), and declared in `.gitattributes` (`linguist-generated=true`) so that IDEs (IntelliJ, VSCode) display read-only locks and warnings against manual edits.
5. **Human-Readable Aliases**: `front/shared/api/contract.ts` is fully generated and defines clear aliases (e.g., `BackendTalk`, `BackendBlogPost`, `BackendConference`, `BackendRole`).
6. **Runtime Enums**: Option lists and validation schemas use direct enum exports from `@/shared/api` (e.g. `TalkStatus`, `Role`, `BlogPostStatus`).
7. **Never declare hand-written `interface BackendXxx` or manual enum arrays** in the frontend. If a backend model changes:
   ```bash
   cd api && ./mvnw package -DskipTests && cp target/openapi/openapi.json openapi.json
   cd ../front && npm run generate:api
   ```
   Commit `api/openapi.json` (the TS files are generated locally and in CI, and stay gitignored).

---

## Frontend Architecture (`front/`)

The frontend follows **Feature-Sliced Design (FSD)**. Layers may only import from layers **below** them:

```
app/          Route entry points (App Router pages: routing, metadata, SSR prefetch).
widgets/      Composite layout components (SideMenu, Providers).
features/     Self-contained feature modules: ui/ (components) + model/ (hooks).
entities/     Domain types, display constants, Zod validation schemas. One sub-folder per entity.
shared/       Cross-cutting utilities with no domain knowledge.
  api/          Fetch functions, query key factory, contract types, runtime enums.
  actions/      Next.js Server Actions ("use server" + revalidatePath).
  lib/          QueryClient factory, MUI theme, generic utils.
  ui/           Reusable primitive components (Badge, DataErrorBoundary, Button, ...).
```

### Key Frontend Patterns

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

### Frontend Coding Standards

- **TypeScript**: Strict mode is on. No `any`, no `@ts-ignore`.
- **Path Aliases**: Use `@/` for all internal imports (maps to `front/`). Never use relative upward paths like `../../shared/api`.
- **Barrel Exports**: Every directory exposes an `index.ts`. Import from the directory, not sub-files.
- **MUI Imports**: Import MUI components individually (`import Button from "@mui/material/Button"`) to preserve tree-shaking.
- **Status Enums**: Frontend uses backend enum keys verbatim (`TalkStatus`, `BlogPostStatus`), mapped to UI labels and badges via dedicated config records (`talkStatusConfig`, `blogPostStatusConfig`).
- **Date Handling**: UI display `DD-MM-YYYY` ↔ backend ISO `YYYY-MM-DDT00:00:00` or `YYYY-MM-DD`. Conversions live in entity `api.ts` or `date-utils.ts`.
- **UI Language**: French — all user-facing strings, labels, and error messages are in French.

---

## Backend Architecture (`api/`)

- **Resource Layer**: JAX-RS endpoints returning typed `RestResponse<T>` so SmallRye OpenAPI correctly documents responses and schemas.
- **Repository Layer**: Firestore collections accessed through `Firestore` client.
- **Model Layer**: Java records or POJOs with Jackson serialization and Jakarta Bean Validation annotations (`@NotBlank`, `@NotNull`, `@NotEmpty`) to enforce data constraints and auto-generate OpenAPI required fields.
- **Formatting**: Imposed by Palantir Java format via Spotless (`./mvnw spotless:apply`).

---

## Adding New Code

### Adding a New Entity (`front/entities/<name>`)
1. Create `model.ts`: Domain types and display constants using runtime enums and `shared/api/contract.ts`.
2. Create `schema.ts`: Zod form schema using runtime enums + inferred type `z.infer<typeof schema>`.
3. Create `api.ts`: API functions calling backend endpoints.
4. Create `index.ts`: Public barrel export.

### Adding a New Feature (`front/features/<name>`)
1. `ui/<FeatureName>.tsx`: Main client component.
2. `model/use<FeatureName>.ts`: `useSuspenseQuery` hook.
3. `model/use<FeatureName>Mutations.ts`: Optimistic mutation hooks.
4. `index.ts`: Feature public exports.

---

## PR Requirements & Definition of Done

A change is complete when all of the following are true:

- [ ] `./mvnw verify` passes in `api/` (all tests pass and Spotless check succeeds).
- [ ] `npm run lint` passes in `front/` with zero errors.
- [ ] `npm run build` passes in `front/` (TypeScript compiles and Next.js build succeeds).
- [ ] If backend models or resources changed, `api/openapi.json` is updated and committed.
- [ ] FSD layer boundaries respected (no upward imports).
- [ ] All user-visible text is in French.
- [ ] Every new data section is wrapped in `<Suspense>` + `<DataErrorBoundary>`.
- [ ] Mutations implement optimistic update + rollback + invalidation.
- [ ] New dialogs are lazy-loaded with `next/dynamic(..., { ssr: false })`.
- [ ] New interactive elements without visible labels have `aria-label` / `aria-labelledby` attributes.
- [ ] New entities have `model.ts`, `schema.ts`, and `index.ts` following the established pattern.
