import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { render, screen, within, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BlogPostsList } from "@/features/blog-posts/ui/BlogPostsList";
import { TalkTable } from "@/features/talks/ui/TalkTable";
import { queryKeys, Role } from "@/shared/api";

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

function renderPosts(role: Role = "CONSULTANT", email: string | null = "alice@zenika.com") {
  const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false } } });
  client.setQueryData(queryKeys.posts.lists(), [
    { id: "mine", title: "Mon brouillon", writers: [{ name: "Bob", email: "bob@zenika.com" }, { name: "Alice", email: " ALICE@ZENIKA.COM " }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
    { id: "published", title: "Ma publication", writers: [{ name: "Alice", email: "alice@zenika.com" }], status: "PUBLISHED", tags: [], creationDate: "01-01-2026" },
    { id: "namesake", title: "Homonyme", writers: [{ name: "Alice" }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
  ]);
  const view = render(
    <SessionProvider session={{ user: { name: "Alice", email, roles: [role] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={client}><BlogPostsList /></QueryClientProvider>
    </SessionProvider>,
  );
  return { ...view, client };
}

describe("Mes articles", () => {
  it("conserve le formulaire et affiche l’échec d’une suppression optimiste", async () => {
    const user = userEvent.setup();
    const { client } = renderPosts();
    const posts = client.getQueryData(queryKeys.posts.lists());
    let rejectDeletion: () => void = () => {};
    const deletion = new Promise<Response>(resolve => { rejectDeletion = () => resolve(new Response(null, { status: 403 })); });
    vi.stubGlobal("fetch", vi.fn(async (_url: string, init?: RequestInit) => init?.method === "DELETE" ? deletion : Response.json(posts)));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    await user.click(screen.getByText("Mon brouillon"));
    const title = await screen.findByRole("textbox", { name: /Titre/ });
    await user.clear(title);
    await user.type(title, "Travail conservé");
    await user.click(screen.getByRole("button", { name: "Supprimer" }));
    await waitFor(() => expect(fetch).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({ method: "DELETE" })));
    rejectDeletion();
    expect(await screen.findByText("La suppression a échoué. Vous pouvez réessayer.")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: /Titre/ })).toHaveValue("Travail conservé");
  });
  it("explique les cinq étapes uniquement dans le popup d’information", async () => {
    const user = userEvent.setup();
    renderPosts();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Comprendre les étapes des articles" }));
    const popup = screen.getByRole("dialog", { name: "Les étapes d’un article" });
    expect(within(popup).getAllByRole("listitem")).toHaveLength(5);
    expect(within(popup).getByText("Prêt à publier")).toBeInTheDocument();
    expect(within(popup).getByText(/travail éditorial est terminé/)).toBeInTheDocument();
    await user.keyboard("{Escape}");
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Comprendre les étapes des articles" })).toHaveFocus();
  });
  it.each(Role)("inclut les coauteurs par email et cumule le statut pour %s", async (role) => {
    const user = userEvent.setup();
    renderPosts(role);
    expect(screen.getByText("Homonyme")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText("Mon brouillon")).toBeInTheDocument();
    expect(screen.queryByText("Homonyme")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "En rédaction" }));
    expect(screen.queryByText("Ma publication")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Réinitialiser" }));
    expect(screen.getByText("Homonyme")).toBeInTheDocument();
    expect(screen.getByText("Ma publication")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false");
  });

  it("ne rapproche pas les auteurs sans email d'une session sans email", async () => {
    renderPosts("CONSULTANT", null);
    await userEvent.setup().click(screen.getByRole("button", { name: "Mes articles" }));
    expect(screen.queryByText("Homonyme")).not.toBeInTheDocument();
    expect(screen.getByText("Aucun article ne correspond aux filtres sélectionnés.")).toBeInTheDocument();
  });

  it("revient désactivé après fermeture et réouverture de la page", async () => {
    const user = userEvent.setup();
    const view = renderPosts();
    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    expect(screen.getByText("Homonyme")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    view.unmount();
    renderPosts();
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByText("Homonyme")).toBeInTheDocument();
  });
});

describe("Mes talks", () => {
  it.each(Role)("filtre les speakers pour %s et ne confond pas nom et email", async (role) => {
    const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity } } });
    client.setQueryData(queryKeys.talks.lists(), [
      { id: "mine", title: "Mon talk", speakers: [{ name: "Bob", email: "bob@zenika.com" }, { name: "Alice", email: " ALICE@ZENIKA.COM " }], status: "DRAFT", visibility: "PUBLIC" },
      { id: "namesake", title: "Autre talk", speakers: [{ name: "Alice", email: "" }], status: "DRAFT", visibility: "PUBLIC" },
    ]);
    render(
      <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: [role] }, expires: "2099-01-01" }}>
        <QueryClientProvider client={client}><TalkTable /></QueryClientProvider>
      </SessionProvider>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Mes talks" }));
    expect(screen.getByRole("button", { name: "Mes talks" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText("Mon talk")).toBeInTheDocument();
    expect(screen.queryByText("Autre talk")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Accepted" }));
    expect(screen.queryByText("Mon talk")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Réinitialiser" }));
    expect(screen.getByText("Autre talk")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mes talks" })).toHaveAttribute("aria-pressed", "false");
  });
});
