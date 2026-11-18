import { render, screen, waitFor } from "@testing-library/react";

import userEvent from "@testing-library/user-event";

import { afterEach, expect, it, vi } from "vitest";

import type { BlogPostData } from "@/entities/post";

import { BlogPostVisualButton } from "./BlogPostVisualButton";



const post: BlogPostData = { id: "post-1", title: "Article de test", writers: [{ name: "Alice" }], creationDate: "16-11-2026", tags: [], status: "IDEA" };



afterEach(() => { window.dispatchEvent(new Event("pagehide")); vi.unstubAllGlobals(); vi.restoreAllMocks(); });



it.each([1, 2])("downloads the generated PNG for %i writers", async (count) => {

  const fetchMock = vi.fn().mockResolvedValue({ ok: true, blob: async () => new Blob(["png"], { type: "image/png" }) });

  vi.stubGlobal("fetch", fetchMock);

  const createUrl = vi.fn().mockReturnValue("blob:visual");

  vi.stubGlobal("URL", { createObjectURL: createUrl, revokeObjectURL: vi.fn() });

  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

  render(<BlogPostVisualButton post={{ ...post, writers: Array.from({ length: count }, (_, i) => ({ name: `Speaker ${i}` })) }} />);

  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));

  await waitFor(() => expect(click).toHaveBeenCalledOnce());

  expect(fetchMock).toHaveBeenCalledWith("/api/backend/blog-posts/post-1/visual", { method: "POST" });

  expect(createUrl).toHaveBeenCalledWith(expect.any(Blob));

});



it("explains access failures and allows retrying", async () => {

  vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 403 }));

  render(<BlogPostVisualButton post={post} />);

  await userEvent.click(screen.getByRole("button", { name: "Télécharger le visuel" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("pas accès à la génération");

  expect(screen.getByRole("button", { name: "Télécharger le visuel" })).toBeEnabled();

});



it.each([0, 3])("disables generation for %i authors", (count) => {

  render(<BlogPostVisualButton post={{ ...post, writers: Array.from({ length: count }, () => post.writers[0]) }} />);

  expect(screen.getByRole("button")).toBeDisabled();

});



it("disables generation while the post has unsaved changes", () => {

  render(<BlogPostVisualButton post={post} disabled />);

  expect(screen.getByRole("button", { name: "Télécharger le visuel" })).toBeDisabled();

});


it("explains the author limit when hovering over the disabled card action", async () => {
  const fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  render(<BlogPostVisualButton post={{ ...post, writers: Array.from({ length: 3 }, () => post.writers[0]) }} iconOnly />);
  const button = screen.getByRole("button", { name: "Télécharger le visuel de l’article : Article de test" });
  expect(button).toBeDisabled();
  await userEvent.hover(button.parentElement!);
  expect(await screen.findByRole("tooltip")).toHaveTextContent("2 auteurs maximum");
  expect(fetchMock).not.toHaveBeenCalled();
});
