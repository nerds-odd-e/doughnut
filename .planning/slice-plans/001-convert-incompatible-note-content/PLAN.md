# Convert incompatible note content for the rich editor

## Source

- Story: [Make incompatible note content editable with one conversion button](../../seeds/SEED-083-rich-editor-content-compatibility.md#story-1)
- **Identity:** SEED-083#story-1

## Goal and scope

A note author whose body opens read-only with "This note has content the rich
editor cannot keep." activates one button in that warning; the body is replaced
by a converted body the rich editor keeps, with all readable text retained,
saved through the ordinary content-edit path (so note undo restores it), and
the editor becomes editable in place. The "Switch to Markdown mode to edit it."
hint is deleted. The warning fits narrow screens.

Exclusions (from the seed): improving the rich editor to keep more constructs;
a preview or per-construct choice; the frontmatter-error warning and its hint
stay as they are. Frontend only; no API change.

## Outside-in proof

| Key example | Observable signal |
| --- | --- |
| details / strike / hard break / `**a** *b*` / task list / adjacent code blocks | `RichMarkdownEditor.bodyItCannotKeep.spec.ts`: after clicking the button, the emitted Markdown equals the expected converted body, the warning is gone, and Quill is editable |
| Frontmatter kept as written | same spec: a body with frontmatter converts only the body |
| Converted body is saved; reopening stays editable | `NoteEditableContent.debouncedSave.spec.ts`: clicking the button calls `updateNoteContent` with the converted content; mounting that saved content shows no warning and an editable editor |
| Undo restores the original | existing content-edit undo (`note_edit.feature`, `NoteUndoButton`); no new proof, the conversion uses that path |
| Narrow screen | narrow-viewport spec (`atViewport(320)`, `interactiveControls`, `rect`): message, consequence line, and button within the window, the button with a comfortable tap size |

## Ordered slices

### 1. Converting a body the editor cannot keep keeps its text and makes it editable
Type: Behavior
Status: done
Proof: `RichMarkdownEditor.bodyItCannotKeep.spec.ts` (replace the "Switch to
Markdown mode" assertion with the button and conversion examples) and one
`NoteEditableContent` save case; run with
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts tests/notes/NoteEditableContent.debouncedSave.spec.ts`.

Behavior: an editable note whose body has `<details>`, `~~strike~~`, a hard
line break, `**a** *b*`, or a task list shows the warning with a short
consequence line and a conversion button → the author clicks it → the emitted
content keeps the frontmatter as written and the body becomes (observed in
refinement) `S`/`Inner` as paragraphs, `strike` as plain text, `line one` and
`line two` as separate paragraphs, `a b`, `- todo\n- done`; the warning is
gone, the editor is editable, and the content is saved at once (emit `blur`
after the update, as property changes do). The Markdown-mode hint for this
warning and its assertion are deleted.

Rule (general, beside `richEditorKeepsBody`): for each top-level Markdown block,
keep it when the editor keeps it; otherwise use the editor's held Markdown for
that block when its readable text (words, with block and line breaks as
separators) is unchanged; otherwise use the block's readable lines as plain
paragraphs.

Accepted proof (slice 1): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts tests/notes/NoteEditableContent.debouncedSave.spec.ts tests/components/form/RichMarkdownEditor.frontmatter.spec.ts tests/components/form/RichMarkdownEditor.listProperties.spec.ts` → 59 passed; `vue-tsc --noEmit` clean. Observations: "converting %j keeps its text and makes it editable", "converting keeps the frontmatter as written", "saves a converted body at once and reopens it editable". Converted outputs matched the expectations above exactly. Conversion lives in `bodyTheRichEditorKeeps` (`richEditorKeepsBody.ts`); the warning is `RichEditingUnavailableWarning.vue`. Sweep reading: "Switch to Markdown mode to edit it" matches only planning files.

### 2. Converted blocks that merge in the editor stay editable
Type: Behavior
Status: done
Proof: same spec, adjacent fenced code blocks (one tagged `ts`) example.

Behavior: a body of two adjacent fenced code blocks → convert → the result is
one code block holding both code texts and the editor is editable (each block
alone is kept, but the editor merges adjacent code blocks, so the joined
per-block result is not kept). Rule extension: when the joined result is not
kept, use the editor's held Markdown of the joined result if it keeps the
readable text, else its readable lines.

Accepted proof (slice 2): same four-spec command as slice 1 → 60 passed. Observation: "converting %j keeps its text and makes it editable" row for two adjacent fenced code blocks (one `ts`) → "```\nconst a = 1\nplain\n```" (one untagged code block), blur once, warning gone, editor editable. `bodyTheRichEditorKeeps` applies the same rule (`asTheEditorKeeps`) per block and then to the joined result.

### 3. The warning and button fit the existing UI on narrow screens
Type: Behavior
Status: done
Proof: a narrow-viewport case in the bodyItCannotKeep spec (or a sibling
`...Narrow.spec.ts` following `propertyValueDialogNarrow`): at 320px the
message, consequence line, and button lie within the window, the button is not
clipped and has a comfortable tap height.

Behavior: the warning shows message, then consequence line, then the action,
with spacing consistent with existing alerts; at phone width the button wraps
below the text instead of overlapping or clipping.

Accepted proof (slice 3): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts tests/components/form/RichMarkdownEditor.bodyItCannotKeepNarrow.touch.spec.ts tests/notes/NoteEditableContent.debouncedSave.spec.ts tests/components/form/RichMarkdownEditor.frontmatter.spec.ts tests/components/form/RichMarkdownEditor.listProperties.spec.ts` → 61 passed; vue-tsc clean. Observation: "stacks message, consequence and a touch-sized button within the window" (chromium-touch, 320px): parts within the window, in vertical order, button not clipped, height ≥ 44. DaisyUI's alert lays children out in columns, so the warning wraps its parts in a vertical flex stack; the button uses the repo's `pointer-coarse:min-h-11` touch size. The frontmatter-error variant shares the stack with unchanged text.

## Current decisions

- Conversion has no confirmation step; recovery is the existing note undo.
- Conversion saves immediately (update then `blur`), like property changes.
- Proof stays at component level; no new E2E scenario. Persistence and undo of
  content edits are existing behavior of the path the conversion uses.

## Decisive premises

- **Per-block rule yields kept, text-preserving content.** Consuming operation:
  mounting the converted body in `RichMarkdownEditor` and the existing
  `richEditorKeepsBody` check. Observation (scratch spec in this workspace,
  reverted): per-block conversion via a detached Quill
  (`root.innerHTML = markdownToHtml(block); update(SILENT)`) produced kept,
  editable, text-equal bodies for details, task list, hard break, `**a** *b*`,
  and strike+quote; adjacent code blocks were not kept after joining → slice 2.
- **Whole-result held Markdown of adjacent code blocks is kept.** Observation
  (scratch, reverted): `"```\nconst a = 1\nplain\n```"` passed
  `richEditorKeepsBody` and retains the text.
- **Raw held content loses text** (why conversion is not "save what Quill
  shows"): scratch observation showed details → `<br>`, `~~strike~~` deleted,
  `**a** *b*` → `**a***b*`, hard break → `line oneline two`.
- **Content edits are saved and undoable** through `TextContentWrapper`
  (`field="edit content"`) → `noteStore.updateTextField` → `NoteUndoButton`
  "edit content"; the conversion emits `update:modelValue` on that same path.

## Plan concerns

Slice-plan refinement: not needed — three independent outcomes with separate
proof loops, each a small change in one component and its helper. No remaining
slice-specific concern identified in this review.

## Execution complete

Product advice: no change. The story's outcome is delivered as refined; the
deferred editor improvements (keeping task checkboxes, collapsible sections,
strikethrough, hard breaks and code-block languages without conversion) stay
deferred. Observed limitation, consistent with the story's block-level rule:
supported inline formatting that shares a block with unsupported formatting is
dropped with it (`a_b *c* ~~d~~` → `a\_b c d`); wiki links survive conversion
as written. Worth considering only if owners report losing emphasis after
converting.
