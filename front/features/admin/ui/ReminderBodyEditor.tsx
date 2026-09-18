"use client";

import { useRef } from "react";
import {
  formatHref, LinkBubbleMenu, LinkBubbleMenuHandler, MenuButtonBold,
  MenuButtonBulletedList, MenuButtonEditLink, MenuButtonItalic,
  MenuButtonOrderedList, MenuButtonUnderline, MenuControlsContainer,
  MenuDivider, MenuSelectFontSize, RichTextEditor, type RichTextEditorRef,
} from "mui-tiptap";
import StarterKit from "@tiptap/starter-kit";
import { FontSize, TextStyle } from "@tiptap/extension-text-style";
import { Button } from "@mui/material";

export const templateVariables = [
  ["talkTitle", "Titre du talk"],
  ["talkDate", "Date du talk"],
  ["conferenceName", "Conférence"],
  ["talksUrl", "Lien vers Thezaurus"],
] as const;

const conditions = [
  ["hasConference", "Conférence renseignée"],
  ["hasDate", "Date renseignée"],
  ["missingVideo", "Vidéo manquante"],
  ["missingAudience", "Audience manquante"],
] as const;

export function ReminderBodyEditor({ initialHtml, disabled, onChange }: {
  initialHtml: string;
  disabled: boolean;
  onChange: (html: string) => void;
}) {
  const editorRef = useRef<RichTextEditorRef>(null);

  return <fieldset disabled={disabled} className="border-0 p-0 m-0 min-w-0">
    <legend className="font-semibold mb-2">Corps du modèle</legend>
    <RichTextEditor
      ref={editorRef}
      immediatelyRender={false}
      editable={!disabled}
      extensions={[
        StarterKit.configure({
          heading: false, blockquote: false, code: false, codeBlock: false,
          horizontalRule: false, strike: false,
          link: {
            openOnClick: false, autolink: false, linkOnPaste: false,
            HTMLAttributes: { target: null, rel: null, class: null },
            isAllowedUri: (url) => url === "{talksUrl}" || /^https?:\/\//i.test(url),
          },
        }),
        TextStyle, FontSize, LinkBubbleMenuHandler,
      ]}
      content={initialHtml}
      editorProps={{ attributes: {
        role: "textbox", "aria-label": "Corps du modèle", "aria-multiline": "true",
      } }}
      onUpdate={({ editor }) => onChange(editor.isEmpty ? "" : editor.getHTML())}
      sx={{ "& .ProseMirror": { minHeight: 256, fontSize: "16px" } }}
      renderControls={() => <MenuControlsContainer>
        <MenuButtonBold tooltipLabel="Gras" />
        <MenuButtonItalic tooltipLabel="Italique" />
        <MenuButtonUnderline tooltipLabel="Souligné" />
        <MenuDivider />
        <MenuButtonBulletedList tooltipLabel="Liste à puces" />
        <MenuButtonOrderedList tooltipLabel="Liste numérotée" />
        <MenuDivider />
        <MenuSelectFontSize
          tooltipTitle="Taille de police"
          inputProps={{ "aria-label": "Taille de police" }}
          options={["12px", "14px", "16px", "18px", "24px", "32px"]}
          unsetOptionLabel="Par défaut (16 px)"
          emptyLabel="16"
        />
        <MenuDivider />
        <MenuButtonEditLink tooltipLabel="Insérer ou modifier un lien" />
      </MenuControlsContainer>}
    >
      {() => <LinkBubbleMenu
        formatHref={(value) => value.trim() === "{talksUrl}" ? "{talksUrl}" : formatHref(value)}
        labels={{
          editLinkAddTitle: "Insérer un lien", editLinkEditTitle: "Modifier le lien",
          editLinkTextInputLabel: "Texte", editLinkHrefInputLabel: "URL",
          editLinkCancelButtonLabel: "Annuler", editLinkSaveButtonLabel: "Appliquer",
          viewLinkEditButtonLabel: "Modifier", viewLinkRemoveButtonLabel: "Retirer le lien",
        }}
      />}
    </RichTextEditor>
      <div className="flex flex-wrap gap-2 p-2 border-b border-border">
        <select aria-label="Insérer une variable dans le corps" value="" className="rounded border border-border bg-surface p-2 text-sm" onChange={(event) => {
          if (event.target.value) editorRef.current?.editor?.chain().focus().insertContent({ type: "text", text: `{${event.target.value}}` }).run();
        }}>
          <option value="">Insérer une variable…</option>
          {templateVariables.map(([variable, label]) => <option key={variable} value={variable}>{label}</option>)}
        </select>
        <select aria-label="Insérer une condition" value="" className="rounded border border-border bg-surface p-2 text-sm" onChange={(event) => {
          if (event.target.value) editorRef.current?.editor?.chain().focus().insertContent([
            { type: "paragraph", content: [{ type: "text", text: `{#if ${event.target.value}}` }] },
            { type: "paragraph", content: [{ type: "text", text: "Texte conditionnel à compléter" }] },
            { type: "paragraph", content: [{ type: "text", text: "{/if}" }] },
          ]).run();
        }}>
          <option value="">Insérer une condition…</option>
          {conditions.map(([condition, label]) => <option key={condition} value={condition}>{label}</option>)}
        </select>
        <Button onClick={() => editorRef.current?.editor?.chain().focus().insertContent({ type: "text", text: "Ouvrir Thezaurus", marks: [{ type: "link", attrs: { href: "{talksUrl}" } }] }).run()}>Lien vers Thezaurus</Button>
      </div>
    <details className="text-sm text-text-muted mt-3">
      <summary className="cursor-pointer">Variables et conditions : aide à la rédaction</summary>
      <p>Les balises restent visibles dans l’éditeur. Leur valeur est remplacée dans l’aperçu. Placez les balises de condition dans des paragraphes séparés, sans mise en forme à l’intérieur des balises.</p>
      <pre className="whitespace-pre-wrap bg-surface-muted rounded p-3">{"{#if hasConference}\nLors de {conferenceName}\n{#else}\nMerci pour votre talk {talkTitle}\n{/if}"}</pre>
      <p>Date et conférence absentes donnent un texte vide. Une audience de zéro est renseignée. Le lien ouvre la liste des talks.</p>
      <ul className="list-disc pl-5">{conditions.map(([condition, label]) => <li key={condition}><code>{condition}</code> : {label}</li>)}</ul>
    </details>
  </fieldset>;
}
