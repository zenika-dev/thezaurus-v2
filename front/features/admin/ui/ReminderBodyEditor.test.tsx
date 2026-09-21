import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ReminderBodyEditor } from "./ReminderBodyEditor";

// jsdom has no layout; ProseMirror uses Range geometry when restoring focus.
Object.defineProperty(Range.prototype, "getClientRects", { configurable: true, value: () => [] });
Object.defineProperty(Range.prototype, "getBoundingClientRect", { configurable: true, value: () => new DOMRect() });

afterEach(cleanup);

const definition = {
  variables: [{ name: "talkTitle", label: "Titre du talk" }],
  conditions: [{ name: "hasConference", label: "Conférence renseignée" }],
  links: [{ variable: "talksUrl", label: "Lien vers Thezaurus", text: "Ouvrir Thezaurus" }],
};

it("does not report a document change when saving or previewing disables the editor", async () => {
  const onChange = vi.fn();
  const initialHtml = '<p><span style="font-size: 24px">Texte enregistré</span></p>';
  const props = { definition, initialHtml, onChange };
  const { rerender } = render(<ReminderBodyEditor {...props} disabled={false} />);
  await screen.findByRole("textbox", { name: "Corps du modèle" });
  rerender(<ReminderBodyEditor {...props} disabled={true} />);
  await waitFor(() => expect(screen.getByRole("textbox", { name: "Corps du modèle" }).getAttribute("contenteditable")).toBe("false"));
  rerender(<ReminderBodyEditor {...props} disabled={false} />);
  await waitFor(() => expect(screen.getByRole("textbox", { name: "Corps du modèle" }).getAttribute("contenteditable")).toBe("true"));
  expect(onChange).not.toHaveBeenCalled();
});

it("inserts a Qute link through the standard link dialog", async () => {
  const onChange = vi.fn();
  render(<ReminderBodyEditor definition={definition} initialHtml="<p></p>" disabled={false} onChange={onChange} />);
  await screen.findByRole("textbox", { name: "Corps du modèle" });
  const linkButton = screen.getByRole("button", { name: /Insérer ou modifier un lien/ });
  await waitFor(() => expect(linkButton.hasAttribute("disabled")).toBe(false));
  fireEvent.click(linkButton);
  fireEvent.change(await screen.findByRole("textbox", { name: /^Texte/ }), { target: { value: "Voir les talks" } });
  fireEvent.change(screen.getByRole("textbox", { name: /^URL/ }), { target: { value: "{talksUrl}" } });
  fireEvent.click(screen.getByRole("button", { name: "Appliquer" }));
  await waitFor(() => expect(onChange).toHaveBeenCalled());
  expect(onChange.mock.lastCall![0]).toContain('<a href="{talksUrl}">Voir les talks</a>');
});

it("preserves visible Qute, supported formatting and variable links through an editor round trip", async () => {
  const onChange = vi.fn();
  const initialHtml = '<p>{#if hasConference}</p><p><strong>{conferenceName}</strong> <span style="font-size: 24px">Conférence</span></p><p>{/if}</p><p><a href="{talksUrl}">Ouvrir Thezaurus</a></p>';
  const first = render(<ReminderBodyEditor definition={definition} initialHtml={initialHtml} disabled={false} onChange={onChange} />);
  const body = await screen.findByRole("textbox", { name: "Corps du modèle" });
  expect(body.innerHTML).toContain('{#if hasConference}');
  expect(body.innerHTML).toContain('href="{talksUrl}"');
  fireEvent.mouseDown(screen.getByRole("combobox", { name: "Insérer une variable dans le corps" }));
  fireEvent.click(await screen.findByRole("option", { name: "Titre du talk" }));
  await waitFor(() => expect(onChange).toHaveBeenCalled());
  const html: string = onChange.mock.lastCall![0];
  expect(html).toContain("{talkTitle}");
  expect(html).toContain("<strong>{conferenceName}</strong>");
  expect(html).toContain("font-size: 24px");
  first.unmount();
  render(<ReminderBodyEditor definition={definition} initialHtml={html} disabled={false} onChange={onChange} />);
  const reopened = await screen.findByRole("textbox", { name: "Corps du modèle" });
  expect(reopened.innerHTML).toBe(html);
});
