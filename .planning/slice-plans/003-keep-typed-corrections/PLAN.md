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
| The rich editor loses the caret when the passage lands | Slice 7 | Throwaway mounted probe (deleted) in this harness on the raw Quill instance: caret placed after "for" reads `{ index: 41, length: 0 }`; after release, the passage is in the editor and `getSelection()` reads `{ index: 0, length: 0 }` (`syncQuillFromModel` sets `root.innerHTML` with no restore) | Confirmed red |
| Quill's selection API works in the mounted spec on the instance `QuillEditor` holds | Slices 6, 7 | Same probe. Through `richQuillInstance` (the `ref<Quill>` value, a Vue reactive proxy), `getSelection`, `setSelection`, `deleteText` and `insertText` all throw `Cannot read properties of null (reading 'offset')`, even before any model sync; through `toRaw(...)` every call works. Parchment's `ScrollBlot.find` checks `blot.scroll === this`, and `this` is the proxy | Fails as held today; works on the raw instance. Slice 6 holds it raw. `QuillEditor`'s own `setSelectionSilently` goes through the same proxy and its `try/catch` hides the throw |
| The Markdown editor loses the caret when the passage lands | Slice 5 | Same probe, Markdown mode: textarea focused, caret set after "for" (41), release → passage at the end, textarea still focused, `selectionStart/End` = 114/114 (the end) | Confirmed red; `TextArea.vue` binds `:value` with no restore |
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
| Caret and selection stay where they were, in both editors | 2 Fix a misheard word (caret) | 5, 7 | New spec cases: caret placed right after "for" in a focused editor; after the passage lands, the rich editor's `getSelection()` index and the textarea's `selectionStart/End` are unchanged |
| Documentation describes the new behavior | Scope | 2 | `docs/voice-input.md` no longer says the append ignores unsaved drafts |

No e2e change. The mocked live-audio feature returns results immediately and
cannot type while a request is pending, and adding delay machinery would only
repeat the mounted proof. Local gates: the focused specs above and the frontend
typecheck required by the `frontend` skill
(`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`).
No API change, so no client regeneration.

## Slices

Focused command for every slice (add `tests/components/form` in slices 5
to 7, whose editors are shared):
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
Status: done
Proof: new spec cases for example 3.
Accepted: two Markdown cases in `NoteAudioTools.typingWhilePending.spec.ts`
passed first time on slice 2's rule (no source change); each asserts the exact
final saved body. Focused command → 19 files, 102 tests; `vue-tsc` clean.

Behavior: (a) typing autosaved while `audioToText` is held, then release;
(b) a typing save held with `holdNoteContentSave` while the passage arrives,
then released → final saved content has the sentence then the passage, once.
Expected to pass on slice 2's rule; fix the autosave hand-off only if a case
fails.

### 4. A passage for a note the author has left goes to its saved body
Type: Behavior
Status: done
Proof: new spec case for example 4; existing `NoteAudioTools.preservation.spec.ts`
stays green.

Behavior: type into note A, switch the same editor to note B with `setProps`,
release → A's saved body gains the passage; B's editor shows no passage and
B gets no content save.
Accepted: mounted case "adds the passage to the saved body of a
note the author has left" (types into A, blurs as a navigation click does,
switches the same editor to B) → A's last save and store content are typing +
passage, B shows no passage and gets no save; focused command → 20 files, 114
tests; `vue-tsc` clean. The harness moved to
`noteAudioToolsTypingTestSupport.ts` for slices 5–6.

Remaining order: the Markdown caret goes first because it is independent and
its red case is already observed. The rich caret needs a preparation step
(slice 6) before its proof can read the caret at all.

Attempt 1 at the rich caret (2026-10-03, about 6 min, reverted; it was then
numbered slice 5): the planned case (caret placed with `quill.setSelection`
after "for", read back with `getSelection()`) threw `Cannot read properties of
null (reading 'offset')` in `normalizedToRange`. Thrash point: it was read as
Parchment's blot lookup being empty after `root.innerHTML` is set. Follow-up
probe: the throw happens on every selection call made through the reactive
proxy, before any sync, and none on the raw instance (premise table). False
sizing assumption: the instance `QuillEditor` exposes could be driven through
Quill's selection API. Existing specs that stub `getSelection` (for example
`dispatchRichPaste`) worked around the same proxy. Baseline before the
attempt: focused command plus `tests/components/form` → 54 files, 408 tests.

