# Keep typed corrections when voice results arrive

**Identity:** SEED-066#preserve-typed-corrections

## Source

[Keep typed corrections when voice results arrive](../../seeds/SEED-066-voice-input.md#preserve-typed-corrections),
refined on 2026-10-03. Owner decisions: join the typing instead of pausing the
editor to save first, and keep the caret where the author was typing as part
of this story.

## Goal and scope

A note author keeps typing in a note while their speech is still being
processed. When the dictated passage arrives, both their typing and the passage
are kept, and the caret stays where they were typing.

- **Included:** When a body editor for the originating note is open, the
  passage is added to the end of that editor's draft, including unsaved
  typing, and saved by its ordinary autosave. Typing that is already saved, or
  is still being saved, is kept, and the passage follows it once. The caret and
  selection stay put in both the rich and Markdown editors. With no open body
  editor for that note, the passage is added to the saved body as today. Update
  `docs/voice-input.md`.
- **Excluded:** Spacing between late typing and the passage (the model's
  "previous content" excerpt is taken when the request starts). Undoing only
  the passage. Several authors editing at once. New recovery when saving the
  typing fails (the existing editor error shows; ADR 0006).
- **Not addressed, considered:** A passage that arrives while a save-then-change
  pause (note removal, image upload) has closed the editor. This story adds
  nothing for that overlap. The current saved-body path has the same race.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The arriving passage is written from store content, not from the editor draft | Slice 2 | Read `frontend/src/store/noteTextEditing.ts` `appendDictatedText`: `(realm.note.content ?? "") + value.dictatedText` through `updateTextField` | Confirmed |
| The store write then discards the unsaved draft | Slice 2 | Read `useDebouncedTextAutosave.ts` `syncFromExternal`: with an unsaved draft and no save in flight, a new external value that differs from both draft and last saved calls `replaceFromExternal`, which replaces the draft | Confirmed by reading. Slice 2's new spec must fail on unchanged code before the fix (symptom reproduced; the [recorded journey](../../../docs/voice-input.md#typing-while-audio-processing-is-pending) is the earlier field observation) |
| The per-note body-editor registry exists and holds `flushAndWait` | Slice 1 | Read `frontend/src/composables/noteContentMutationBarrier.ts`; only `TextContentWrapper.vue` registers; `useNoteRemovalFlow.ts` and `RichFrontmatterImagePropertyValue.vue` call `closeAndFlushNoteContentMutations` | Confirmed |
| The registry follows the note last typed into, not the note displayed | Slice 1 | Read `TextContentWrapper.vue`: `registerContentAutosave(noteId)` runs only from `onUpdate`. `NoteShowPage.vue`, `NoteShow.vue`, `NoteTextContent.vue` and the `RouterView` in `NotebookSidebarLayout.vue` have no `:key`, so the same editor instance is reused when the author opens another note | Confirmed. Handing a passage to this registration as-is could put note A's passage into note B's draft and save it to A. Slice 1 registers by the displayed note instead |
| The rich editor loses the caret when the model changes | Slice 5 | Read `QuillEditor.vue` `syncQuillFromModel`: sets `quill.root.innerHTML` with no selection restore | Confirmed |
| The Markdown editor binds `:value` on a native textarea | Slice 6 | Read `TextArea.vue` | Binding confirmed. Whether a programmatic value change moves the caret in the Chromium browser-mode specs was not observed; slice 6 writes its Markdown caret case first and adds the restore only if that case fails |
| Existing Quill caret placement can be reused | Slice 5 | Read `QuillEditor.vue` `setSelectionSilently` | Confirmed |
| Focused frontend specs run locally on this revision | All slices | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/notes/NoteEditableContent` | 11 files, 76 tests passed |
| The save-then-change pause is covered by page-level specs | Slice 1 | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NoteShowPage.autosaveTrash tests/pages/NoteShowPage.imageUpload tests/notes/NoteTextContent tests/components/notes/NoteTextContentUndo` | 6 files, 19 tests passed |

## Outside-in proof

The stable boundary is a mounted browser-mode spec that renders the real body
editor (`NoteTextContent`, editable) and the real `NoteAudioTools` for the same
note, sharing the real store and registry. Note content saves and
`audioToText` are mocked at the SDK, and `audioToText` is held so the test can
type while the request is pending. Existing helpers:
`noteAudioToolsTestSupport.ts` (`mountNoteAudioTools`, `processAudio`,
`dictatedTextResponse`), `noteTextContentTestSupport.ts`
(`mountNoteTextContent`, `holdNoteContentSave`) and
`noteEditableContentTestSupport.ts` (`richQuillInstance`, `setTextareaValue`).
Place the new spec beside the audio specs, for example
`frontend/tests/notes/NoteAudioTools.typingWhilePending.spec.ts`.

"After reload" in the examples is observed as the last `updateNoteContent`
request body and the store's note content, which is what reload reads.

| Promise | Example | Owning slice | Proof |
| --- | --- | --- | --- |
| Unsaved typing stays and the passage follows it, saved together | 1 Red bicycle (rich editor) | 2 | New spec: type the red-bicycle sentence at the end while `audioToText` is held, release it. Editor shows typing then passage; saved content is originals + lighthouse + red bicycle + passage, each once. Must fail before the fix |
| The same in the Markdown editor | 1 (Markdown) | 2 | Same spec, Markdown mode, typing through the textarea |
| Typing already saved, or with its save still in flight, is kept; the passage follows once | 3 Edit already saved | 3 | New spec: (a) typing autosaved before release; (b) save held with `holdNoteContentSave` while the passage arrives, then released. Final saved content has the sentence then the passage once |
| With the author on another note, the passage goes to the originating note's saved body and the other note's editor is unchanged | 4 Moved to another note | 1, 4 | New spec: type into note A, switch the same editor to note B with `setProps`, then release. A's saved body gains the passage; B's editor and saves are untouched. Existing `NoteAudioTools.preservation.spec.ts` (no editor open) stays green |
| An edit in the middle stays, the passage goes to the end | 2 Fix a misheard word (content) | 2 | New spec: change "from" to "for" mid-body while held; after release "for my sister" stays and the passage is at the end |
| Typing is never blocked while a result is pending or arriving | Scope | 2 | Covered by the typing steps above succeeding while `audioToText` is held |
| Caret and selection stay where they were, in both editors | 2 Fix a misheard word (caret) | 5, 6 | New spec cases: caret placed right after "for" in a focused editor; after the passage lands, the rich editor's `getSelection()` index and the textarea's `selectionStart/End` are unchanged |
| Documentation describes the new behavior | Scope | 2 | `docs/voice-input.md` no longer says the append ignores unsaved drafts |

No e2e change. The mocked live-audio feature returns results immediately and
cannot type while a request is pending, and adding delay machinery would only
repeat the mounted proof. Local gates: the focused specs above and the frontend
typecheck required by the `frontend` skill
(`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`).
No API change, so no client regeneration.

## Slices

Focused command for every slice (add `tests/components/form` in slices 5
and 6, whose editors are shared):
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteAudioTools tests/notes/NoteEditableContent tests/notes/NoteTextContent tests/components/notes/NoteTextContentUndo tests/pages/NoteShowPage.autosaveTrash tests/pages/NoteShowPage.imageUpload`

### 1. The body-editor registration follows the displayed note
Type: Structure
Status: done
Proof: existing focused specs stay green, including
`NoteShowPage.autosaveTrash` and `NoteShowPage.imageUpload`, which pause the
registered draft and flush it before changing the note.
Accepted: the focused command plus `tests/notes/TextContentWrapper
tests/notes/NoteEditableTitle` → 18 files, 97 tests; `vue-tsc` clean.
`TextContentWrapper` takes a required `noteId` (the former `titleEditNoteId`
merged into it); the slot's `update` takes only the new value. The registry now
holds a registration from mount, so removal and image upload pause even an
untouched editor.

`TextContentWrapper` registers its content autosave in
`noteContentMutationBarrier.ts` for the note it displays, from mount and
whenever that note changes, instead of on first typing. It takes the
displayed note's id from `NoteEditableContent`. No visible change. Enables
slice 2, which hands a passage to this registration and must never reach a
draft showing another note.

### 2. Unsaved typing stays and the passage follows it
Type: Behavior
Status: done
Proof: new spec (example 1, rich and Markdown; content half of example 2),
failing first on unchanged code with the typed sentence missing.
Accepted: `NoteAudioTools.typingWhilePending.spec.ts` (3 cases, all failed
before the fix with the typing missing) asserts editor text and the exact last
`updateNoteContent` body; focused command → 19 files, 100 tests;
`tests/store/noteStore.spec.ts` 10 tests; `vue-tsc` clean. The registry entry
is `OpenNoteContentEditor` (`registerOpenNoteContentEditor`, `appendToDraft`);
`appendToOpenNoteContentDraft` routes the passage. The spec's harness renders
the editor from the store realm; slices 3–6 reuse it.

Behavior: an open body editor with unsaved typing (at the end, or "from"
changed to "for" mid-body) and a held `audioToText` → release → the editor
shows the typing with the passage at the end, and the saved content is the
originals, lighthouse addition, typing and passage, each once.

The registration gains "append to draft": propose the current draft plus the
passage, so ordinary autosave and undo save it. `appendDictatedText` hands the
passage to the note's registration when there is one, and otherwise keeps
today's saved-body append. Update `docs/voice-input.md`: the "unsaved editor
drafts have separate behavior" sentence and the "Visible typing can be lost"
section describe the kept typing.

### 3. Typing already saved or still saving is kept
Type: Behavior
Status: planned
Proof: new spec cases for example 3.

Behavior: (a) typing autosaved while `audioToText` is held, then release;
(b) a typing save held with `holdNoteContentSave` while the passage arrives,
then released → final saved content has the sentence then the passage, once.
Expected to pass on slice 2's rule; fix the autosave hand-off only if a case
fails.

### 4. A passage for a note the author has left goes to its saved body
Type: Behavior
Status: planned
Proof: new spec case for example 4; existing `NoteAudioTools.preservation.spec.ts`
stays green.

Behavior: type into note A, switch the same editor to note B with `setProps`,
release → A's saved body gains the passage; B's editor shows no passage and
B gets no content save.

### 5. The rich editor keeps the caret
Type: Behavior
Status: planned
Proof: new spec case: focused rich editor, caret right after "for", passage
lands → `getSelection()` index and length unchanged.

When `QuillEditor` applies a new model value while it holds a selection,
restore that selection with `setSelectionSilently`. Appending at the end keeps
earlier offsets valid.

### 6. The Markdown editor keeps the caret
Type: Behavior
Status: planned
Proof: new spec case: focused textarea, caret right after "for", passage
lands → `selectionStart/End` unchanged.

Write the case first. If it already passes, the slice is the case alone.
Otherwise `TextArea` restores `selectionStart/End` when it receives a new
value while focused.

## Current decisions

- An open editor's draft owns the note's in-progress body. A passage for that
  note becomes an edit of the draft, not a separate save pushed into the
  editor. The saved-body append stays only for a note with no open editor.
- The registry follows the note the editor displays, not the note last typed
  into, because the note page reuses one editor instance across notes.

## Learnings

- With an editor open, the passage is saved after the editor's debounce, so a
  next audio request starting within it builds its "previous content" excerpt
  without the previous passage (spacing only, excluded scope).
- A passage arriving while removal or image upload has closed admission is
  ignored by the editor's `onUpdate` and not written elsewhere — the
  save-then-change overlap the plan leaves unaddressed.

- Run local frontend tests with `NODE_ENV` unset; this shell sets
  `NODE_ENV=production`, which hides `<script setup>` bindings from
  `wrapper.vm` in browser-mode tests.
