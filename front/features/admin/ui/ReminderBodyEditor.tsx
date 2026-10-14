"use client";

import { useRef, useState } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Tooltip from "@mui/material/Tooltip";
import {
  Bold,
  Code,
  FileCode,
  Italic,
  Link as LinkIcon,
  List,
  ListOrdered,
  Quote,
  Strikethrough,
} from "lucide-react";
import {
  LinkBubbleMenu,
  LinkBubbleMenuHandler,
  MenuButton,
  MenuButtonBold,
  MenuButtonBulletedList,
  MenuButtonEditLink,
  MenuButtonItalic,
  MenuButtonOrderedList,
  MenuButtonUnderline,
  MenuControlsContainer,
  MenuDivider,
  MenuSelect,
  MenuSelectFontSize,
  RichTextEditor,
  RichTextReadOnly,
} from "mui-tiptap";
import StarterKit from "@tiptap/starter-kit";
import { FontSize, TextStyle } from "@tiptap/extension-text-style";
import { type BackendMessageTemplateDefinition } from "@/shared/api";

function HtmlTemplateEditor({
  definition,
  initialHtml,
  disabled,
  onChange,
}: {
  definition: BackendMessageTemplateDefinition;
  initialHtml: string;
  disabled: boolean;
  onChange: (html: string) => void;
}) {
  return (
    <RichTextEditor
      editable={!disabled}
      extensions={[
        StarterKit.configure({
          heading: false, blockquote: false, code: false, codeBlock: false,
          horizontalRule: false, strike: false,
          link: {
            openOnClick: false, autolink: false, linkOnPaste: false,
            HTMLAttributes: { target: null, rel: null, class: null },
            isAllowedUri: (url) => definition.links?.some((link) => url === `{{${link.variable}}}`) === true || /^https?:\/\//i.test(url),
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
          if (event.target.value) editor?.chain().focus().insertContent({ type: "text", text: `{{${event.target.value}}}` }).run();
        }}>
          {(definition.variables ?? []).map(({ name, label }) => <MenuItem key={name} value={name}>{label}</MenuItem>)}
        </MenuSelect>
        <MenuSelect inputProps={{ "aria-label": "Insérer une condition" }} value="" displayEmpty renderValue={() => "Condition"} disabled={!editor?.isEditable} onChange={(event) => {
          if (event.target.value) editor?.chain().focus().insertContent([
            { type: "paragraph", content: [{ type: "text", text: `{{#${event.target.value}}}` }] },
            { type: "paragraph", content: [{ type: "text", text: "Texte conditionnel à compléter" }] },
            { type: "paragraph", content: [{ type: "text", text: `{{/${event.target.value}}}` }] },
          ]).run();
        }}>
          {(definition.conditions ?? []).map(({ name, label }) => <MenuItem key={name} value={name}>{label}</MenuItem>)}
        </MenuSelect>
        {(definition.links ?? []).map((link) => <MenuButton key={link.variable} tooltipLabel={link.label ?? "Insérer un lien"} disabled={!editor?.isEditable} onClick={() => editor?.chain().focus().insertContent({ type: "text", text: link.text, marks: [{ type: "link", attrs: { href: `{{${link.variable}}}` } }] }).run()}>{link.label}</MenuButton>)}
      </MenuControlsContainer>}
    >
      {() => <LinkBubbleMenu
        formatHref={(value) => definition.links?.some((link) => value.trim() === `{{${link.variable}}}`) ? value.trim() : formatHref(value)}
        labels={{
          editLinkAddTitle: "Insérer un lien", editLinkEditTitle: "Modifier le lien",
          editLinkTextInputLabel: "Texte", editLinkHrefInputLabel: "URL",
          editLinkCancelButtonLabel: "Annuler", editLinkSaveButtonLabel: "Appliquer",
          viewLinkEditButtonLabel: "Modifier", viewLinkRemoveButtonLabel: "Retirer le lien",
        }}
      />}
    </RichTextEditor>
  );
}

function MarkdownTemplateEditor({
  definition,
  initialContent,
  disabled,
  onChange,
}: {
  definition: BackendMessageTemplateDefinition;
  initialContent: string;
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  const [prevInitialContent, setPrevInitialContent] = useState(initialContent);
  const [value, setValue] = useState(initialContent);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  if (prevInitialContent !== initialContent) {
    setPrevInitialContent(initialContent);
    setValue(initialContent);
  }

  const handleChange = (newVal: string) => {
    setValue(newVal);
    onChange(newVal);
  };

  const applyWrap = (prefix: string, suffix: string, placeholder = "texte") => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = value.substring(start, end);
    const content = selected || placeholder;
    const updated = value.substring(0, start) + prefix + content + suffix + value.substring(end);
    handleChange(updated);
    requestAnimationFrame(() => {
      textarea.focus();
      const selStart = start + prefix.length;
      const selEnd = selStart + content.length;
      textarea.setSelectionRange(selStart, selEnd);
    });
  };

  const applyLinePrefix = (prefix: string, placeholder = "élément") => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = value.substring(start, end);
    let updatedText: string;
    if (selected) {
      const lines = selected.split("\n").map(l => `${prefix}${l}`).join("\n");
      updatedText = value.substring(0, start) + lines + value.substring(end);
    } else {
      updatedText = value.substring(0, start) + `${prefix}${placeholder}` + value.substring(end);
    }
    handleChange(updatedText);
    requestAnimationFrame(() => {
      textarea.focus();
    });
  };

  const insertText = (toInsert: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const updated = value.substring(0, start) + toInsert + value.substring(end);
    handleChange(updated);
    requestAnimationFrame(() => {
      textarea.focus();
      const nextPos = start + toInsert.length;
      textarea.setSelectionRange(nextPos, nextPos);
    });
  };

  return (
    <div className="border border-border rounded-lg overflow-hidden bg-surface">
      <div className="flex flex-wrap items-center gap-1 p-1.5 border-b border-border bg-surface-muted/40">
        <Tooltip title="Gras (*texte*)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("*", "*", "texte en gras")} aria-label="Gras">
              <Bold size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Italique (_texte_)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("_", "_", "texte en italique")} aria-label="Italique">
              <Italic size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Barré (~texte~)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("~", "~", "texte barré")} aria-label="Barré">
              <Strikethrough size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Divider orientation="vertical" flexItem className="mx-1" />
        <Tooltip title="Code en ligne (`code`)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("`", "`", "code")} aria-label="Code">
              <Code size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Bloc de code (```)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("```\n", "\n```", "bloc de code")} aria-label="Bloc de code">
              <FileCode size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Citation (> citation)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyLinePrefix("> ", "citation")} aria-label="Citation">
              <Quote size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Divider orientation="vertical" flexItem className="mx-1" />
        <Tooltip title="Liste à puces (• élément)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyLinePrefix("• ", "élément")} aria-label="Liste à puces">
              <List size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Liste numérotée (1. élément)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyLinePrefix("1. ", "élément")} aria-label="Liste numérotée">
              <ListOrdered size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="Lien (<url|texte>)">
          <span>
            <IconButton size="small" disabled={disabled} onClick={() => applyWrap("<https://|", ">", "Libellé du lien")} aria-label="Insérer un lien">
              <LinkIcon size={16} />
            </IconButton>
          </span>
        </Tooltip>
        <Divider orientation="vertical" flexItem className="mx-1" />
        <Select
          size="small"
          inputProps={{ "aria-label": "Insérer une variable dans le corps" }}
          value=""
          displayEmpty
          renderValue={() => "Variable"}
          disabled={disabled}
          sx={{ height: 32, fontSize: "0.875rem" }}
          onChange={(event) => {
            if (event.target.value) insertText(`{{${event.target.value}}}`);
          }}
        >
          {(definition.variables ?? []).map(({ name, label }) => (
            <MenuItem key={name} value={name}>{label}</MenuItem>
          ))}
        </Select>
        <Select
          size="small"
          inputProps={{ "aria-label": "Insérer une condition" }}
          value=""
          displayEmpty
          renderValue={() => "Condition"}
          disabled={disabled}
          sx={{ height: 32, fontSize: "0.875rem" }}
          onChange={(event) => {
            if (event.target.value) {
              insertText(`{{#${event.target.value}}}\nTexte conditionnel à compléter\n{{/${event.target.value}}}\n`);
            }
          }}
        >
          {(definition.conditions ?? []).map(({ name, label }) => (
            <MenuItem key={name} value={name}>{label}</MenuItem>
          ))}
        </Select>
        {(definition.links ?? []).map((link) => (
          <Button
            key={link.variable}
            size="small"
            variant="outlined"
            disabled={disabled}
            sx={{ height: 32, fontSize: "0.75rem", textTransform: "none" }}
            onClick={() => insertText(`<${link.variable ? `{{${link.variable}}}` : "https://"}|${link.text || link.label}>`)}
          >
            {link.label}
          </Button>
        ))}
      </div>
      <textarea
        ref={textareaRef}
        role="textbox"
        aria-label="Corps du modèle"
        aria-multiline="true"
        disabled={disabled}
        value={value}
        onChange={(e) => handleChange(e.target.value)}
        className="w-full min-h-64 p-3 font-mono text-sm leading-relaxed bg-transparent border-0 resize-y focus:outline-none"
        placeholder="Rédigez votre modèle en syntaxe Markdown / Slack..."
      />
    </div>
  );
}

