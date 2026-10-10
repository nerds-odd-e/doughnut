# Restore title styling and make spoken title editing unobtrusive

Work item: **SEED-066#unobtrusive-selection-aware-spoken-title**

**Source:** [story](../../seeds/SEED-066-voice-input.md#unobtrusive-selection-aware-spoken-title)

## Goal and scope

A note author sees the note title as the note's heading again, and speaks a
title through one small microphone button at the end of the title that puts
the heard words where their caret or selection is, on the note page and in
New note. The status line, the text-labelled Speak the title and Stop
buttons, and whole-title replacement on an existing note are gone without
trace.

Included: the five slices below. Each removal follows the story's removal
scope and principle 7 in `AGENTS.md`.

Excluded (see the story): a status line in any form, a retry control, a
keyboard shortcut, voice title entry from the sidebar or for folders, any
change to how the sidebar or note cards render titles, backend or generated
API changes, body dictation (the one-button story owns it).

Assumptions: the common error toast is `useToast().showErrorToast`; the
toolbar's pressed-toggle style (`daisy-btn-soft daisy-btn-primary`, ghost
otherwise, in `NoteMoreOptionsActions.vue`) is the listening highlight; the
`Mic` icon from `@lucide/vue` already used by the toolbar is the idle icon.

## Known architecture (PFE, read 2026-10-10 at `f6fa23a17e`)

- **Title rendering.** `NoteEditableTitle.vue` wraps `PathNameEditor`'s
  `#title` slot in `<h2 class="path-name-heading">` with a scoped rule
  `font-size: 1.5rem; font-weight: 400`. `NoteTextContent.vue` is its only
  user; `NoteShow.vue` and `NoteContextReader.vue` render that. Tailwind's
  preflight leaves headings unstyled, so the scoped rule is the whole look.
- **Title editor.** `SeamlessTextEditor.vue` is a single-text-node
  contenteditable. Its `onPaste` already computes the text before and after
  the current selection, builds the new text, emits `update:modelValue`,
  and puts the caret after the pasted text on the next tick. Private helpers
  `getCaretOffsetsInSingleTextChild` and
  `applyCaretOffsetsInSingleTextChild` exist. `PathNameEditor.vue` passes
  `seamlessBindings` through, sanitizes on `onModelUpdate`, and exposes
  `applyExternalValue` (used only by New note's spoken path).
- **Spoken title today.** `SpeakTitleControl.vue` owns a stop-only recorder,
  the phase table with the status wording, a `busy` model, and emits
  `heardSegments` once. `NoteEditableTitle.vue` calls `update(joinDictatedSegments("", segments))`
  (whole replacement) and refocuses the editor; `NoteNewForm.vue` calls
  `applyExternalValue(titleAfterSpokenSegments(...))` where
  `noteNewFormTitle.ts` joins onto the current title or replaces an untouched
  "Untitled". `joinDictatedSegments.ts` holds the CJK/space rule for joining
  a segment onto a base; the body passage path uses the same function.
- **Reference choice and autosave.** `TextContentWrapper.vue` proposes or,
  for a linked note, holds the draft and shows the reference panel; its
  buttons use `@mousedown.prevent` so clicking them keeps focus in the
  wrapper; `onReferencedTitleFocusOut` discards when focus leaves the
  wrapper root. The control renders inside that root, so it stays inside.
- **Proof entry points.** Mounted specs `NoteEditableTitle.spokenTitle.spec.ts`,
  `NoteNewForm.spokenTitle.spec.ts`, `NoteNewForm.spokenTitle.outcomes.spec.ts`
  through `spokenTitleTestSupport.ts` (button finders by text,
  `speakTitleStatus`, `speakAndStop`, hold-converting helper) with
  `noteAudioToolsMocks.ts`; `SeamlessTextEditor.spec.ts` with
  `seamlessTextEditorTestSupport.ts` (`setCaretInEditor`, `pasteClipboard`);
  `joinDictatedSegments.spec.ts`; toasts through
  `tests/helpers/toastTestSupport.ts`. Vitest runs these in a real Chromium
  (browser mode), so `Selection`, `Range` and `getComputedStyle` are the
  browser's. E2E `record_live_audio.feature` scenarios "Create a note by
  speaking the title", "Rename a note by speaking the title" and "Rename a
  linked note by speaking the title…" through `audio.ts` steps and
  `noteCreationForm.speakTheTitle` / `stopSpeakingTheTitle`, which click
  buttons by the names "Speak the title" and "Stop". North Star "One
  real-service audio test": no real-service scenario is added.
- **Documentation.** `docs/voice-input.md` sections "Speaking a title in New
  note" and "Speaking a title on an existing note" describe the status line,
  the join-or-replace rules and the button names; slices 3–5 rewrite them.
- **The body's voice button (landed on trunk at `b189b01af6`, read after
  rebasing onto it).** `NoteVoiceInputButton.vue` is bound to a note's body
  session through `useNoteVoiceInput(note)`, so the component itself is not
  reusable for a title. What this story reuses from it: the appearance
  conventions (a `Mic` icon idle, the pressed toggle classes while active, a
  `LoaderCircle` spinner with `disabled` while finishing), the classes in
  `noteToolbarButtonClasses.ts` (`toolbarToggleBtnClass`), the naming
  pattern of `noteVoiceInputTitles` in `noteMoreOptionsTitles.ts`, and
  `useToast().showErrorToast` with the same microphone-refused wording. The
  spoken-title specs now import `noteVoiceInputButtonMocks.ts` and
  `audioTextResponse` from `noteVoiceInputButtonTestSupport.ts`.

