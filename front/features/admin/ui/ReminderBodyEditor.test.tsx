import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ReminderBodyEditor } from "./ReminderBodyEditor";

// jsdom has no layout; ProseMirror uses Range geometry when restoring focus.
Object.defineProperty(Range.prototype, "getClientRects", { configurable: true, value: () => [] });
Object.defineProperty(Range.prototype, "getBoundingClientRect", { configurable: true, value: () => new DOMRect() });

afterEach(cleanup);

it("preserves visible Qute, supported formatting and variable links through an editor round trip", async () => {
  const onChange = vi.fn();
  const initialHtml = '<p>{#if hasConference}</p><p><strong>{conferenceName}</strong> <span style="font-size: 24px">Conférence</span></p><p>{/if}</p><p><a href="{talksUrl}">Ouvrir Thezaurus</a></p>';
  const first = render(<ReminderBodyEditor initialHtml={initialHtml} disabled={false} onChange={onChange} />);
  const body = await screen.findByRole("textbox", { name: "Corps du modèle" });
  expect(body.innerHTML).toContain('{#if hasConference}');
  expect(body.innerHTML).toContain('href="{talksUrl}"');
  fireEvent.change(screen.getByRole("combobox", { name: "Insérer une variable dans le corps" }), { target: { value: "talkTitle" } });
  await waitFor(() => expect(onChange).toHaveBeenCalled());
  const html: string = onChange.mock.lastCall![0];
  expect(html).toContain("{talkTitle}");
  expect(html).toContain("<strong>{conferenceName}</strong>");
  expect(html).toContain("font-size: 24px");
  first.unmount();
  render(<ReminderBodyEditor initialHtml={html} disabled={false} onChange={onChange} />);
  const reopened = await screen.findByRole("textbox", { name: "Corps du modèle" });
  expect(reopened.innerHTML).toBe(html);
});
