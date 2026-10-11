# Focus-aware toolbar voice input: proof

## Observed premises and limits

All commands below ran in the named workspace on 2026-10-11, with product code
at the starting revision. Only preparation prose remained changed afterward.

1. Existing title insertion consumes the captured range through the real editor.
   Setup and assertions: `frontend/tests/notes/NoteEditableTitle.spokenTitlePlacement.spec.ts`;
   read-only and feedback observations: its `spokenTitleReadOnly` and
   `spokenTitleMarker` siblings. Command:
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitlePlacement.spec.ts tests/notes/NoteEditableTitle.spokenTitleReadOnly.spec.ts tests/notes/NoteEditableTitle.spokenTitleMarker.spec.ts`.
   Result: 19 passed; title words, caret, request payload, read-only lifetime,
   pending marker, and recovery reached their assertions.
2. Placement proof discriminates. Temporarily replace
   `useTitleDictation`'s `insertHeardSegments` call to `session.insert` with a
   no-op; run
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle.spokenTitlePlacement.spec.ts`.
   Result: all seven cases failed on the resulting title text (for example,
   `Orchard` instead of `Orchard notes`), exit 1. Restored the original file in
   `finally`, then ran the broader command below successfully. The scratch
   change is not retained.
3. The title/body consumers and recovery routes run in local headless Chromium.
   Command:
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle tests/notes/NoteNewForm.spokenTitle tests/notes/NoteShow.spokenTitleLeavingNote.spec.ts tests/notes/NoteVoiceInputButton tests/notes/NoteToolbar.voiceInput tests/notes/TextContentWrapper.spec.ts`.
   Result: 23 files / 144 tests passed, Vitest duration 9.36 s. Fixtures mock
   microphone/worklet and backend HTTP; this observes DOM and save requests,
   not actual persistence. In particular, `TextContentWrapper.spec.ts` confirms
   that leaving a dirty referenced-title wrapper discards its draft. The new
   toolbar continuation must deliberately accommodate that existing rule.
4. The actual isolated mocked-audio E2E route reaches backend saves and later
   reads. Command:
   `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature`.
   Result: all five scenarios passed (15 s scenario run after stack startup)
   at `http://127.0.0.1:60374`, database
   `doughnut_e2e_wt_e3618e1b03f54ecabf55100d652a13c5`. Existing note rename,
   referenced rename with kept visible text, New note, body conversion and retry
   reached their observable outcomes. Setup and assertions are in that feature;
   `audio.ts`, `noteCreationForm.ts`, and `voiceInputPage.ts` own its UI actions.
   Current title scenarios invoke the separate control: they establish reusable
   recording/save machinery, not the proposed toolbar route. Extend them as
   specified below, with explicit reload for final persistence proof.

New targeting and toolbar draft retention are implementation obligations, not
claimed existing behavior. Future tests must drive actual focus transitions,
not preseed a target or mock in-process session routing. No unobserved external
premise or early probe is needed for this approach.

## Original outside-in proof ownership (before refinement)

Use mounted `NoteShow` with real toolbar, title/content editors, dropdown and
store for routing proof; mock only allowed device boundaries and backend HTTP.
Extend existing fixtures instead of making a parallel note-page harness.
Proposed new tests live under capability names such as
`frontend/tests/notes/NoteShow.focusAwareVoiceInput.spec.ts` and
`NoteShow.voiceInputToolbarNavigation.spec.ts`.

| Final promise | Owning slice and observable proof |
| --- | --- |
| Title caret insertion and partial/whole selection; preserve surrounding title | 2: mounted note-page actions assert title and ordinary save payload; mocked-audio E2E inserts within an existing title and reloads it. |
| Content focus, unrelated focus, or no title focus selects existing content behavior | 2: note-page title → content and initial content cases; retained rich/Markdown insertion and saved-body cases. 3 adds leaving the toolbar journey for an unrelated destination. |
| Keyboard and overflow preserve title intent and selection | 3: real focus/keyboard interactions and actual overflow action yield the selected title replacement; a subsequent fresh content session writes content. |
| Captured session is stable; Stop finishes it; next recording chooses again | 2: move focus/selection during recording and conversion, assert insertion at captured title range; finish and start a fresh content session. Navigation uses existing departure behavior and never targets the destination note with old title speech. |
| Read-only, marker, finishing state, accessible action, reduced motion and editable caret afterward | 2: adapt surviving mounted title behavior tests to toolbar activation, asserting the editor/marker and control states. 3: narrow toolbar keeps Stop reachable while active. |
| Microphone failure, empty speech and conversion failure preserve title and restore editing; content retry survives | 2: toolbar-driven title recovery cases plus existing content retry suite. 1 protects the shared title lifecycle, including New note. |
| Typed referenced-title draft and range survive toolbar access; save requires ordinary reference choice | 2: typed draft → direct toolbar start/stop → save choice and title payload; E2E saves a referenced rename and verifies the link after reload. 3 repeats draft retention through overflow/keyboard access, including microphone startup failure. |
| One toolbar action on existing notes; New note keeps spoken-title entry | 4: surviving toolbar and New note journeys pass; one-time product-wide removal reading, not an absence test. |