## Premises observed (2026-10-10, this worktree at `f6fa23a17e` plus the refined seed; rerun on 2026-10-11 after rebasing onto trunk `b189b01af6`, see the last item)

- Baseline:
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitle.spec.ts tests/notes/NoteNewForm.spokenTitle.spec.ts tests/notes/NoteNewForm.spokenTitle.outcomes.spec.ts tests/components/form/SeamlessTextEditor.spec.ts tests/components/notes/core/PathNameEditor.spec.ts tests/notes/TextContentWrapper.spec.ts tests/models`
  passes (15 files, 114 tests, in `|chromium|`);
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`
  passes (5 scenarios, 20 s, exit 0) on this worktree's isolated stack.
- Title look in the product: on the held isolated stack, a note page's title
  editor computed `24px` / `400`; a body paragraph `16px` / `400`; a body
  `##` heading `24px` / `700`. A scratch mounted spec of `NoteEditableTitle`
  read `getComputedStyle(h2)` as `24px` / `400` as well, so slice 1's proof
  can observe the scoped rule in the mounted harness (scratch spec deleted).
- Caret insertion in the harness: `SeamlessTextEditor.spec.ts` case "at
  cursor position, then appends when selection is cleared" passes, so
  `setCaretInEditor` plus a paste inserts at the caret and appends without
  a selection in this harness; slice 2 keeps that case as its proof.
- Selection after the click: with "Orchard" selected in the title, clicking
  Speak the title moved focus to the button and left the document selection
  in the title (offsets 0–7) in Chromium. Slice 2 does not rely on it: the
  editor remembers its own last selection.
- New note opens with "Untitled" focused and wholly selected (observed in
  the product); `settleScheduledAutofocus` settles that in mounted specs.
- Not observed: how the small button reads inline with the heading and
  inside the New note field. Visual judgment, checked by eye during slice 5
  on the held stack; the specs prove names, states and toasts, not the look.
- After the rebase onto trunk `b189b01af6`, which landed the one-button
  body story: the same frontend command passes (15 files, 113 tests) and the same
  E2E command passes (5 scenarios, 13 s, exit 0). The one-button story did not touch
  `SpeakTitleControl.vue`, `NoteEditableTitle.vue`, `NoteNewForm.vue`,
  `SeamlessTextEditor.vue`, `PathNameEditor.vue`, `joinDictatedSegments.ts`,
  the three title scenarios, `noteCreationForm.ts`, or the two title
  sections of `docs/voice-input.md`; it renamed the audio test mocks and
  support the spoken-title specs import.

