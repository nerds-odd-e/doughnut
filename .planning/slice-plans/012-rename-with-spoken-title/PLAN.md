# Change an existing note's title by speaking

## Source

- Story: [Change an existing note's title by speaking](../../seeds/SEED-066-voice-input.md#rename-with-spoken-title)
- Identity: SEED-066#rename-with-spoken-title
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/change-an-existing-note-s-title-by-speaking`
- Branch: `claude/change-an-existing-note-s-title-by-speaking`
- Preparing agent: Yumi-chan; established assignment revision `caf58408a5ac6c9a92ca7b1c254db0237c649eed`.
- Publication target: `origin/main`; integration checkout: `/Users/terryyin/git/doughnut`.
- Authority: the current request is planning only. This plan does not Take,
  execute, commit to trunk, publish, or release the preparation assignment.

## Goal and scope

A note author who may edit a note renames it by speaking: the note page's
title offers New note's "Speak the title" control, the heard words replace
the whole title after Stop, and the result is saved by the same rules as a
typed title. A note nothing links to saves as typing does; a note other notes
link to shows the existing reference panel and saves only on a choice, and
leaving without choosing discards the heard title. Readers who may not edit
the note are not offered the control. Nothing heard or a failed conversion
leaves the title alone, and speaking again replaces it again. The body and
body dictation are untouched.

Excluded, per the story: speaking a title from the sidebar, for a folder, or
in the book-reading block dialog; speaking while Audio tools records the
body; automatic stop on silence; tidying the heard words; choosing the
reference handling by voice; a microphone chooser or Write text now for the
title; keeping a failed recording for Retry; observing operating-system
dictation or hardware capture. No backend change is expected.

Two refinement proposals the owner can still overrule are built in: the
native control instead of OS dictation, and replace instead of join.

## Existing solutions and architectural constraints

PFE search covered the note page title editor, New note's spoken title, the
audio recorder and transcription path, frontend mounted-test support, E2E
audio steps and page objects, and the voice-input documentation:

- Reuse `frontend/src/components/notes/SpeakTitleControl.vue` unchanged. It
  owns listening, converting only at Stop, the status wording, failure
  handling, a fresh recorder per attempt, and the `busy` model; it emits
  `heardSegments` once after Stop.
- Reuse `joinDictatedSegments` from `frontend/src/models/audio/` for the
  replace rule: New note already replaces an untouched default by joining
  the segments onto an empty title (`noteNewFormTitle.ts`). The existing
  note joins onto an empty string every time; no new helper is warranted.
- Change `frontend/src/components/notes/core/NoteEditableTitle.vue`: it
  already receives `readonly` and renders the title through
  `TextContentWrapper`'s default slot, whose `update` is the one path typing
  uses. Feeding the heard title through `update` gives autosave for an
  unlinked note and the reference panel for a linked note without touching
  `TextContentWrapper.vue`.
- Reuse `scheduleFocusTargetWithin` (`frontend/src/utils/focusTarget.ts`,
  already used by `PathNameEditor`) to return focus to the title once the
  heard words are in it.
- Extend, do not duplicate, the E2E support: `@usingMockedOpenAiService`,
  "the OpenAI transcription service will return the text … when I stop",
  "I speak the title", and "I stop speaking the title" (`audio.ts`) find
  buttons by name and work on the note page as they are; only the
  assertions differ (`the note title should be`, `the note {notepath} in
  Donut should have content`, the wiki-link assertions in `wiki_link.feature`).
  The referenced-title page object (`notePage.ts` `saveReferencedNoteTitle`)
  shows the panel test ids to click.
- Extend the mounted-test support: `noteNewFormTestSupport.ts` holds the
  spoken-title lifecycle, button finders, status reader and the
  hold-converting helper; `textContentWrapperTestSupport.ts` holds the
  referenced-title mount, edit and blur-discard flush helpers. Share what
  both new specs need rather than copying.

Accepted decisions: [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
keeps conversion failure visible through the control's existing message;
nothing is caught to continue silently. The North Star topic
[One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test)
governs: every new audio E2E scenario here uses the mocked OpenAI service. No
new ADR or North Star topic is warranted; the work follows established
structure.

Skills to load during execution: `unit-testing`, `frontend` (and its
path-scoped `frontend-component`, `frontend-testing`), `e2e-authoring`.

## Outside-in proof

Key examples are the story's, numbered as there.

| Promise (example) | Owner | Observable proof |
| --- | --- | --- |
| Editors get "Speak the title" with the heading; heard words replace the title; saved as typed; reload keeps title; body unchanged (1) | 1 | E2E scenario in `record_live_audio.feature`: mocked transcript, speak and stop on the note page, title on page and in the sidebar reads the heard words, the note's content in Donut unchanged; reload-equivalent via `the note title should be` after re-routing. |
| Correct by typing after speaking (2); speaking again replaces (3) | 1, 3 | Mounted `NoteEditableTitle` spec: existing title, spoken segments replace it; typing after that proposes the corrected value; a second spoken result replaces again. |
| Readers without edit rights see no control (8) | 1 | Mounted spec with `readonly: true`: no "Speak the title" button. |
| Control reads "Stop" while listening, status wording as New note (state in words) | 1 | Mounted spec asserts "Stop" and "Recording. Speak now." while listening on the note page; the control's own wording is already proven by `NoteNewForm.spokenTitle*.spec.ts`. |
| Linked note shows the reference panel with the heard title, saves on choice, keeps link text (4) | 2 | E2E scenario: note linked as `[[WikiLinks CI]]`, speak "WikiLinks CI Renamed", choose Keep visible reference text, the linking note still shows "WikiLinks CI" and it opens the renamed note. |
| Leaving a linked rename without choosing discards the heard title (5) | 2 | Mounted spec through the real `TextContentWrapper` with inbound references: after the heard title arrives, blur to outside; title back to the original, panel gone. |
| Nothing heard leaves the title and saves nothing (6); failure leaves the title, speaking again sends only the new recording (7) | 3 | Mounted specs: empty-segment response and a failing `audioToText` leave the title and the save spy untouched; a following successful attempt replaces the title. |
| Body dictation unchanged; New note unchanged; typed rename and undo unchanged (Keep) | 1–3 | Existing `record_live_audio.feature` scenarios and `NoteNewForm.spokenTitle*.spec.ts`, `TextContentWrapper.spec.ts` stay green; `note_edit.feature` and `wiki_link.feature` are untouched and run in CI. |
| Documentation describes the behavior (Keep) | 3 | New section in `docs/voice-input.md`. |

Focused commands (Nix prefix as the repository requires; `env -u NODE_ENV`
when the shell sets production):

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitle.spec.ts tests/notes/TextContentWrapper.spec.ts tests/notes/NoteNewForm.spokenTitle.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

The typecheck is required by the `frontend` skill's "Frontend proof" rule
before accepting any frontend change. The E2E feature is the stable boundary
for the main journeys (examples 1 and 4); edges stay in mounted specs.

## Decisive premises and observations

Observed on 2026-10-07 at revision `3728347b1a` (only planning prose differs
from trunk).

| Premise | Consumed by | Literal observation and result |
| --- | --- | --- |
| The title editor accepts a proposed value through the slot's `update`; a linked note shows the panel and saves nothing until a choice, an unlinked note autosaves | 1, 2 | Read `TextContentWrapper.vue` `onUpdate`: blank title ignored; with `titleRenameNeedsExplicitReferenceChoice` it sets `localValue` and the panel shows via `hasUnsavedChanges()`; otherwise `propose` debounces the save. True. |
| Discard happens only on `focusout` leaving the wrapper root; a target inside the root is kept | 2 | Read `onReferencedTitleFocusOut` and the spec "does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper". True; so the control must render inside the wrapper's default slot, and focus must be back in the title after the heard words for "leaving" to mean the same as after typing (Stop is disabled while converting, so focus does not stay on it). |
| `SpeakTitleControl` is self-contained and emits segments once after Stop | 1, 3 | Read `SpeakTitleControl.vue`: `convertOnlyAtStop`, `emit("heardSegments")` in `processAudio`, phase and status table, fresh recorder per attempt. True. |
| `readonly` reaches `NoteEditableTitle` for non-editors | 1 | Read `NoteShow.vue:28` `readonly: readonly(noteRealm)` → `NoteTextContent.vue` → `NoteEditableTitle` prop. True. |
| Replacing by joining onto an empty title is the existing rule | 1 | Read `noteNewFormTitle.ts` `titleAfterSpokenSegments`: `joinDictatedSegments("", segments)` when replacing the untouched default. True. |
| Mounted spoken-title and referenced-title test support exists and passes | 1–3 | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/TextContentWrapper.spec.ts tests/notes/NoteNewForm.spokenTitle.spec.ts`: 2 files, 14 tests passed (4.38 s). |
| E2E speak/stop steps work on any page; assertions are form-specific | 1, 2 | Read `audio.ts` and `noteCreationForm.ts`: `cy.findByRole('button', { name: 'Speak the title' })` and `Stop` by name; `expectTitle` reads the form's Title field, so note-page scenarios assert with `the note title should be` (`note_editing.ts:96`) and Donut content steps. True. |
| Wiki-link rename E2E vocabulary exists for the linked journey | 2 | Read `wiki_link.feature` scenarios "Renaming a referenced note while keeping visible reference text" and the `notePage.ts` panel test ids. True. |
| Audio E2E must use the mocked service | 1, 2 | Read North Star "One real-service audio test" and `record_live_audio.feature`'s `@usingMockedOpenAiService`. True. |

No premise needs a paid, credentialed, or state-changing observation; no probe
slice is required.

## Current decisions and cumulative design

- One rule for a spoken title on an existing note: the heard segments joined
  onto an empty title become the proposed title, through the same `update`
  typing uses. No second save path, no special case for linked notes: the
  wrapper already decides between autosave and the reference panel.
- The control renders inside `NoteEditableTitle`'s slot, under the heading,
  only when not readonly. It stays `SpeakTitleControl` as New note uses it;
  the note page does not bind its optional `busy` model (there is no Submit
  to hold). Shared spoken-title test helpers live in
  `frontend/tests/notes/spokenTitleTestSupport.ts`.
- After the heard words are in the title, focus the title editor so the
  author can correct by typing and so leaving the title area means the same
  as after typing. Do this through the existing focus helper, not a new
  mechanism.
- No new status text, no Retry, no recorder changes, no backend change.
- Documentation: one new section "Speaking a title on an existing note" in
  `docs/voice-input.md` after the New note section, in the same voice.

## Execution resume

- Mode: story-branch
- Workspace: `/Users/terryyin/git/doughnut/.worktrees/change-an-existing-note-s-title-by-speaking`
- Branch: `cursor/change-an-existing-note-s-title-by-speaking`
- Agent: Nao-chan; publisher ID: `dashboard-territory.local-doughnut`
- CI observer: `/tmp/dough-ci-501/watch-XW77Kl` (workflow `ci.yml`, target
  branch `cursor/change-an-existing-note-s-title-by-speaking`)
- Last accepted delivery: `07f888578ee96b7248e08ba202a5a252fe26ae8b`
  (CI repair for typed page-object chaining; prior slice 3
  `10834bc742d7475acfb550385c1b3ba79fe1f42d`).

## Ordered slices

### 1. Rename an unlinked note by speaking
Type: Behavior
Status: done
Proof: E2E scenario "Rename a note by speaking the title" in
`record_live_audio.feature` (mocked service; `@mockBrowserTime` + 1 minute
tick so title autosave fires); mounted
`NoteEditableTitle.spokenTitle.spec.ts` for replace-over-existing-title,
typed correction after speaking, "Stop" and status while listening, and no
control when readonly; typecheck.
Accepted commands:
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitle.spec.ts`
(+ New note spoken-title specs after refactor support extract);
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`;
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`.

Behavior: an editable note page shows "Speak the title" with the heading →
the author speaks and chooses Stop → the heard words replace the title, it is
saved as a typed title is, the sidebar and a later visit show it, and the body
is unchanged; a reader without edit rights sees no control.

### 2. Rename a linked note by speaking
Type: Behavior
Status: done
Proof: E2E scenario "Rename a linked note by speaking the title, keeping
visible reference text" in `record_live_audio.feature` (Keep via
`chooseReferencedTitleSave`); mounted
`shows the reference panel for a spoken linked rename and discards when leaving without choosing`
in `NoteEditableTitle.spokenTitle.spec.ts`; `TextContentWrapper.spec.ts` green.
No product change — slice 1's update path already showed the panel when
`hasInboundReferences` is true.
Accepted commands:
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitle.spec.ts tests/notes/TextContentWrapper.spec.ts`;
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`;
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`.

Behavior: a note other notes link to → the author speaks a new title and
chooses Stop → the heard title shows with the reference panel and focus in the
title; choosing Keep visible reference text saves the rename with unchanged
link text that opens the renamed note; clicking away without choosing restores
the old title and hides the panel.

### 3. Failed or empty speech leaves the title alone; speaking again replaces
Type: Behavior
Status: done
Proof: mounted `NoteEditableTitle.spokenTitle.spec.ts` — nothing heard
(silent stop), empty segments, fail-then-success with save spy untouched
until success; `docs/voice-input.md` section "Speaking a title on an
existing note"; focused command set green (editable + New note spoken-title
+ TextContentWrapper + vue-tsc + `record_live_audio.feature`).
Shared outcome stubs: `stubSilentStopRecording`,
`mockAudioToTextWithNoSegments`, `mockAudioToTextFailThen` in
`spokenTitleTestSupport.ts`.

Behavior: on the note page the author speaks and nothing is heard, or the
conversion fails → the status says so, the title and saved state are
unchanged → the author speaks again → only the new words replace the title.
Adds the `docs/voice-input.md` section describing slices 1–3.

## Execution complete

Product advice: Spoken rename on the note page delivered as intended. The
slice-2 page-object refactor broke typed Keep/Update wiki-link E2E via
arrow-function `this`; that regression was repaired on
`07f888578ee96b7248e08ba202a5a252fe26ae8b` (correction plan 013 marked done;
DD-218 recorded). No other product follow-up; backlog order unchanged.
