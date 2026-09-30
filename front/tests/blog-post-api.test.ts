import { afterEach, expect, it, vi } from "vitest";
import { postApi, type BlogPostData } from "@/entities/post";

afterEach(() => vi.unstubAllGlobals());

it("transmet séparément les dates prévue et réelle et restitue l’agence lors de la lecture", async () => {
  const stored: Record<string, unknown>[] = [];
  vi.stubGlobal("fetch", vi.fn(async (_url: string, init?: RequestInit) => {
    if (init?.method === "POST") {
      stored.push(JSON.parse(String(init.body)));
      return Response.json(stored[0], { status: 201 });
    }
    return Response.json(stored);
  }));
  const post: BlogPostData = {
    id: "article", title: "Article", writers: [{ name: "Alice" }], status: "PUBLISHED", office: "nantes", tags: [],
    creationDate: "20-01-2026", publicationDate: "12-01-2026", actualPublicationDate: "10-01-2026",
    link: "https://blog.zenika.com/article", googleDocDraftLink: "",
  };
  await postApi.createPost(post);
  expect(stored[0]).toMatchObject({
    creationDate: "2026-01-20T00:00:00", publicationDate: "2026-01-12T00:00:00",
    actualPublicationDate: "2026-01-10T00:00:00", office: "nantes",
  });
  expect(await postApi.getPosts()).toEqual([post]);
});

it("transmet explicitement les dates effacées sans en recréer", async () => {
  const fetch = vi.fn<typeof globalThis.fetch>(async () => Response.json({}));
  vi.stubGlobal("fetch", fetch);
  await postApi.updatePost({ id: "article", title: "Idée", writers: [{ name: "Alice" }], status: "IDEA", tags: [], creationDate: "", publicationDate: "", actualPublicationDate: "" });
  const request: RequestInit = fetch.mock.calls[0]?.[1] ?? {};
  expect(JSON.parse(String(request.body))).toMatchObject({ creationDate: null, publicationDate: null, actualPublicationDate: null });
});