## Outside-in proof

| Promise | Proof | Slice |
| --- | --- | --- |
| The title is bold and larger than a body section heading, in view and while editing | mounted `NoteEditableTitle` spec: computed font weight ≥ 700 and font size > 24 px for the heading, readonly and editable; by eye on the held stack | 1 |
| Caret-aware insertion is one operation of the title editor; paste unchanged | `SeamlessTextEditor.spec.ts` stays green; a case: `insertAtSelection` with a remembered selection after focus left the editor | 2 |
| Words replace a selection, insert at a caret with spacing on both sides, append at the end, append when there was no caret, replace the whole selected title; caret after the words; saved as a typed title | `NoteEditableTitle.spokenTitle.spec.ts` cases named in slice 3; `joinDictatedSegments.spec.ts` insertion cases; E2E "Rename a note by speaking the title" with select-all before speaking | 3 |
| Linked note: panel, save on choice, discard on leaving, as typed | existing spec "shows the reference panel for a spoken linked rename…" retargeted to a selection; E2E linked scenario | 3 |
| New note: untouched "Untitled" replaced even after the selection was lost; title pattern and typed title append at the caret; search and Submit as before | `NoteNewForm.spokenTitle.spec.ts` cases named in slice 4 | 4 |
| One small icon button with idle, listening and converting appearances and names; no status line; errors in toasts; nothing heard is silent; readers get no button; Submit and Enter rules kept | `NoteEditableTitle.spokenTitle.spec.ts`, `NoteNewForm.spokenTitle*.spec.ts` retargeted to the accessible names, `aria-pressed`, `disabled`, `toastShown`/`noToastShown`, no `[role="status"]`; E2E steps click by the new names; sweep reading | 5 |
| Documentation describes the behavior | the two title sections of `docs/voice-input.md` rewritten | 3, 4, 5 |

## Ordered slices

### 1. The note title reads as the heading
Type: Behavior
Status: done
Accepted proof: `frontend/tests/notes/NoteEditableTitle.heading.spec.ts`
(editable and readonly cases, computed weight ≥ 700 and size > 24 px) in the
full `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`
(330 files, 2082 tests) with `pnpm -C frontend exec vue-tsc --noEmit`; held
stack reading in Chromium: title 30 px / 700, also while focused, against a
body `##` heading at 24 px / 700. The rule is `1.875rem` / `700`.
Proof: a case in `NoteEditableTitle.spokenTitle.spec.ts` (or a new
`NoteEditableTitle.heading.spec.ts`) mounting an editable and a readonly
title and asserting `getComputedStyle` of the heading: font weight ≥ 700 and
font size greater than 24 px; a by-eye check on the held stack
(`pnpm e2e:hold`) that the title sits above a body `##` heading as the
page heading.

Behavior: a note is displayed or its title is edited → the title is bold and
larger than the body's section headings. Change the scoped rule in
`NoteEditableTitle.vue` (the implementer picks the size, for example
`1.875rem` / `700`); nothing else renders through it. About 5 min.
Stop-safe.

### 2. Inserting text at the caret or selection is one operation of the title editor
Type: Structure
Status: planned
Proof: `SeamlessTextEditor.spec.ts` and `PathNameEditor.spec.ts` stay green,
with one new editor case: focus the editor, set a selection, blur to a
button, call the exposed insertion → the text replaces the remembered
selection and the caret follows the inserted text; and one case: no
selection ever placed → the text is appended.

Internal change: `SeamlessTextEditor.vue` exposes
`insertAtSelection(compose: (before: string, after: string) => string)`:
it resolves the target range as the current selection when it is inside the
editor, otherwise the selection it remembered on its last `blur` or
`selectionchange`, otherwise the end; builds `before + compose(before, after) + after`,
updates the content, emits `update:modelValue`, and puts the caret after the
inserted text on the next tick. `onPaste` becomes
`insertAtSelection(() => plainText)` with its own early returns kept.
`PathNameEditor.vue` exposes the same method through to the inner editor
(the `#title` slot consumer reaches it via the slot's `editor` component or
a ref the implementer chooses) and keeps `applyExternalValue` until slice 4
removes its last caller. Enables slice 3. About 5 min. Stop-safe: paste
behaves as today.

