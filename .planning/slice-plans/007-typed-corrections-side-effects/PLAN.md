# Fix two side effects of keeping typed corrections

**Identity:** SEED-066#typed-corrections-side-effects

## Source

Retrospective correction of
Keep typed corrections when voice results arrive (`.planning/seeds/SEED-066-voice-input.md#preserve-typed-corrections` at `34cfbd43`)
(plan `.planning/slice-plans/003-keep-typed-corrections/PLAN.md` at `34cfbd43`, reviewed 2026-10-03). Story home:
[Fix two side effects of keeping typed corrections](../../seeds/SEED-066-voice-input.md#typed-corrections-side-effects).

Provenance: reviewed commits on `claude/keep-typed-corrections-when-voice-results-arrive-2`
after `3ad53248de` (Take): `fc92e9fe` (registry follows the displayed note,
registers from mount), `e397a44b` (passage joins the open draft), `9d166e59`,
`d22ce6cf` (flush on append), `5a281c26`, `e1e8da43`, `6c1eec61` (Markdown
caret kept in `TextArea`), `3acf8647`, `c907ba60`.

## Findings (current evidence)

1. **Markdown paste caret regression** (from `6c1eec61`). `TextArea.vue`'s
   `modelValue` watch saves the selection when a focused textarea receives a
   different value and restores it after `await nextTick()`.
   `useNoteContentPaste` (Markdown HTML paste) calls `update(newValue)` and then
   places the caret after the pasted Markdown in its own `nextTick`; the
   `TextArea` restore is chained later and wins. Observed with a throwaway
   mounted probe on `NoteEditableContent` (Markdown, textarea focused,
   selection over "[SELECTED]" in `before [SELECTED] after`, paste
   `<p><b>Styled</b> text</p>`): value `before **Styled** text after`,
   selection `[7, 17]` (a wrong range is selected) instead of `[22, 22]`. With
   `TextArea.vue` from `3ad53248de` the same probe passes. The paste choice's
   `replace` uses the same `update` + `nextTick` caret placement while the
   textarea keeps focus, so it shares the mechanism (not separately observed).
   Existing paste specs assert values, not the caret, so the suite stayed
   green.
2. **Dictated passage dropped during a save-then-change pause** (from
   `fc92e9fe` + `e397a44b`). The body editor now registers from mount, so
   `appendToOpenNoteContentDraft` finds it whenever the note page is open and
   returns `true`. While image upload or note removal has closed admission,
   `appendToDraft` calls `onUpdate`, which returns early, and `flush()` saves
   nothing, so the passage is neither in the draft nor saved. Observed with a
   throwaway probe on the `useBodyEditorWithHeldDictation` harness: close
   admission with `closeAndFlushNoteContentMutations` while `audioToText` is
   held, release, reopen → no `updateNoteContent` call at all. Before the
   story, `appendDictatedText` always wrote `content + passage` through
   `updateTextField` (no admission check). Plan 003 recorded this overlap as
   "this story adds nothing"; the delivered behavior turned a race into a
   certain silent loss.

## Preserved promises and constraints

- All promises of plan 003 stay: typing kept, passage at the end, saved once,
  caret kept in both editors, passage for a left note goes to its saved body.
  `NoteAudioTools.typingWhilePending.spec.ts` stays green unchanged.
- The save-then-change overlap stays unaddressed beyond restoring the
  pre-story saved-body write: no new waiting, queueing, or retry, and the race
  with the upload or removal request is not fixed.
- Failure handling follows ADR 0006.

## Outside-in proof

| Finding | Owning slice | Proof |
| --- | --- | --- |
| 1 | 1 | New case in `frontend/tests/notes/NoteEditableContent.paste.spec.ts` (Markdown, focused textarea, selection over "[SELECTED]", HTML paste) asserting `selectionStart/End` right after the pasted Markdown. Observed red today (`[7, 17]`). The Markdown caret case in `NoteAudioTools.typingWhilePending.spec.ts` stays green |
| 2 | 2 | New case in `frontend/tests/notes/NoteAudioTools.typingWhilePending.spec.ts`: open editor, `closeAndFlushNoteContentMutations(note.id)` while `audioToText` is held, release → the last `updateNoteContent` body is the original body plus the passage. Observed red today (no save) |

Local gates: the focused command below and
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
(`frontend` skill). No e2e or API change.

Focused command:
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/notes/NoteEditableContent tests/notes/NoteTextContent tests/components/notes/NoteTextContentUndo tests/pages/NoteShowPage.autosaveTrash tests/pages/NoteShowPage.imageUpload tests/components/form tests/components/conversation tests/notes/NoteAddQuestion`
(the last two cover the other `TextArea` users).

## Slices

### 1. A Markdown paste leaves the caret after the pasted text
Type: Behavior
Status: planned
Proof: the new paste case fails first with `[7, 17]`, then passes; focused
command green; `vue-tsc` clean.

`TextArea` keeps the caret for an outside value, but a caller that places the
caret after its own change must win. Suggested approach: capture the
selection before Vue patches `value` and restore it in the same render flush
(a post-flush step), so callers' own `nextTick` placement runs afterwards.
If the paste choice's `replace` turns out to share the symptom, add one
assertion for it in the same spec file.

### 2. A passage that arrives during a save-then-change pause goes to the saved body
Type: Behavior
Status: planned
Proof: the new mounted case fails first with no save, then passes; focused
command green; `vue-tsc` clean.

`appendToOpenNoteContentDraft` hands the passage to the open editor only while
admission is open; otherwise it returns `false`, so `appendDictatedText` keeps
the saved-body append it had before the story.

## Current decisions

- Restoring the pre-story saved-body write is the whole of slice 2; the
  overlap itself stays as plan 003 left it.
