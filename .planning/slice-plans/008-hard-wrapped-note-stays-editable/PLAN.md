# A hard-wrapped note stays editable in the rich editor

## Source

- Story: [SEED-046#story-12](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-12)
- **Identity:** SEED-046#story-12
- Correction of SEED-046#story-10 (plan
  [005](../005-rich-editor-keeps-content/PLAN.md)); reviewed commits
  `c5338213d0` and `0e36045508` on `exec/005-rich-editor-keeps-content`.

## Current findings

Execution retrospective, 2026-09-26, probing the real `RichMarkdownEditor`
(browser mode, real Quill) at `0e36045508`:

- `line one\nline two` (a paragraph wrapped across two lines) opens read-only
  with "This note has content the rich editor cannot keep."
- `- item that wraps\n  onto a second line\n- two` opens read-only.
- The editor saves both as the same words on one line, which renders the same.
  `richEditorKeepsBody` compares the `marked` HTML strings and normalizes only
  newline whitespace *between* tags, so a newline inside text (`line one\nline
  two`) differs from the saved space (`line one line two`).
- Story-10 requires a body whose round trip changes only style to stay
  editable; its safe stopping point says notes the editor can carry edit
  exactly as before. Wrapped text is the common shape of locally authored
  notes, so the defect locks many of the notes the story protects.

## Preserved promises and constraints

- Every body story-10 refuses stays refused, including example 3's hard break
  (`line one␠␠\nline two`) and `**a** *b*`, and the two fenced code blocks.
- No per-construct rules: the check still compares renderings.
- Code keeps exact whitespace: a change of whitespace inside `<pre>` is still a
  change of meaning.
- Out of scope: loose lists the editor saves tight (story-10 plan decision;
  still refused).

## Outside-in proof

`frontend/tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts`
through `createRichMarkdownEditorTestHarness`:

| Example | Expectation |
| --- | --- |
| `line one\nline two` | editable; typed word emitted |
| `- item that wraps\n  onto a second line\n- two` | editable; typed word emitted |
| existing refused bodies | unchanged: read-only with the warning |

Command:
`CURSOR_DEV=true nix develop -c pnpm frontend:test RichMarkdownEditor NoteEditableContent NoteTextContent`
plus `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.

## Slices

### 1. Wrapped text compares as rendered

Type: Behavior
Status: done — red run: both wrapped cases failed at `quillReadonly()`; after
the fix the command above passed 20 files / 158 tests and `vue-tsc --noEmit`
exited 0. `rendered` collapses whitespace runs to one space outside `<pre>`
after removing newline whitespace between tags.
Proof: add the two wrapped bodies to the spec's editable cases (red first:
both fail at `0e36045508`); the existing refused cases stay green; then the
command above.

Change: in `richEditorKeepsBody.ts`, normalize runs of whitespace in text to
one space outside `<pre>` before comparing, so the comparison follows how HTML
renders. Keep it one normalization, not a rule per construct.

## Execution complete

Product advice: no change beyond plan 005's advice — measure how many real
notes still refuse (loose lists saved tight) before deciding on a story.

## Current decisions

- Retrospective, 2026-09-26: correct by normalizing the rendered comparison,
  not by changing what the editor saves (story-5 owns save style).

## Learnings

None yet.
