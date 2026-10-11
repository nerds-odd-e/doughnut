# Focus-aware toolbar voice input

**Identity:** SEED-066#focus-aware-toolbar-voice-input
**Source:** [Dictate into the focused title or the content from the toolbar](../../seeds/SEED-066-voice-input.md#focus-aware-toolbar-voice-input)

## Goal and scope

An author editing an existing note starts voice input from the toolbar. Title
focus selects the title and its caret/selection; otherwise the existing content
journey applies. Preserve title intent while the author reaches that action
through the toolbar or its overflow menu, including by keyboard. Moving outside
that journey clears title intent. The owner accepted this rule on 2026-10-11.

Freeze the destination and insertion range for the recording. Preserve title
conversion on Stop, content conversion/retry, read-only and pending-marker
feedback, ordinary title validation and saving, and the explicit reference-save
choice. Preserve a typed title draft while reaching the recording controls.
Remove the separate existing-title microphone and its exclusive dependencies;
retain New note's spoken-title journey and shared code it needs.

Title-aware AI wording, title retry/retained audio, cadence changes, a target
picker, and a layout redesign are deferred. No backend contract change is
currently needed. This is a planning-only draft, not execution authority.

## Workspace and preparation

- Reused established owned workspace:
  `/Users/terryyin/git/doughnut/.worktrees/dictate-into-the-focused-title-or-the-content-fr`.
- Branch: `codex/dictate-into-the-focused-title-or-the-content-fr`.
- Starting revision and published assignment: `74b65f04be2a91559f913448b2fee6335c19ef30`.
- Preparing agent: Koharu-chan; integration checkout: `/Users/terryyin/git/doughnut`.
- Authorized publication target, if later kept: `origin/main`.
- The owner authorized landing the seed and this plan on 2026-10-11. Landing
  releases the Preparing assignment and retires the preparation workspace;
  execution must establish its own workspace under the normal execution workflow.

## Current decisions and reuse

One note-scoped decision chooses a field when a fresh recording starts; that
session owns its captured target until completion or departure. Toolbar focus
is a continuation of editing intent, not a global last-focused-field cache.
Keep the destination rule independent of how the action is reached: pointer,
keyboard, or the portaled overflow menu. Existing Stop and content Retry operate
their active journey rather than making a new title/content choice.

PFE search covered frontend editors, toolbar, recording and save owners, their
tests and E2E support, voice-input documentation, and CLI/MCP/API entry points:

| Responsibility | Existing solution and decision |
| --- | --- |
| Caret/selection insertion, joining, locking, marker and caret restoration | Reuse `DictationTarget`, `SeamlessTextEditor.beginDictation`, `PathNameEditor`, and `useTitleDictation`. Do not create another title-string splice or selection model. |
| Title recording and conversion on Stop | Modularize the lifecycle currently inside `SpeakTitleControl` so the current control and the toolbar can invoke the same title-session owner. Keep New note on the same behavior. |
| Content recording, mutation admission and retry | Reuse `useNoteVoiceInput`, `useNoteAudioProcessing`, and `noteContentMutationBarrier`; content retry retains its existing insertion rules. These are body-specific policies, not a reason to route titles through body saving. |
| Title validation and persistence | Reuse `NoteEditableTitle` → `PathNameEditor` → `TextContentWrapper` → note store. Preserve explicit reference handling and ordinary normalization. Extend draft lifetime only for the authorized toolbar journey/session. |
| Toolbar access and active control | Extend `NoteToolbarMoreOptions` / `NoteMoreOptionsActions` / `NoteVoiceInputButton`; retain active-action pinning and the real dropdown's focus lifecycle. |
| Choosing the focused field | Gap: the toolbar currently starts only content recording. Connect the title editor and toolbar through the existing note-page ownership boundary; avoid an app-wide editor registry or a second transcription service. |

The cumulative design is one targeting rule plus the existing field-specific
recording policies. Slice boundaries below are delivery steps, not separate
recognizers or permanent implementations. Sharing the title lifecycle is
immediately required by the next slice, not speculative infrastructure.

Applicable Accepted decisions (index and in-file status agree):

- [ADR 0002 — Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md): use ordinary accepted title/content writes, preserving the publication boundary.
- [ADR 0004 — OKF-compatible notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#titles-filenames-body): preserve ordinary title/name semantics and normalization.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md): navigation proof uses production named routes and existing page objects.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage): preserve deliberate microphone/conversion feedback and recovery; add no generic swallowed failures.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md): use this worktree's disposable E2E allocation.
- [North Star: One real-service audio test](../../NORTH-STAR.md#one-real-service-audio-test): new E2E coverage uses mocked OpenAI services. No paid observation is required.

No conflicting decision or new North Star direction was found.

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

## Outside-in proof ownership

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

## Ordered slices

### 1. Share the existing title recording lifecycle

Type: Structure
Status: planned
Proof: Existing title and New note mounted behavior suites remain green, including
conversion on Stop, pending marker, no words/failure, and note departure.

Modularize only the recording lifecycle currently coupled to `SpeakTitleControl`
so it can be invoked by the toolbar in slice 2 while preserving the current
control's behavior. Keep one owner for a title session and its cleanup; do not
create a second recorder in the same note or unify the distinct title/body
conversion policies. Use the existing `DictationTarget` insertion owner.

Run the focused baseline command in observation 3 and frontend typechecks.
This is safe to stop with all existing controls unchanged. Target ~5 minutes
including focused proof and cleanup; no further preparatory framework.

### 2. Dictate into the title from the toolbar

Type: Behavior
Status: planned
Proof: New mounted `NoteShow` targeting cases, adapted title session/recovery
cases, and updated mocked-audio E2E title scenarios reach the real toolbar.

Behavior: An editable existing title with a caret or selection → activate the
visible toolbar Voice input action → title speech inserts at that captured
position on Stop, preserving surrounding text and the ordinary save path.
Otherwise start the existing content journey. Freeze the field and range
before pointer focus transfer and asynchronous microphone startup; a failed
startup leaves the author's draft editable. Stop continues the captured
session even if focus moved. Preserve feedback, conversion policy, navigation
cleanup, and content Retry. A new recording evaluates focus again.

Integrate referenced-title draft lifetime with this journey: the toolbar is
outside the current title wrapper, so preserve a typed draft while starting,
recording, and stopping; then let the author make the existing reference-save
choice. Leaving that editing journey normally still uses the existing discard
behavior. Test a draft containing typed changes as well as a clean saved title.

Extend `record_live_audio.feature`'s existing-note cases to toolbar actions
without changing New note steps. Use a title caret example ("Orchard notes" +
"harvest" → "Orchard harvest notes") and the linked-title save choice, with
explicit reload and resulting title/reference assertions. Keep whole and
partial selection variations in mounted proof. The existing body scenarios
continue to prove persisted content and retry.

Run observation 3's focused command plus the new note-page spec, observation 4's
E2E command, and frontend typechecks. Verify each new case red before its
implementation and green afterward; do not substitute test setup for routing.

Interim behavior: the separate title control remains available; keyboard and
overflow title activation are completed in slice 3. Both visible controls use
the same active title lifecycle, so they cannot create overlapping recordings.
Slice 4 removes the redundant control. Target 5–8 minutes of implementation,
focused verification and cleanup, scrutinized because of referenced-title blur;
slice 1 removes the recording extraction from this step. Only measured focused
test/stack wait may extend beyond 10 minutes; implementation overrun triggers
finer decomposition before continuing.

### 3. Keep title intent while reaching the toolbar action

Type: Behavior
Status: planned
Proof: Mounted note-page keyboard/overflow scenarios with the real dropdown,
selection and typed referenced-title draft; observe final title/save choice and
the next content session rather than an internal target flag.

Behavior: Select "Spring" in "Spring orchard notes" → reach Voice input by
keyboard through the toolbar or through its "more options" menu, speak
"Autumn", Stop → "Autumn orchard notes". Enter/Space activation and pointer
opening of overflow share the same target rule. The dropdown's portaled panel
belongs to that journey; opening/closing it must not clear or redirect title
intent. Preserve the current typed draft and selection during that transition
and through a microphone-start failure. Keep the active Stop action reachable
on a narrow toolbar.

Moving focus to content or another unrelated destination before starting clears
title intent. Later activation then follows existing content insertion rules.
Moving to a different note also starts with that note's own intent. Extend the
same routing model from slice 2, not a keyboard-specific recorder or cache.

Run the new navigation spec, observation 3's focused command, and frontend
typechecks. Verify the new navigation cases fail before the behavior is added.
Safe stopping point: all supported toolbar routes work; slice 4 only completes
the single-control outcome and cleanup. Target ~5 minutes, up to 8 after
scrutiny for real focus and portal transitions; apply the same hard-limit rule.

### 4. Use the toolbar as the existing-note voice entry point

Type: Behavior
Status: planned
Proof: All retained toolbar title/content and New note journeys pass, including
the focused mocked-audio E2E feature. Complete the one-time removal reading.

Behavior: An author on an editable existing note uses the toolbar for either
field; New note still offers its spoken-title control. Delete the separate
existing-title microphone and recursively remove everything exclusive to it.
Move surviving title behavior coverage to the actual toolbar boundary; delete
tests of the removed presentation. Preserve New note's shared control/session,
fixtures and behavior. Do not add tests or guards asserting the old control is
absent.

Sweep backend, frontend, CLI, MCP, docs, comments, guidance and generated
artifacts for obsolete existing-title-control references. Update
`docs/voice-input.md` and `docs/voice-input-spoken-title.md` to describe the final
focus rule, toolbar journey and field-specific feedback; remove obsolete
descriptions rather than retaining historical notes. Shared New note names
remain valid. Record a one-time search/read result for the actual deleted
exclusive symbols and obsolete descriptions as acceptance reading, not a test.

Run the full frontend suite (shared control and test-helper consumers span New
note, note page and toolbar), frontend typechecks, and observation 4's E2E
command. If component inventory changes, regenerate it with the frontend skill's
Vite build command. Target ~5 minutes plus measured verification wait; no new
behavior or subsystem refactor is hidden in cleanup.

## Verification and delivery gates

The [frontend skill](../../../.agents/skills/frontend/SKILL.md#frontend-proof)
requires both typechecks on each accepted frontend change:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Use the [unit-testing](../../../.agents/skills/unit-testing/SKILL.md),
[frontend-testing](../../../.agents/skills/frontend-testing/SKILL.md), and
[E2E-authoring](../../../.agents/skills/e2e-authoring/SKILL.md) rules. The main
proof is the integrated note-page UI plus saved state after reload, not internal
helpers. Repeat broader suites only after relevant edits; full frontend proof
at the final shared-surface removal uses `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
No full E2E suite, hardware recording, paid API call, or manual-testing gate is
required by this plan.

When execution is authorized, apply `AGENTS.md` and dough-execute-plan delivery:
Jidoka → fresh dough-post-change-refactor agent → API generation if needed →
coordinator runs `./scripts/run.sh pnpm format:changed` once → update this plan
without another routine format pass → commit with check-only lint hook → push
and own asynchronous CI repair. Implementer/refactorer runs neither formatter
nor standalone `lint:changed`. Preparation itself does not start those steps.

Slices target roughly five minutes including focused proof and cleanup.
Scrutinize work over five; at ten minutes stop and finer-decompose unless the
recorded excess is focused-test or external wait. Record elapsed time and the
failed sizing assumption; preserve compatible work. Scope changes return to
the owner instead of silently dropping a promise. Keep execution, retrospective,
and wrap-up records under the ordinary lifecycle; this plan has no done slices
or execution-complete claim.

## Plan review

No extra slice-plan-refinement pass was needed: the initial decomposition keeps
the necessary lifecycle extraction immediately before its first consumer,
separates toolbar navigation risk, and gives final removal its own safe boundary.
The temporary second title entry point is explicitly replaced by slice 4.
No remaining slice-specific concern was identified after the baseline and
discrimination observations. This is planning confidence, not product acceptance.