### 3. On the note page, spoken words go where the caret or selection is
Type: Behavior
Status: planned
Proof: `joinDictatedSegments.spec.ts`: `dictatedInsertion(before, segments, after)`
cases: empty before; Latin before and after (one space each side); CJK on
either side (no space); whitespace already present (no second space).
`NoteEditableTitle.spokenTitle.spec.ts`: "appends at the caret at the end"
("Orchard" → "Orchard notes", caret after "notes", `updateTextField` called
with "Orchard notes"); "replaces the selection" ("Orchard notes", select
"Orchard", hear "Garden" → "Garden notes"); "inserts between words" (caret
between → "Orchard harvest notes"); "appends when the author never placed a
caret" ("Orchard notes" + "today"); "replaces the whole selected title";
"keeps a typed correction after speaking" and "replaces again when speaking
a second time" retargeted (select all before the second speak); the linked
rename case with a selection. E2E: "Rename a note by speaking the title"
and the linked scenario gain a step `When I select the whole note title`
(select all in the title editor) before `I speak the title`; assertions
unchanged. `docs/voice-input.md` "Speaking a title on an existing note"
rewritten for placement and the no-caret default.

Behavior: an editable note's title with a selection, a caret, or neither →
the author speaks and stops → the words replace the selection, are inserted
at the caret with the joining rule on both sides, or join the end; the caret
sits after the words, the title has focus, and the result is proposed
through the same path as typing. `NoteEditableTitle.vue` calls the editor's
`insertAtSelection((before, after) => dictatedInsertion(before, segments, after))`
instead of `update(joinDictatedSegments("", segments))`; the editor's
emitted update reaches `TextContentWrapper` through `PathNameEditor` as a
typed change does. `dictatedInsertion` lives beside `joinDictatedSegments`
and reuses its join rule for the left side and the mirrored rule for the
right side. About 8 min: one rule, one call site, but the E2E scenarios and
the documentation move with it. Stop-safe: New note keeps today's join
until slice 4.

### 4. In New note, spoken words go where the caret or selection is
Type: Behavior
Status: planned
Proof: `NoteNewForm.spokenTitle.spec.ts`: "replaces an untouched default
title" (selection intact), "replaces an untouched default title after the
selection was lost" (blur the editor and clear the selection first),
"joins heard segments onto a title pattern" (caret at the end of
"2026-10-06 "), "joins onto a typed title" (caret at the end of "Project"),
"inserts at a caret inside a typed title"; "searches for existing notes with
the heard title" and "submits the title after typing a correction" stay.
`docs/voice-input.md` "Speaking a title in New note" rewritten.

Behavior: New note's title with an untouched default → the words replace
it; otherwise → the same placement rule as the note page. `NoteNewForm.vue`
calls `applyExternalValue(joinDictatedSegments("", segments))` for the
untouched default (the rule stays in `noteNewFormTitle.ts` as a decision
"replace the untouched default, otherwise insert") and the editor's
`insertAtSelection` otherwise; `titleAfterSpokenSegments` goes.
`hasTitleBeenEdited` stays for the search key. About 5 min. Stop-safe.

### 5. One small microphone button, no status line, errors in toasts
Type: Behavior
Status: planned
Proof: `spokenTitleTestSupport.ts` retargeted: find the button by accessible
name ("Speak the title", "Stop speaking the title"), delete
`speakTitleStatus`/`speakTitleStatusNode` and `findSpeakTitleButtonByText`.
`NoteEditableTitle.spokenTitle.spec.ts` and `NoteNewForm.spokenTitle*.spec.ts`:
idle button named "Speak the title" without `aria-pressed`; click →
`aria-pressed="true"`, pressed style, name "Stop speaking the title", no
`[role="status"]` anywhere; stop → `disabled` while converting (hold
helper), then idle; nothing heard (silent stop and empty-segment response)
→ idle, title unchanged, `noToastShown`; microphone refused → `toastShown("error")`
with the existing wording, button idle; failed conversion →
`toastShown("error")` "Could not turn your speech into text.", title and
caret unchanged, idle, next speak uses a fresh recorder; readonly → no
button; New note Submit disabled while listening or converting and Enter
inert while listening stay. E2E: `noteCreationForm.speakTheTitle` and
`stopSpeakingTheTitle` click by the new names; scenarios unchanged
otherwise. Sweep reading over the product: "Recording. Speak now.",
"Turning your speech into text", "No speech was turned into text",
`role="status"` in the control, `speak-title-control`, `daisy-btn-sm`
text button "Speak the title", `titleAfterSpokenSegments` return nothing.
`docs/voice-input.md`: both title sections describe the button, its names
and appearances, and the toasts. By eye on the held stack: the button sits
at the end of the heading line and inside the New note field before the
Wikidata button.

