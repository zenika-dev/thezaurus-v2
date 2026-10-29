import { Suspense } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BlogPostsList } from "@/features/blog-posts/ui/BlogPostsList";
import { TalkTable } from "@/features/talks/ui/TalkTable";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Talks TablePagination", () => {
  it("affiche la pagination et réagit au changement de page et de taille de page", async () => {
    let requestedUrl = "";
    vi.stubGlobal("fetch", vi.fn(async (url: string) => {
      requestedUrl = url;
      return Response.json({
        items: [
          { id: "talk-1", title: "Talk 1", speakers: [{ name: "Alice", email: "alice@zenika.com" }], status: "DRAFT", visibility: "PUBLIC", format: "quickie", language: "francais", notes: "" },
          { id: "talk-2", title: "Talk 2", speakers: [{ name: "Bob", email: "bob@zenika.com" }], status: "ACCEPTED", visibility: "PUBLIC", format: "quickie", language: "francais", notes: "" },
        ],
        total: 55,
        page: 0,
        pageSize: 10,
        totalPages: 6,
      });
    }));

    const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    render(
      <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["ADMIN"] }, expires: "2099-01-01" }}>
        <QueryClientProvider client={client}>
          <Suspense fallback={<div>Loading...</div>}>
            <TalkTable />
          </Suspense>
        </QueryClientProvider>
      </SessionProvider>,
    );

    expect(await screen.findByText("Talk 1")).toBeInTheDocument();
    expect(screen.getByText(/1[–-]10 sur 55|1-10 of 55/i)).toBeInTheDocument();

    const user = userEvent.setup();
    const nextPageBtn = screen.getByRole("button", { name: /page suivante|next page/i });
    await user.click(nextPageBtn);

    await waitFor(() => {
      expect(requestedUrl).toContain("page=1");
    });
  });
});

describe("BlogPosts Pagination", () => {
  it("affiche la pagination par pastilles et permet de changer de page", async () => {
    let requestedUrl = "";
    vi.stubGlobal("fetch", vi.fn(async (url: string) => {
      requestedUrl = url;
      return Response.json({
        items: [
          { id: "post-1", title: "Post 1", writers: [{ name: "Alice", email: "alice@zenika.com" }], status: "DRAFT", tags: [], creationDate: "01-01-2026" },
        ],
        total: 55,
        page: 0,
        pageSize: 10,
        totalPages: 6,
      });
    }));

    const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    render(
      <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["ADMIN"] }, expires: "2099-01-01" }}>
        <QueryClientProvider client={client}>
          <Suspense fallback={<div>Loading...</div>}>
            <BlogPostsList />
          </Suspense>
        </QueryClientProvider>
      </SessionProvider>,
    );

    expect(await screen.findByText("Post 1")).toBeInTheDocument();
    expect(screen.getByText(/1[–-]10 sur 55/)).toBeInTheDocument();

    const user = userEvent.setup();
    const page2Btn = screen.getByRole("button", { name: /page 2|aller à la page 2|go to page 2/i });
    await user.click(page2Btn);

    await waitFor(() => {
      expect(requestedUrl).toContain("page=1");
    });
  });

  it("réinitialise simultanément le statut et le filtre personnel lors du clic sur Réinitialiser", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => {
      return Response.json({
        items: [],
        total: 0,
        page: 0,
        pageSize: 10,
        totalPages: 0,
      });
    }));

    const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    render(
      <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["ADMIN"] }, expires: "2099-01-01" }}>
        <QueryClientProvider client={client}>
          <Suspense fallback={<div>Loading...</div>}>
            <BlogPostsList />
          </Suspense>
        </QueryClientProvider>
      </SessionProvider>,
    );

    const user = userEvent.setup();
    const personalBtn = await screen.findByRole("button", { name: "Mes articles" });
    await user.click(personalBtn);

    const draftBtn = await screen.findByRole("button", { name: "Idée" });
    await user.click(draftBtn);

    const resetBtn = await screen.findByRole("button", { name: "Réinitialiser" });
    await user.click(resetBtn);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Mes articles" })).toHaveAttribute("aria-pressed", "false");
      expect(screen.getByRole("button", { name: "Tous" })).toHaveAttribute("aria-pressed", "true");
      expect(screen.queryByRole("button", { name: "Réinitialiser" })).not.toBeInTheDocument();
    });
  });
});
