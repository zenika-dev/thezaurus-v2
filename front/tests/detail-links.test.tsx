import { act, render, renderHook, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, it, vi } from "vitest";
import { useDetailUrl } from "@/shared/lib/useDetailUrl";
import { usePaginationUrlParams } from "@/shared/lib/usePaginationUrlParams";
import { CopyDetailLinkButton } from "@/shared/ui/CopyDetailLinkButton";
import { setMockSearchParams } from "./setup";

function wrapper({ children }: { children: React.ReactNode }) {
  return <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>;
}

it.each([['talk', 'talks'], ['post', 'posts'], ['conference', 'conferences']])(
  "builds a native %s destination with the PR parameter and current filters", (parameter, scope) => {
    setMockSearchParams(`page=3&personal=true&${parameter}=old`);
    const fetchDetail = vi.fn();
    const { result } = renderHook(() => useDetailUrl(parameter, scope, [{ id: "old" }], fetchDetail), { wrapper });
    const url = new URL(result.current.href("id & other"), "http://localhost");
    expect(url.searchParams.get(parameter)).toBe("id & other");
    expect(url.searchParams.get("page")).toBe("3");
    expect(url.searchParams.get("personal")).toBe("true");
    expect(url.searchParams.has("detail")).toBe(false);
    expect(result.current.selectedId).toBe("old");
    expect(fetchDetail).not.toHaveBeenCalled();
  },
);

it.each([['talk', 'talks'], ['post', 'posts'], ['conference', 'conferences']])(
  "opens a %s outside the current list and closes without losing filters", async (parameter, scope) => {
    setMockSearchParams(`page=3&personal=true&${parameter}=outside`);
    const fetchDetail = vi.fn().mockResolvedValue({ id: "outside", title: "Detail" });
    const { result } = renderHook(() => useDetailUrl(parameter, scope, [{ id: "listed" }], fetchDetail), { wrapper });
    await waitFor(() => expect(result.current.selected?.id).toBe("outside"));
    expect(fetchDetail).toHaveBeenCalledWith("outside");
    act(() => result.current.close());
    expect(result.current.selectedId).toBeNull();
    act(() => result.current.select("listed"));
    expect(result.current.selected?.id).toBe("listed");
    expect(fetchDetail).toHaveBeenCalledTimes(1);
  },
);

it("keeps selection when changing pagination and follows browser URL changes", () => {
  setMockSearchParams("talk=selected&page=2");
  const fetchDetail = vi.fn();
  const items = [{ id: "selected" }];
  const { result } = renderHook(() => ({
    detail: useDetailUrl("talk", "talks", items, fetchDetail),
    pagination: usePaginationUrlParams(["DONE"]),
  }), { wrapper });
  act(() => result.current.pagination.setPage(3));
  expect(result.current.detail.selectedId).toBe("selected");
  expect(result.current.pagination.page).toBe(3);
  act(() => result.current.detail.close());
  expect(result.current.pagination.page).toBe(3);
  act(() => setMockSearchParams("talk=selected&page=2"));
  expect(result.current.detail.selected?.id).toBe("selected");
  expect(fetchDetail).not.toHaveBeenCalled();
});

it("reports a failed direct detail fetch without retrying indefinitely", async () => {
  setMockSearchParams("post=missing");
  const fetchDetail = vi.fn().mockRejectedValue(new Error("404"));
  const { result } = renderHook(() => useDetailUrl("post", "posts", [], fetchDetail), { wrapper });
  await waitFor(() => expect(result.current.error).toBe(true));
  expect(result.current.loading).toBe(false);
  expect(fetchDetail).toHaveBeenCalledTimes(1);
});

it.each([['/talks', 'talk'], ['/blog-posts', 'post'], ['/conferences', 'conference']])(
  "copies a shareable %s URL with an encoded identifier", async (path, parameter) => {
    const user = userEvent.setup();
    const write = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
    render(<CopyDetailLinkButton path={path} parameter={parameter} id="id & other" />);
    await user.click(screen.getByRole("button", { name: "Copier le lien" }));
    const url = new URL(write.mock.calls[0][0]);
    expect(url.pathname).toBe(path);
    expect(url.searchParams.get(parameter)).toBe("id & other");
    expect([...url.searchParams.keys()]).toEqual([parameter]);
    expect(await screen.findByText("Lien copié")).toBeInTheDocument();
  },
);

it("shows a copy failure without closing the detail", async () => {
  const user = userEvent.setup();
  vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("denied"));
  render(<CopyDetailLinkButton path="/talks" parameter="talk" id="id" />);
  await user.click(screen.getByRole("button", { name: "Copier le lien" }));
  expect(await screen.findByText(/Impossible de copier le lien/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Copier le lien" })).toBeEnabled();
});

it("copies from the icon-only action without showing a label or navigating", async () => {
  const user = userEvent.setup();
  const write = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
  const before = window.location.href;
  render(<CopyDetailLinkButton path="/talks" parameter="talk" id="listed" iconOnly />);
  const button = screen.getByRole("button", { name: "Copier le lien" });
  expect(button.textContent).toBe("");
  await user.click(button);
  expect(write).toHaveBeenCalledWith(`${window.location.origin}/talks?talk=listed`);
  expect(window.location.href).toBe(before);
});

it("exposes a contextual name and a tooltip description on keyboard focus", async () => {
  const user = userEvent.setup();
  render(<CopyDetailLinkButton path="/talks" parameter="talk" id="listed" iconOnly label="Copier le lien vers le talk : Accessibilité" />);
  const button = screen.getByRole("button", { name: "Copier le lien vers le talk : Accessibilité" });
  await user.tab();
  expect(button).toHaveFocus();
  expect(await screen.findByRole("tooltip")).toHaveTextContent("Copier le lien direct dans le presse-papiers");
  expect(button).toHaveAccessibleDescription("Copier le lien direct dans le presse-papiers");
  await user.keyboard("{Escape}");
  await waitFor(() => expect(screen.queryByRole("tooltip")).not.toBeInTheDocument());
  expect(button).toHaveAccessibleName("Copier le lien vers le talk : Accessibilité");
});
