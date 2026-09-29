# Blank-looking note body stays editable in the rich editor

## Source

- Story: [Hide the empty-content rich Markdown mismatch warning](../../seeds/SEED-058-silent-empty-markdown-mismatch.md#story-1)
- **Identity:** SEED-058#story-1

## Goal and scope

Learners opening a note whose body shows nothing in the rich editor can type
into it straight away, with no warning, and opening it alone does not change
the note.

- Included: a body that renders only to empty paragraphs, line breaks,
  non-breaking spaces, and whitespace counts as something the rich editor keeps,
  so there is no "cannot keep" warning and no read-only lock.
- Preserved: opening emits no content change; bodies with visible content the
  editor cannot keep still warn and open read-only; the invalid-frontmatter
  warning; Markdown mode.
- Excluded: repairing how the editor converts such markup; cleaning up stored
  blank-looking bodies.

## Current decisions

- The rule lives in the one existing "cannot keep" comparison,
  `frontend/src/components/form/richEditorKeepsBody.ts`: a body whose rendered
  HTML holds nothing visible (only `<p>`/`</p>`, `<br>` in any form, `&nbsp;`,
  and whitespace) is kept. `RichMarkdownEditor.vue` needs no change, because
  its warning and read-only lock both follow `bodyItCannotKeep`.
- Use an allowlist of blank markup, not "no text". Visible elements without
  text, such as images or rules, must still go through the existing
  comparison.
- The proof is component-level, through `RichMarkdownEditor`, which is where
  the existing "cannot keep" examples live. No E2E: the behavior is entirely
  inside this component, and its mount already uses real Quill in the browser.

## Decisive premises (observed 2026-09-29)

| Premise | Observation | Result |
| --- | --- | --- |
| Blank-looking bodies warn and lock today; truly empty ones do not | Temporary spec mounting `RichMarkdownEditor` via `richMarkdownEditorTestHarness`, run with `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/<spec>` | `&nbsp;`, `<p></p>`, `<br/>\n`: warning + read-only. `""`, `"\n"`, `"   "`, frontmatter only: neither |
| Warning and lock come from one check | Read `RichMarkdownEditor.vue`: `effectiveReadonly` includes `richEditingUnavailableReason`, which includes `bodyItCannotKeep` | Confirmed |
| Once unlocked, opening emits no change and typing saves the typed text | The same temporary spec with a trial blank rule in `richEditorKeepsBody` (reverted), waiting 300 ms after mount, then `insertText(0, "Hello", "user")` | Nothing emitted on open for all cases. Typing gave `Hello` for `&nbsp;` and `<p></p>`, `Hello<br>\n` for `<br/>\n`, and `---\ntype: note\n---\n\nHello\n` for frontmatter + `&nbsp;`. `<details>…` stayed warned and read-only |

## Outside-in proof

| Promise (seed key example) | Slice | Observation |
| --- | --- | --- |
| `&nbsp;` / `<p></p>` / `<br/>\n` body → open → no warning, editable, nothing emitted | 1 | New `it.each` in `RichMarkdownEditor.bodyItCannotKeep.spec.ts`: warning absent, `quillReadonly()` false, no `update:modelValue` emitted |
| Same body → type "Hello" → saved body is the typed text, invisible markup dropped | 1 | Same spec, `&nbsp;` case: after `insertText`, `lastEmittedMarkdown()` is `Hello` |
| `<details>` body → warning and read-only | 1 | Existing `opens read-only with the warning` cases stay green |
| Empty / frontmatter-only body unchanged | 1 | Existing editor specs stay green (focused command below) |

Focused command:
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts tests/components/form/RichMarkdownEditor.changesOnlyTheEdit.spec.ts tests/components/form/RichMarkdownEditor.spec.ts`

## Slices

### 1. A blank-looking body opens editable without the warning

Type: Behavior
Status: planned
Proof: the new and existing cases in `RichMarkdownEditor.bodyItCannotKeep.spec.ts`, plus the focused command above.

Behavior: a note body of `&nbsp;`, `<p></p>`, or `<br/>` and a newline → open it
in the rich editor → no "cannot keep" warning, the editor is editable, and no
content change is emitted. Typing "Hello" into the `&nbsp;` body saves `Hello`.
Bodies with visible content the editor cannot keep still warn and open
read-only.

Sizing: one small rule in `richEditorKeepsBody.ts`, an updated doc comment, and
one parameterized spec case. About 5–10 minutes.
