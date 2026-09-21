"use client";

import {
  formatHref, LinkBubbleMenu, LinkBubbleMenuHandler, MenuButtonBold,
  MenuButtonBulletedList, MenuButtonEditLink, MenuButtonItalic,
  MenuButtonOrderedList, MenuButtonUnderline, MenuControlsContainer,
  MenuButton, MenuDivider, MenuSelect, MenuSelectFontSize, RichTextEditor,
} from "mui-tiptap";
import StarterKit from "@tiptap/starter-kit";
import { FontSize, TextStyle } from "@tiptap/extension-text-style";
import { MenuItem } from "@mui/material";

import type { BackendMessageTemplateDefinition } from "@/shared/api";

export function ReminderBodyEditor({ initialHtml, disabled, onChange, definition }: {
  definition: BackendMessageTemplateDefinition;
  initialHtml: string;
  disabled: boolean;
  onChange: (html: string) => void;
}) {
  return <fieldset disabled={disabled} className="border-0 p-0 m-0 min-w-0">
    <legend className="font-semibold mb-2">Corps du modèle</legend>
    <RichTextEditor
      immediatelyRender={false}
      editable={!disabled}
      extensions={[
        StarterKit.configure({
          heading: false, blockquote: false, code: false, codeBlock: false,
          horizontalRule: false, strike: false,
          link: {
            openOnClick: false, autolink: false, linkOnPaste: false,
            HTMLAttributes: { target: null, rel: null, class: null },
            isAllowedUri: (url) => definition.links?.some((link) => url === `{${link.variable}}`) === true || /^https?:\/\//i.test(url),
          },
        }),
        TextStyle, FontSize, LinkBubbleMenuHandler,
      ]}
      content={initialHtml}
      editorProps={{ attributes: {
        role: "textbox", "aria-label": "Corps du modèle", "aria-multiline": "true",
      } }}
      onUpdate={({ editor, transaction }) => {
        if (transaction.docChanged) onChange(editor.isEmpty ? "" : editor.getHTML());
      }}
      sx={{ "& .ProseMirror": { minHeight: 256, fontSize: "16px" } }}
      renderControls={(editor) => <MenuControlsContainer>
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
        <MenuDivider />
        <MenuSelect inputProps={{ "aria-label": "Insérer une variable dans le corps" }} value="" displayEmpty renderValue={() => "Variable"} disabled={!editor?.isEditable} onChange={(event) => {
          if (event.target.value) editor?.chain().focus().insertContent({ type: "text", text: `{${event.target.value}}` }).run();
        }}>
          {(definition.variables ?? []).map(({ name, label }) => <MenuItem key={name} value={name}>{label}</MenuItem>)}
        </MenuSelect>
        <MenuSelect inputProps={{ "aria-label": "Insérer une condition" }} value="" displayEmpty renderValue={() => "Condition"} disabled={!editor?.isEditable} onChange={(event) => {
          if (event.target.value) editor?.chain().focus().insertContent([
            { type: "paragraph", content: [{ type: "text", text: `{#if ${event.target.value}}` }] },
            { type: "paragraph", content: [{ type: "text", text: "Texte conditionnel à compléter" }] },
            { type: "paragraph", content: [{ type: "text", text: "{/if}" }] },
          ]).run();
        }}>
          {(definition.conditions ?? []).map(({ name, label }) => <MenuItem key={name} value={name}>{label}</MenuItem>)}
        </MenuSelect>
        {(definition.links ?? []).map((link) => <MenuButton key={link.variable} tooltipLabel={link.label ?? "Insérer un lien"} disabled={!editor?.isEditable} onClick={() => editor?.chain().focus().insertContent({ type: "text", text: link.text, marks: [{ type: "link", attrs: { href: `{${link.variable}}` } }] }).run()}>{link.label}</MenuButton>)}
      </MenuControlsContainer>}
    >
      {() => <LinkBubbleMenu
        formatHref={(value) => definition.links?.some((link) => value.trim() === `{${link.variable}}`) ? value.trim() : formatHref(value)}
        labels={{
          editLinkAddTitle: "Insérer un lien", editLinkEditTitle: "Modifier le lien",
          editLinkTextInputLabel: "Texte", editLinkHrefInputLabel: "URL",
          editLinkCancelButtonLabel: "Annuler", editLinkSaveButtonLabel: "Appliquer",
          viewLinkEditButtonLabel: "Modifier", viewLinkRemoveButtonLabel: "Retirer le lien",
        }}
      />}
    </RichTextEditor>
    <details className="text-sm text-text-muted mt-3">
      <summary className="cursor-pointer">Variables et conditions : aide à la rédaction</summary>
      <p>Les balises restent visibles dans l’éditeur. Leur valeur est remplacée dans l’aperçu. Placez les balises de condition dans des paragraphes séparés, sans mise en forme à l’intérieur des balises.</p>
      <pre className="whitespace-pre-wrap bg-surface-muted rounded p-3">{definition.example}</pre>
      <p>{definition.help}</p>
      <ul className="list-disc pl-5">{(definition.conditions ?? []).map(({ name, label }) => <li key={name}><code>{name}</code> : {label}</li>)}</ul>
    </details>
  </fieldset>;
}
