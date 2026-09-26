# The rich editor never silently loses content

## Source

- Story: [SEED-046#story-10](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-10)
- **Identity:** SEED-046#story-10

## Goal and scope

When the rich editor opens a note whose body it cannot carry through an edit
unchanged in meaning, it shows the note read-only with a warning to switch to
Markdown mode. Meaning is judged by rendering: the body the editor would save
must render the same as the authored body. A body whose round trip changes only
style stays editable.

Excluded (seed): supporting more constructs in the rich editor; frontmatter
(story-5); restoring content lost earlier; switching the page to Markdown mode
automatically; content pasted during an edit. Preserved: Markdown mode saves
text as typed; the read-only warning for frontmatter the editor cannot parse;
how the rich editor displays a note.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 raw HTML `<details>` body → read-only, warning | slice 2, new `RichMarkdownEditor.bodyItCannotKeep.spec.ts` |
| 2 task list → read-only, warning | slice 2, same |
| 3 hard line break; `**a** *b*` → read-only, warning | slice 2, same |
| 4 two fenced code blocks, one tagged `ts` → read-only, warning | slice 2, same |
| 5 `# Heading`, `- item` bullets, a paragraph with a link → editable; a typed word is emitted | slice 2, same |
| 6 Markdown mode → saves text as typed | unchanged; existing `NoteEditableContent` tests stay green |

Command:
`CURSOR_DEV=true nix develop -c pnpm frontend:test RichMarkdownEditor NoteEditableContent NoteTextContent`

The spec mounts the real `RichMarkdownEditor` through
`createRichMarkdownEditorTestHarness` (browser mode, real Quill), as
`RichMarkdownEditor.frontmatter.spec.ts` does for unparseable frontmatter, and
observes `quillReadonly()`, the warning text, and for example 5 the emitted
Markdown after `quillInstance().insertText(…, "user")`.

## Slices

### 1. One reason explains why rich editing is unavailable

Type: Structure
Status: planned
Proof: `RichMarkdownEditor.frontmatter.spec.ts` stays green unchanged —
unparseable frontmatter still shows its message plus "Switch to Markdown mode
to fix the frontmatter." and the editor is read-only.

Change: in `RichMarkdownEditor.vue`, replace `frontmatterParseErrorMessage`
with one computed reason (message and hint, or none) that drives both the
warning block and `effectiveReadonly`; unparseable frontmatter is its only
source for now. Enables slice 2 to add the body reason without a second banner
or a second read-only rule.

### 2. A body the rich editor cannot keep opens read-only with the warning

Type: Behavior
Status: planned
Proof: new `RichMarkdownEditor.bodyItCannotKeep.spec.ts` — examples 1–4 as one
parametrized case each asserting read-only and a warning that names Markdown
mode; example 5 asserting editable and that the emitted Markdown contains the
typed word. Then the command above runs green; adjust an existing test's
fixture only when that test is not about the body it now refuses.

Behavior: pre-condition — a note body holding a construct the rich editor
drops or changes in meaning; trigger — the note opens in rich mode; result —
the editor is read-only and the warning says the note has content the rich
editor cannot keep and to switch to Markdown mode to edit it. A body with only
style differences opens editable exactly as today.

Change (in `frontend/src/components/form/`):

- One pure check, beside `markdownizer.ts`: the authored body and the Markdown
  the editor would save from the HTML Quill holds after loading it are both
  rendered through `markdownToQuillHtml` (the conversion the editor loads
  with) and compared, normalizing only whitespace between tags; no
  per-construct rules.
- `RichMarkdownEditor.vue` evaluates it when `markdownForRichDisplay` changes
  from outside (not on its own emissions), reading the HTML the loaded Quill
  holds, because Quill itself drops some constructs `marked` produced; the
  result becomes the second source of slice 1's reason.

## Current decisions

- Refinement, 2026-09-26: the fallback is read-only plus a warning, following
  the existing unparseable-frontmatter precedent in `RichMarkdownEditor.vue`,
  not an automatic switch to Markdown mode (`asMarkdown` in `NoteShow.vue`
  stays the owner's toggle).
- Planning, 2026-09-26: meaning is compared on rendered HTML, so style-only
  changes (setext vs `#`, `*` vs `-`, `_` vs `*`, list numbering, blank lines,
  reference links inlined) pass without special cases. A loose list the editor
  saves tight renders differently (`<li><p>`) and will be read-only; accept
  that unless it proves common, and then record a learning rather than adding
  a special case in this slice.
- Planning, 2026-09-26: story-5 (style churn) and this story touch the same
  component but no shared rule; whichever lands second rebases.

## Learnings

None yet.