### 5. The Markdown editor keeps the caret
Type: Behavior
Status: done
Accepted: case "keeps the Markdown editor's caret where the author was
typing" failed first with `[114, 114]`, then keeps `[41, 41]`; `TextArea`'s
modelValue watch restores the selection when a focused textarea receives an
outside value. Focused command plus `tests/components/form` → 54 files, 409
tests; `tests/components/conversation tests/notes/NoteAddQuestion` → 7 files,
39 tests; `vue-tsc` exit 0.
Proof: new case in `NoteAudioTools.typingWhilePending.spec.ts`, Markdown
mode: focus the textarea, change "from" to "for" with `setTextareaValue`
while `audioToText` is held, put the caret right after "for" with
`setSelectionRange`, release → the textarea ends with the passage and its
`selectionStart/End` are unchanged. Observed red: they move to the end.

`TextArea` keeps `selectionStart/End` when it receives a new value while it
has focus (read them before Vue patches `value`, set them back after).
Appending at the end keeps earlier offsets valid.

### 6. The rich editor holds its Quill instance raw
Type: Structure
Status: done
Accepted: `QuillEditor` holds Quill in a `shallowRef`; the refactor removed the
proxy-error workarounds (try/catch in `setSelectionSilently` and
`interceptRichPaste`, `getSelection` stubs, `clearNativeSelectionForQuillMutation`),
so paste specs set a real Quill selection. Focused command plus
`tests/components/form` → 54 files, 409 tests; other QuillEditor consumers →
47 files, 235 tests; `vue-tsc` exit 0.
Proof: focused command plus `tests/components/form` stay green (baseline 54
files, 408 tests); `vue-tsc` clean.

`QuillEditor` keeps its Quill instance in a `shallowRef` (not a deep `ref`),
so its own calls and `richQuillInstance` in specs reach the raw instance and
Quill's selection API works. Enables slice 7. Removing the specs' now-needless
`getSelection` stubs or the `setSelectionSilently` swallow is not part of this
slice; the post-change refactor may judge it.

### 7. The rich editor keeps the caret
Type: Behavior
Status: planned
Proof: new case in `NoteAudioTools.typingWhilePending.spec.ts`, rich mode,
real Quill selection (no stub): while `audioToText` is held, replace "from"
with "for" through `deleteText`/`insertText` (source `user`) and
`setSelection` right after "for" (source `user`, which focuses the editor);
release → the editor text ends with the passage and `getSelection()` equals
the placed caret. Observed red on the raw instance: it reads index 0.

In `syncQuillFromModel`, when Quill holds a selection, read it before setting
`root.innerHTML`, bring Quill's blots up to date (`quill.update` with the
silent source), then put the same index and length back silently. Do it
synchronously, before the browser's selection-change event, so the editor
does not report a blur that would flush mid-change. If the case fails because
the restore lands before the blots match, stop and record it rather than
switching to Quill's content API in the same slice.

## Current decisions

- An open editor's draft owns the note's in-progress body. A passage for that
  note becomes an edit of the draft, not a separate save pushed into the
  editor. The saved-body append stays only for a note with no open editor.
- The registry follows the note the editor displays, not the note last typed
  into, because the note page reuses one editor instance across notes.

## Learnings

- CI repair (run 37124860341, `record_live_audio.feature` under mocked
  browser time): a passage joining the open draft waited for the autosave
  debounce. The editor's `appendToDraft` now flushes right away; mounted case
  "saves the passage as soon as it joins the open editor's draft" failed first.
- Switching the same editor instance to another note without a blur drops
  unsaved typing still inside the debounce (draft replaced by the new note's
  content). Pre-existing and outside this story; a navigation click blurs first.
- A passage arriving while removal or image upload has closed admission is
  ignored by the editor's `onUpdate` and not written elsewhere — the
  save-then-change overlap the plan leaves unaddressed.

- Run local frontend tests with `NODE_ENV` unset; this shell sets
  `NODE_ENV=production`, which hides `<script setup>` bindings from
  `wrapper.vm` in browser-mode tests.
