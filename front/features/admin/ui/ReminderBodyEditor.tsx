"use client";

import { useState } from "react";
import { EditorContent, useEditor, useEditorState } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { FontSize, TextStyle } from "@tiptap/extension-text-style";
import { Button, TextField } from "@mui/material";

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
  const [linkUrl, setLinkUrl] = useState("");
  const [linkError, setLinkError] = useState("");
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({
        heading: false, blockquote: false, code: false, codeBlock: false,
        horizontalRule: false, strike: false,
        link: {
          openOnClick: false, autolink: false, linkOnPaste: false,
          HTMLAttributes: { target: null, rel: null, class: null },
          isAllowedUri: (url) => url === "{talksUrl}" || /^https?:\/\//i.test(url),
        },
      }),
      TextStyle,
      FontSize,
    ],
    content: initialHtml,
    editorProps: { attributes: {
      role: "textbox", "aria-label": "Corps du modèle", "aria-multiline": "true",
      class: "min-h-64 p-4 outline-none text-base [&_p]:my-2 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_a]:text-blue-700 [&_a]:underline",
    } },
    onUpdate: ({ editor }) => onChange(editor.isEmpty ? "" : editor.getHTML()),
  });
  const active = useEditorState({ editor, selector: ({ editor }) => editor ? {
    bold: editor.isActive("bold"), italic: editor.isActive("italic"), underline: editor.isActive("underline"),
    bulletList: editor.isActive("bulletList"), orderedList: editor.isActive("orderedList"),
    size: editor.getAttributes("textStyle").fontSize ?? "16px",
  } : null });

  function addLink() {
    if (!editor) return;
    if (linkUrl !== "{talksUrl}") {
      try {
        const url = new URL(linkUrl);
        if (!["http:", "https:"].includes(url.protocol)) throw new Error();
      } catch {
        setLinkError("Utilisez une URL http(s) ou {talksUrl}.");
        return;
      }
    }
    editor.chain().focus().extendMarkRange("link").setLink({ href: linkUrl }).run();
    setLinkError("");
  }

  return <fieldset disabled={disabled} className="border-0 p-0 m-0 min-w-0">
    <legend className="font-semibold mb-2">Corps du modèle</legend>
    <div className="rounded-xl border border-border overflow-hidden bg-surface">
      <div role="toolbar" aria-label="Mise en forme" className="flex flex-wrap gap-1 border-b border-border p-2">
        <Button variant={active?.bold ? "contained" : "text"} aria-pressed={active?.bold ?? false} onClick={() => editor?.chain().focus().toggleBold().run()}>Gras</Button>
        <Button variant={active?.italic ? "contained" : "text"} aria-pressed={active?.italic ?? false} onClick={() => editor?.chain().focus().toggleItalic().run()}>Italique</Button>
        <Button variant={active?.underline ? "contained" : "text"} aria-pressed={active?.underline ?? false} onClick={() => editor?.chain().focus().toggleUnderline().run()}>Souligné</Button>
        <Button variant={active?.bulletList ? "contained" : "text"} aria-pressed={active?.bulletList ?? false} onClick={() => editor?.chain().focus().toggleBulletList().run()}>Puces</Button>
        <Button variant={active?.orderedList ? "contained" : "text"} aria-pressed={active?.orderedList ?? false} onClick={() => editor?.chain().focus().toggleOrderedList().run()}>Liste numérotée</Button>
        <label className="flex items-center gap-2 text-sm">Taille
          <select aria-label="Taille de police" value={active?.size ?? "16px"} onChange={(event) => editor?.chain().focus().setFontSize(event.target.value).run()} className="rounded border border-border bg-surface p-2">
            {[12, 14, 16, 18, 24, 32].map((size) => <option key={size} value={`${size}px`}>{size} px</option>)}
          </select>
        </label>
      </div>
      <div className="flex flex-wrap gap-2 p-2 border-b border-border">
        <select aria-label="Insérer une variable dans le corps" value="" className="rounded border border-border bg-surface p-2 text-sm" onChange={(event) => {
          if (event.target.value) editor?.chain().focus().insertContent({ type: "text", text: `{${event.target.value}}` }).run();
        }}>
          <option value="">Insérer une variable…</option>
          {templateVariables.map(([variable, label]) => <option key={variable} value={variable}>{label}</option>)}
        </select>
        <select aria-label="Insérer une condition" value="" className="rounded border border-border bg-surface p-2 text-sm" onChange={(event) => {
          if (event.target.value) editor?.chain().focus().insertContent([
            { type: "paragraph", content: [{ type: "text", text: `{#if ${event.target.value}}` }] },
            { type: "paragraph", content: [{ type: "text", text: "Texte conditionnel à compléter" }] },
            { type: "paragraph", content: [{ type: "text", text: "{/if}" }] },
          ]).run();
        }}>
          <option value="">Insérer une condition…</option>
          {conditions.map(([condition, label]) => <option key={condition} value={condition}>{label}</option>)}
        </select>
        <Button onClick={() => editor?.chain().focus().insertContent({ type: "text", text: "Ouvrir Thezaurus", marks: [{ type: "link", attrs: { href: "{talksUrl}" } }] }).run()}>Lien vers Thezaurus</Button>
      </div>
      <div className="flex flex-wrap items-start gap-2 p-2 border-b border-border">
        <TextField label="Lien pour le texte sélectionné" size="small" value={linkUrl} placeholder="https://… ou {talksUrl}" onChange={(event) => setLinkUrl(event.target.value)} error={!!linkError} helperText={linkError} />
        <Button onClick={addLink} disabled={!linkUrl}>Appliquer le lien</Button>
        <Button onClick={() => editor?.chain().focus().extendMarkRange("link").unsetLink().run()}>Retirer le lien</Button>
      </div>
      <div inert={disabled}><EditorContent editor={editor} /></div>
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