function TemplateHelp({ definition, isMarkdown }: { definition: BackendMessageTemplateDefinition; isMarkdown: boolean }) {
  return (
    <details className="text-sm text-text-muted mt-3">
      <summary className="cursor-pointer">Variables et conditions : aide à la rédaction</summary>
      <p>
        {isMarkdown
          ? "Les balises Mustache et la syntaxe Markdown Slack (*gras*, _italique_, <url|texte>) sont prises en charge. Leur valeur est remplacée dans l’aperçu."
          : "Les balises restent visibles dans l’éditeur. Leur valeur est remplacée dans l’aperçu. Placez les balises de condition dans des paragraphes séparés, sans mise en forme à l’intérieur des balises."}
      </p>
      <pre className="whitespace-pre-wrap bg-surface-muted rounded p-3">{definition.example}</pre>
      <p>{definition.help}</p>
      <ul className="list-disc pl-5">
        {(definition.conditions ?? []).map(({ name, label }) => (
          <li key={name}><code>{name}</code> : {label}</li>
        ))}
      </ul>
    </details>
  );
}

export function ReminderBodyEditor({
  definition,
  initialHtml,
  disabled,
  onChange,
}: {
  definition: BackendMessageTemplateDefinition;
  initialHtml: string;
  disabled: boolean;
  onChange: (html: string) => void;
}) {
  const isMarkdown = definition.format === "markdown";

  return (
    <fieldset className="border-0 p-0 m-0">
      <legend className="sr-only">Corps du modèle</legend>
      {isMarkdown ? (
        <MarkdownTemplateEditor
          definition={definition}
          initialContent={initialHtml}
          disabled={disabled}
          onChange={onChange}
        />
      ) : (
        <HtmlTemplateEditor
          definition={definition}
          initialHtml={initialHtml}
          disabled={disabled}
          onChange={onChange}
        />
      )}
      <TemplateHelp definition={definition} isMarkdown={isMarkdown} />
    </fieldset>
  );
}

function formatHref(url: string) {
  const clean = url.trim();
  if (!clean) return "";
  if (/^https?:\/\//i.test(clean)) return clean;
  return `https://${clean}`;
}

export function ReminderBodyReadOnly({ html }: { html: string }) {
  return (
    <Box sx={{ border: 1, borderColor: "divider", borderRadius: 1, p: 2, bgcolor: "background.paper" }}>
      <RichTextReadOnly
        extensions={[StarterKit, TextStyle, FontSize]}
        content={html}
      />
    </Box>
  );
}
