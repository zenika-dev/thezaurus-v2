import { Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { render, screen, within, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BlogPostsList } from "@/features/blog-posts/ui/BlogPostsList";
import { TalkTable } from "@/features/talks/ui/TalkTable";
import { Role } from "@/shared/api";
import { resetMockSearchParams } from "./setup";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

const samplePosts = [
  { id: "mine", title: "Mon brouillon", writers: [{ name: "Bob", email: "bob@zenika.com" }, { name: "Alice", email: " ALICE@ZENIKA.COM " }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
  { id: "published", title: "Ma publication", writers: [{ name: "Alice", email: "alice@zenika.com" }], status: "PUBLISHED", tags: [], creationDate: "01-01-2026" },
  { id: "namesake", title: "Homonyme", writers: [{ name: "Alice", email: undefined }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
];

const sampleTalks = [
  { id: "mine", title: "Mon talk", speakers: [{ name: "Bob", email: "bob@zenika.com" }, { name: "Alice", email: " ALICE@ZENIKA.COM " }], status: "DRAFT", visibility: "PUBLIC" },
  { id: "namesake", title: "Autre talk", speakers: [{ name: "Alice", email: "" }], status: "DRAFT", visibility: "PUBLIC" },
];

function setupPostsFetch(email: string | null = "alice@zenika.com", failDelete = false) {
  vi.stubGlobal("fetch", vi.fn(async (url: string, init?: RequestInit) => {
    if (init?.method === "DELETE") {
      if (failDelete) {
        return new Response(null, { status: 403 });
      }
      return Response.json({});
    }
    const parsed = new URL(url, "http://localhost");
    const statusParam = parsed.searchParams.get("status");
    const personalParam = parsed.searchParams.get("personal");

    let filtered = samplePosts;
    if (personalParam === "true") {
      if (!email) {
        filtered = [];
      } else {
        const norm = email.trim().toLowerCase();
        filtered = filtered.filter(p => p.writers.some(w => w.email && w.email.trim().toLowerCase() === norm));
      }
    }
    if (statusParam && statusParam !== "All") {
      filtered = filtered.filter(p => p.status === statusParam);
    }

    return Response.json({
      items: filtered,
      total: filtered.length,
      page: 0,
      pageSize: 10,
      totalPages: Math.ceil(filtered.length / 10),
    });
  }));
}

function setupTalksFetch(email: string | null = "alice@zenika.com") {
  vi.stubGlobal("fetch", vi.fn(async (url: string) => {
    const parsed = new URL(url, "http://localhost");
    const statusParam = parsed.searchParams.get("status");
    const personalParam = parsed.searchParams.get("personal");

    let filtered = sampleTalks;
    if (personalParam === "true") {
      if (!email) {
        filtered = [];
      } else {
        const norm = email.trim().toLowerCase();
        filtered = filtered.filter(t => t.speakers.some(s => s.email && s.email.trim().toLowerCase() === norm));
      }
    }
    if (statusParam && statusParam !== "All") {
      filtered = filtered.filter(t => t.status === statusParam);
    }

    return Response.json({
      items: filtered,
      total: filtered.length,
      page: 0,
      pageSize: 10,
      totalPages: Math.ceil(filtered.length / 10),
    });
  }));
}

function renderPosts(role: Role = "CONSULTANT", email: string | null = "alice@zenika.com", failDelete = false) {
  resetMockSearchParams();
  setupPostsFetch(email, failDelete);
  const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false } } });
  const view = render(
    <SessionProvider session={{ user: { name: "Alice", email, roles: [role] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={client}>
        <Suspense fallback={<div>Loading...</div>}>
          <BlogPostsList />
        </Suspense>
      </QueryClientProvider>
    </SessionProvider>,
  );
  return { ...view, client };
}

describe("Mes articles", () => {
  it("conserve le formulaire et affiche l’échec d’une suppression optimiste", async () => {
    const user = userEvent.setup();
    renderPosts("CONSULTANT", "alice@zenika.com", true);

    vi.spyOn(window, "confirm").mockReturnValue(true);
    await user.click(await screen.findByText("Mon brouillon"));
    const title = await screen.findByRole("textbox", { name: /Titre/ });
    await user.clear(title);
    await user.type(title, "Travail conservé");
    await user.click(screen.getByRole("button", { name: "Supprimer" }));
    expect(await screen.findByText("La suppression a échoué. Vous pouvez réessayer.")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: /Titre/ })).toHaveValue("Travail conservé");
  });

  it("explique les cinq étapes uniquement dans le popup d’information", async () => {
    const user = userEvent.setup();
    renderPosts();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    await user.click(await screen.findByRole("button", { name: "Comprendre les étapes des articles" }));
    const popup = await screen.findByRole("dialog", { name: "Les étapes d’un article" });
    expect(within(popup).getAllByRole("listitem")).toHaveLength(5);
    expect(within(popup).getByText("Prêt à publier")).toBeInTheDocument();
    expect(within(popup).getByText(/travail éditorial est terminé/)).toBeInTheDocument();
    await user.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(await screen.findByRole("button", { name: "Comprendre les étapes des articles" })).toHaveFocus();
  });

  it.each(Role)("inclut les coauteurs par email et cumule le statut pour %s", async (role) => {
    const user = userEvent.setup();
    renderPosts(role);
    expect(await screen.findByText("Homonyme")).toBeInTheDocument();

    const personalBtn = await screen.findByRole("button", { name: "Mes articles" });
    await user.click(personalBtn);
    await waitFor(() => expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "true"));
    expect(await screen.findByText("Mon brouillon")).toBeInTheDocument();
    expect(screen.queryByText("Homonyme")).not.toBeInTheDocument();

    const draftBtn = await screen.findByRole("button", { name: "En rédaction" });
    await user.click(draftBtn);
    await waitFor(() => expect(screen.queryByText("Ma publication")).not.toBeInTheDocument());

    const resetBtn = await screen.findByRole("button", { name: "Réinitialiser" });
    await user.click(resetBtn);
    expect(await screen.findByText("Homonyme")).toBeInTheDocument();
    expect(await screen.findByText("Ma publication")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false");
  });

  it("ne rapproche pas les auteurs sans email d'une session sans email", async () => {
    renderPosts("CONSULTANT", null);
    const personalBtn = await screen.findByRole("button", { name: "Mes articles" });
    await userEvent.setup().click(personalBtn);
    await waitFor(() => {
      expect(screen.queryByText("Homonyme")).not.toBeInTheDocument();
      expect(screen.getByText("Aucun article ne correspond aux filtres sélectionnés.")).toBeInTheDocument();
    });
  });

  it("revient désactivé après fermeture et réouverture de la page", async () => {
    const user = userEvent.setup();
    const view = renderPosts();
    expect(await screen.findByText("Homonyme")).toBeInTheDocument();

    const personalBtn = await screen.findByRole("button", { name: "Mes articles" });
    await user.click(personalBtn);
    await waitFor(() => expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "true"));

    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false"));
    expect(await screen.findByText("Homonyme")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "true"));

    view.unmount();
    renderPosts();
    expect(await screen.findByText("Homonyme")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false");
  });
});

describe("Mes talks", () => {
  it.each(Role)("filtre les speakers pour %s et ne confond pas nom et email", async (role) => {
    resetMockSearchParams();
    setupTalksFetch("alice@zenika.com");
    const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false } } });
    render(
      <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: [role] }, expires: "2099-01-01" }}>
        <QueryClientProvider client={client}>
          <Suspense fallback={<div>Loading...</div>}>
            <TalkTable />
          </Suspense>
        </QueryClientProvider>
      </SessionProvider>,
    );
    const user = userEvent.setup();
    expect(await screen.findByText("Autre talk")).toBeInTheDocument();

    const personalBtn = await screen.findByRole("button", { name: "Mes talks" });
    await user.click(personalBtn);
    await waitFor(() => expect(screen.getByRole("button", { name: "Mes talks" })).toHaveAttribute("aria-pressed", "true"));
    expect(await screen.findByText("Mon talk")).toBeInTheDocument();
    expect(screen.queryByText("Autre talk")).not.toBeInTheDocument();

    const acceptedBtn = await screen.findByRole("button", { name: "Accepted" });
    await user.click(acceptedBtn);
    await waitFor(() => expect(screen.queryByText("Mon talk")).not.toBeInTheDocument());

    const resetBtn = await screen.findByRole("button", { name: "Réinitialiser" });
    await user.click(resetBtn);
    expect(await screen.findByText("Autre talk")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mes talks" })).toHaveAttribute("aria-pressed", "false");
  });
});
