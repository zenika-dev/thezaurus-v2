import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { BlogPostsList } from "@/features/blog-posts/ui/BlogPostsList";
import { TalkTable } from "@/features/talks/ui/TalkTable";
import { queryKeys, Role } from "@/shared/api";

function renderPosts(role: Role = "CONSULTANT", email: string | null = "alice@zenika.com") {
  const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false } } });
  client.setQueryData(queryKeys.posts.lists(), [
    { id: "mine", title: "Mon brouillon", writers: [{ name: "Bob", email: "bob@zenika.com" }, { name: "Alice", email: " ALICE@ZENIKA.COM " }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
    { id: "published", title: "Ma publication", writers: [{ name: "Alice", email: "alice@zenika.com" }], status: "PUBLISHED", tags: [], creationDate: "01-01-2026" },
    { id: "namesake", title: "Homonyme", writers: [{ name: "Alice" }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
  ]);
  return render(
    <SessionProvider session={{ user: { name: "Alice", email, roles: [role] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={client}><BlogPostsList /></QueryClientProvider>
    </SessionProvider>,
  );
}

describe("Mes articles", () => {
  it.each(Role)("inclut les coauteurs par email et cumule le statut pour %s", async (role) => {
    const user = userEvent.setup();
    renderPosts(role);
    expect(screen.getByText("Homonyme")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Mes articles" }));
    expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText("Mon brouillon")).toBeInTheDocument();
    expect(screen.queryByText("Homonyme")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Draft" }));
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
