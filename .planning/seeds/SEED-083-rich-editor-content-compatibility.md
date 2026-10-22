---
id: SEED-083
status: dormant
planted: 2026-10-23
planted_during: owner-requested first-priority backlog capture on main
trigger_when: a note contains content the rich editor cannot keep
scope: small
---

# SEED-083: Make incompatible note content editable in Donut

## Why This Matters

A note author encountering "This note has content the rich editor cannot keep."
needs a direct way to make the note editable in Donut. The owner requested a
conversion button and a reasonable warning layout as the first backlog story.

## Story Decomposition

<a id="story-1"></a>

### Make incompatible note content editable with one conversion button

**Identity:** SEED-083#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-convert-incompatible-note-content/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"ac954ac4b0b7cd9bdb9031841776732e55eeefb084a92f1696a270c5c570e097","plan":"c56b6d5cad2fd1db36d6e6249aafa3b0d90a122458e6562418534b7cbb5ba9b9"}}
```

**Goal:** A note author whose note opens read-only in the rich editor with
"This note has content the rich editor cannot keep." can, with one explicit
action, convert the body into content the rich editor keeps and continue
editing there, without losing the note's readable text. This restores the path
from captured content (pasted, imported, or written elsewhere) to everyday
note editing and refinement.

**Scope:**

- Keep the warning "This note has content the rich editor cannot keep."
- Delete the accompanying instruction "Switch to Markdown mode to edit it."
  and its exclusively dependent code and fixtures. At implementation acceptance,
  sweep product references to that instruction as a separate reading; prove
  the replacement conversion behavior through its observable results.
- Offer one clearly labeled conversion button in the warning, with one short
  line telling the author that conversion keeps the text and removes formatting
  the rich editor cannot keep. Activating it, with no confirmation step:
  - replaces the body with a converted body the rich editor keeps, saved
    through the ordinary content-edit path, so the existing note undo restores
    the previous content;
  - keeps every word of the body's readable text, in order, with words and
    lines still separated as they read before;
  - keeps body parts the editor already keeps unchanged, and keeps formatting
    the editor supports where doing so does not lose text; formatting the
    editor cannot hold may be dropped (see key examples);
  - leaves the frontmatter as written;
  - removes the warning and makes the editor editable in place.
- Make the warning and button fit the existing UI: clear visual hierarchy
  (message first, consequence line, then the action), comfortable spacing, a
  readable action label, and wrapping that works on narrow screens without
  clipping or overlap.
- Naturally general: every place that uses the rich note editor (note bodies
  and notebook/folder READMEs) shows the same warning and button; no separate
  verification commitment for READMEs.
- Unchanged: the frontmatter-error warning and its "fix the frontmatter" hint;
  read-only viewers see no warning or button, as today.
- Deferred: improving the rich editor itself so that more constructs (task
  checkboxes, collapsible sections, strikethrough, hard line breaks, code-block
  languages) are kept without conversion; a preview or per-construct choice
  before converting.

**Key examples** (observed unsupported constructs from the current
compatibility check; the editor's raw held content loses text for several of
them, so conversion is not just saving what the editor shows):

- `<details><summary>S</summary>Inner</details>` → converted body shows "S"
  and "Inner" as ordinary text (the raw held content would drop both).
- `text ~~strike~~ and more` → "strike" stays as plain text (the raw held
  content deletes it).
- `line one` + two trailing spaces + newline + `line two` (hard line break) → "line one" and "line two" stay
  separate lines or paragraphs, never "line oneline two".
- `**a** *b*` → "a" and "b" stay separate words; their emphasis may be dropped
  if keeping it would join them.
- `- [ ] todo\n- [x] done` → a bullet list "todo", "done"; checkbox state is
  dropped (accepted trade-off, recoverable with undo).
- Two fenced code blocks, one tagged `ts` → their code text is kept as code;
  the language tag and the split between blocks may be dropped.
- An editable note shows the warning → the author activates the conversion
  button → the warning disappears, the editor is editable, the converted body
  is saved; the author types and saves more; reopening the note opens it
  editable in the rich editor with no warning.
- The author regrets the conversion → the existing note undo ("undo edit
  content") restores the original body.
- The same warning on a narrow (phone-width) screen → message, consequence line,
  and button wrap without clipping or overlap, and the button is easy to tap.

**Architecture:** Frontend-only; no API or storage change. Conversion is a
body-Markdown transformation that sits with the existing rich-editor
compatibility check (`richEditorKeepsBody`) and is judged by the same check: its
result must be kept by the editor. The body is replaced through the existing
in-place composition so the frontmatter stays as written, consistent with
ADR 0004 (stored note Markdown is authoritative; frontmatter kept valid). The
rule is general, not per construct: keep what the editor keeps, otherwise keep
the editor's formatting only when it retains the text, otherwise fall back to
the readable text with its line and block breaks.

- **Value / learning:** Restore an immediate path from captured content to
  everyday note editing and refinement.
- **Effort hypothesis:** M (1–2 hours); the text-keeping conversion rule is
  the main work, the button and layout are small.
- **Depends on:** No blocking story identified.
- **Safe stopping point:** One explicit conversion action makes affected notes
  editable; the result is useful independently of other editor improvements.

## Refinement Input

Use a representative affected note to settle the supported conversion results
and any unavoidable formatting tradeoffs before implementation planning.

## When to Surface

First in the product backlog, as explicitly requested by the owner.

## Breadcrumbs

- Owner instruction, 2026-10-23: replace the Markdown-mode instruction beside
  the rich-editor compatibility warning with one conversion button, ensure a
  reasonable UI, and add the story at the top on main, synced with origin.