Behavior: an editable title shows a small microphone button at its end;
click → listening, highlighted, named "Stop speaking the title"; click →
converting, unavailable; words placed → idle. `SpeakTitleControl.vue`
becomes the icon button with `aria-pressed` while listening, `disabled`
while converting, no status, `useToast().showErrorToast` for the two
failures, and keeps its recorder, `busy` model and `heardSegments`.
`NoteEditableTitle.vue` renders it inside the heading line (the `#title`
slot's `h2` becomes a flex row with the editor filling it); `NoteNewForm.vue`
renders it in `PathNameEditor`'s `#append` slot before `WikidataSearchByLabel`,
styled as a field-join append button like the Wikidata one. About 8 min:
the control, two placements, the spec retargeting through one support file,
the E2E page object and the documentation. Stop-safe.

## Current decisions

- Button names: "Speak the title" while idle and while converting; "Stop
  speaking the title" while listening. `aria-pressed` only while listening.
- Toast wording: microphone refused keeps "Could not use the microphone.
  Allow microphone access in your browser, then try again."; failed
  conversion "Could not turn your speech into text."; nothing heard shows
  nothing.
- The no-caret default is append (fill when empty); New note's untouched
  "Untitled" is replaced regardless of the selection.
- The target range is the editor's own: the current selection when inside
  the editor, else the remembered last selection, else the end. No
  dependence on the browser keeping the selection after a button click.
- Shared appearance, separate control: `SpeakTitleControl.vue` keeps its own
  stop-only recorder and becomes the small icon button using the body
  button's classes, icons and naming pattern; the body button's component
  and session stay the body's.
- The E2E scenarios select the whole title before speaking to keep their
  existing assertions; no scenario is added.
- The real-service feature is untouched (it has no title scenario).

## Story obligations

### G1. Reader's title not read in the product
Reported: slice 1 — "The readonly title for a reader who may not edit (for example a bazaar or shared notebook) was not observed in the product."
Story clause: "in view and while editing, on the note page for editors and readers alike"
Disposition: proved by slice 1: `frontend/tests/notes/NoteEditableTitle.heading.spec.ts` readonly case; readers and editors render the same `h2` under the same scoped rule

### G2. Product reading in Chromium only
Reported: slice 1 — "The product reading is Chromium only."
Story clause: "The note title is bold and larger than a body section heading"
Disposition: no user cost "the note title reads as the note's heading again": the change is two standard font declarations in one scoped rule, which no browser treats differently

## Execution

Story Branch Mode in `.worktrees/restore-title-styling-and-make-spoken-title-edit`
on `claude/restore-title-styling-and-make-spoken-title-edit`, published to
`origin`; claim `624fd7f060` accepted on `main`. CI source: GitHub Actions
`ci.yml` on the execution branch.

## Learnings

- One full frontend run failed once in
  `RichMarkdownEditor.propertyListWikiLinks.spec.ts` ("Frame was detached",
  5 s timeout) while a held stack, a Playwright script and the typecheck ran
  beside it; the run alone passed in full. That spec does not render the
  title. Cause not established beyond that; run the frontend suite without a
  held stack beside it.
- The held stack's `/api/testability/inject_notes` seeds a note for a by-eye
  reading at `/n<id>` (slice 5).
