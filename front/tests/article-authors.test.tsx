import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { CreateBlogPostDialog } from "@/features/blog-posts/ui/CreateBlogPostDialog";
import { BlogPostDetailsDialog } from "@/features/blog-posts/ui/BlogPostDetailsDialog";

afterEach(() => vi.unstubAllGlobals());

it("préremplit l'auteur connecté dans le sélecteur multiple à la création", () => {
  const client = new QueryClient();
  render(
    <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["CONSULTANT"] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={client}>
        <CreateBlogPostDialog open onClose={vi.fn()} onSubmit={vi.fn()} />
      </QueryClientProvider>
    </SessionProvider>,
  );
  const dialog = screen.getByRole("dialog");
  expect(within(dialog).getByRole("combobox", { name: /Auteurs/ })).toBeInTheDocument();
  expect(within(dialog).getByText("Alice")).toBeInTheDocument();
});

it("permet de retirer l'auteur connecté et de saisir un auteur libre, sans rétablir le préremplissage", async () => {
  const user = userEvent.setup();
  vi.stubGlobal("fetch", vi.fn(async () => Response.json([])));
  render(
    <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["CONSULTANT"] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={new QueryClient()}>
        <CreateBlogPostDialog open onClose={vi.fn()} onSubmit={vi.fn()} />
      </QueryClientProvider>
    </SessionProvider>,
  );
  const authors = screen.getByRole("combobox", { name: /Auteurs/ });
  await user.click(authors);
  await user.keyboard("{Backspace}");
  expect(screen.queryByText("Alice")).not.toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  expect(await screen.findByText("Au moins un auteur est requis")).toBeInTheDocument();
  await user.type(authors, "Auteur externe{Enter}");
  expect(screen.getByText("Auteur externe")).toBeInTheDocument();
  expect(screen.queryByText("Alice")).not.toBeInTheDocument();
});

it("rattache un ancien auteur par la recherche sans perdre le coauteur libre", async () => {
  const user = userEvent.setup();
  vi.stubGlobal("fetch", vi.fn(async () => Response.json([{ name: "Alice", email: "alice@zenika.com" }])));
  const onUpdate = vi.fn();
  render(
    <QueryClientProvider client={new QueryClient()}>
      <BlogPostDetailsDialog open onClose={vi.fn()} onDelete={vi.fn()} onUpdate={onUpdate}
        post={{ id: "legacy", title: "Article historique", writers: [{ name: "Bob" }, { name: "Alice" }], creationDate: "01-01-2026", tags: ["java"], status: "DRAFT" }} />
    </QueryClientProvider>,
  );
  const authors = screen.getByRole("combobox", { name: /Auteurs/ });
  await user.click(authors);
  await user.keyboard("{Backspace}");
  await user.type(authors, "Alice");
  await user.click(await screen.findByRole("option", { name: /alice@zenika.com/ }));
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(onUpdate).toHaveBeenCalledWith(expect.objectContaining({
    writers: [{ name: "Bob", email: undefined }, { name: "Alice", email: "alice@zenika.com" }],
  }));
});
