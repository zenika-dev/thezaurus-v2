import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider } from "next-auth/react";
import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import dayjs from "dayjs";
import { queryKeys } from "@/shared/api";
import { CreateBlogPostDialog } from "@/features/blog-posts/ui/CreateBlogPostDialog";
import { BlogPostDetailsDialog } from "@/features/blog-posts/ui/BlogPostDetailsDialog";

afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); vi.useRealTimers(); });

it("préremplit le jour de Paris même si le navigateur est encore la veille", async () => {
  vi.stubEnv("TZ", "UTC");
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(new Date("2030-01-20T23:30:00Z"));
  const user = userEvent.setup();
  const { onSubmit } = renderCreate();
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Idée après minuit");
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ creationDate: "21-01-2030" })));
});

it.each([
  ["creationDate", "01-01-2999", "La date de création ne peut pas être future"],
  ["actualPublicationDate", "01-01-2999", "La date de publication réelle ne peut pas être future"],
  ["publicationDate", "31-02-2026", "Indiquez une date valide"],
  ["link", "javascript:alert(1)", "Indiquez une URL HTTP ou HTTPS valide"],
])("refuse %s invalide lors de la modification", async (field, value, message) => {
  const onUpdate = vi.fn();
  render(<QueryClientProvider client={new QueryClient()}>
    <BlogPostDetailsDialog open onClose={vi.fn()} onDelete={vi.fn()} onUpdate={onUpdate}
      post={{ id: "invalid", title: "Article", writers: [{ name: "Alice" }], tags: [], status: "IDEA", creationDate: "", [field]: value }} />
  </QueryClientProvider>);
  await userEvent.setup().click(screen.getByRole("button", { name: "Enregistrer" }));
  expect(await screen.findByText(message)).toBeInTheDocument();
  expect(onUpdate).not.toHaveBeenCalled();
});

function renderCreate(onSubmit = vi.fn(), office = "nantes") {
  const client = new QueryClient({ defaultOptions: { queries: { staleTime: Infinity, retry: false } } });
  client.setQueryData(queryKeys.profile.me(), { name: "Alice", email: "alice@zenika.com", office });
  render(
    <SessionProvider session={{ user: { name: "Alice", email: "alice@zenika.com", roles: ["CONSULTANT"] }, expires: "2099-01-01" }}>
      <QueryClientProvider client={client}>
        <CreateBlogPostDialog open onClose={vi.fn()} onSubmit={onSubmit} />
      </QueryClientProvider>
    </SessionProvider>,
  );
  return { onSubmit, client };
}

it("enregistre une idée avec l’auteur, l’agence et la date préremplis, sans tags", async () => {
  const user = userEvent.setup();
  const { onSubmit } = renderCreate();
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Une idée");
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
    title: "Une idée", status: "IDEA", tags: [], office: "nantes",
    creationDate: dayjs().format("DD-MM-YYYY"),
    writers: [{ name: "Alice", email: "alice@zenika.com" }],
  })));
});

it("respecte une agence volontairement vidée lorsque le profil arrive ensuite", async () => {
  const user = userEvent.setup();
  const { onSubmit, client } = renderCreate(vi.fn(), "");
  await user.click(screen.getByRole("combobox", { name: "Agence" }));
  await user.click(screen.getByRole("option", { name: "Rennes" }));
  await user.click(screen.getByRole("combobox", { name: "Agence" }));
  await user.click(screen.getByRole("option", { name: "Non renseignée" }));
  await act(async () => { client.setQueryData(queryKeys.profile.me(), { office: "nantes" }); });
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Idée sans agence");
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ office: "" })));
});

it("permet de vider la date de création préremplie", async () => {
  const user = userEvent.setup();
  const { onSubmit } = renderCreate();
  const date = screen.getByRole("group", { name: "Date de création" });
  await user.hover(date);
  await user.click(within(date).getByRole("button", { name: /clear/i }));
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Idée sans date");
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ creationDate: "" })));
});

it.each(["En relecture", "Prêt à publier"])("exige le texte à relire pour %s", async (status) => {
  const user = userEvent.setup();
  const { onSubmit } = renderCreate();
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Article à relire");
  await user.click(screen.getByRole("combobox", { name: "Statut" }));
  await user.click(screen.getByRole("option", { name: status }));
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  expect(await screen.findByText("Le lien du texte à relire est requis")).toBeInTheDocument();
  expect(onSubmit).not.toHaveBeenCalled();
  await user.type(screen.getByRole("textbox", { name: /Lien du texte/ }), "https://docs.google.com/document/d/article");
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalled());
});

it("exige une URL publique et une date réelle pour publier", async () => {
  const user = userEvent.setup();
  const { onSubmit } = renderCreate();
  await user.type(screen.getByRole("textbox", { name: /Titre/ }), "Article publié");
  await user.click(screen.getByRole("combobox", { name: "Statut" }));
  await user.click(screen.getByRole("option", { name: "Publié" }));
  await user.click(screen.getByRole("button", { name: "Créer le post" }));
  expect(await screen.findByText("L’URL publique est requise")).toBeInTheDocument();
  expect(screen.getByText("La date de publication réelle est requise")).toBeInTheDocument();
  expect(onSubmit).not.toHaveBeenCalled();
});

it("conserve les deux dates de publication, les liens et l’agence lors d’un retour à Idée", async () => {
  const user = userEvent.setup();
  const onUpdate = vi.fn();
  const post = {
    id: "published", title: "Article", writers: [{ name: "Alice" }], tags: [],
    status: "PUBLISHED" as const, office: "nantes" as const,
    creationDate: "20-01-2026", publicationDate: "12-01-2026", actualPublicationDate: "10-01-2026",
    link: "https://blog.zenika.com/article", googleDocDraftLink: "https://docs.google.com/document/d/article",
  };
  render(<QueryClientProvider client={new QueryClient()}>
    <BlogPostDetailsDialog open post={post} onClose={vi.fn()} onUpdate={onUpdate} onDelete={vi.fn()} />
  </QueryClientProvider>);
  await user.click(screen.getByRole("combobox", { name: "Statut" }));
  await user.click(screen.getByRole("option", { name: "Idée" }));
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
  await waitFor(() => expect(onUpdate).toHaveBeenCalledWith({ ...post, status: "IDEA", writers: [{ name: "Alice", email: undefined }] }));
});