## Plan review

No extra slice-plan-refinement pass was needed: the initial decomposition keeps
the necessary lifecycle extraction immediately before its first consumer,
separates toolbar navigation risk, and gives final removal its own safe boundary.
The temporary second title entry point is explicitly replaced by slice 4.
No remaining slice-specific concern was identified after the baseline and
discrimination observations. This is planning confidence, not product acceptance.

## Preparation provenance

- Reused established owned workspace:
  `/Users/terryyin/git/doughnut/.worktrees/dictate-into-the-focused-title-or-the-content-fr`.
- Branch: `codex/dictate-into-the-focused-title-or-the-content-fr`.
- Starting revision and published assignment: `74b65f04be2a91559f913448b2fee6335c19ef30`.
- Preparing agent: Koharu-chan; integration checkout: `/Users/terryyin/git/doughnut`.
- Authorized publication target, if later kept: `origin/main`.
- The owner authorized landing the seed and this plan on 2026-10-11. Landing
  releases the Preparing assignment and retires the preparation workspace;
  execution must establish its own workspace under the normal execution workflow.

## Slice 1

Shared `useTitleVoiceInput` owns title recording, conversion on Stop, feedback
and cleanup; `useTitleDictation` keeps the existing `DictationTarget` owner.
Both current title controls invoke it. Consumer sweep found only
`NoteEditableTitle.vue` and `NoteNewForm.vue`, both updated; no Storybook/direct
control test consumer. Approximately six active minutes, with remaining Nix
startup and proof wait separated. Independent refactor found production clean
and split this planning evidence to respect the 250-line limit.

Accepted proof (2026-10-11):

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableTitle tests/notes/NoteNewForm.spokenTitle tests/notes/NoteShow.spokenTitleLeavingNote.spec.ts tests/notes/NoteVoiceInputButton tests/notes/NoteToolbar.voiceInput tests/notes/TextContentWrapper.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

23 files / 144 tests passed; both typechecks exited 0. Inspected production
changes and unchanged device/API setup in `spokenTitleTestSupport.ts`.
Observations: placement suite asserts inserted words, caret and save payload;
`NoteEditableTitle.spokenTitle.spec.ts` observes no words and failed conversion
followed by a fresh successful session; marker and read-only siblings observe
listening/converting state and editable restoration; New note outcomes preserve
silence/failure behavior; `NoteShow.spokenTitleLeavingNote.spec.ts` observes note
departure, destination text/editability and the next recording. No product
refactor edits invalidated proof. Whitespace check and selective formatting
passed. Hardware capture is outside this plan; toolbar persistence belongs to
slice 2. Startup target capture remains slice 2.

Published slice 1: `a3a2bb2bdec58f3720063d4be74f72ebc77de0e7` accepted on
`origin/codex/dictate-into-the-focused-title-or-the-content-fr`, with suffix
base `c69216321ab713b1566725628164b9064331ad52`, no replay. Managed delivery
reused `/tmp/dough-ci-501/watch-DeZNG8`; CI remains asynchronous.

## Current proof ownership after refinement

Slice 2 preserves shared title behavior and delayed startup capture. Slice 3
owns clean title caret/partial/whole insertion, frozen session, next content
choice and persisted caret reload. Slice 4 owns typed linked draft/save choice,
all title recovery/read-only/marker/accessibility/reduced-motion observations
through the toolbar, and persisted linked rename/reference reload. Slice 5 owns
keyboard/overflow, unrelated focus, draft/start failure through navigation and
narrow active Stop. Slice 6 owns the single existing-note entry point, surviving
New note/body journeys and one-time deletion sweep. The original table above
is provenance; these current owners and the plan govern execution.
