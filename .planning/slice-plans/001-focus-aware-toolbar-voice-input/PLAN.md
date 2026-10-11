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
currently needed. Execution is authorized by the owner’s skill invocation.

## Execution workspace

Workspace: `/Users/terryyin/git/doughnut/.worktrees/dictate-into-the-focused-title-or-the-content-fr`.
Branch: `codex/dictate-into-the-focused-title-or-the-content-fr`.
Preparation provenance is retained in [PROOF.md](PROOF.md).

## Current decisions and reuse

Execution resumed on 2026-10-11 by Yui-chan under the owner's explicit
`dough-execute-plan` invocation. Story Branch Mode uses the established owned
workspace and branch above; integration checkout is `/Users/terryyin/git/doughnut`.
Publisher: `dashboard-territory.local-doughnut`; remote `origin`, trunk `main`.
Established starting revision: `44dc8e1135f423290df80d7db5d65b6508ffd183`.
Accepted Take and first increment base: `c69216321ab713b1566725628164b9064331ad52`.
Existing planning authority is retained for sizing refinement.
Checkout preparation `./scripts/run.sh bash scripts/worktree_setup.sh` passed;
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed.

CI source: GitHub Actions, verified `ci.yml` push workflow on all branches.
Story-branch observer: coordinator `yui-474320fd`,
directory `/tmp/dough-ci-501/watch-DeZNG8`, PID `33937`, stream session `67840`,
Codex cell `13`, execution checkout above. The trunk Take is unobserved by
this story-branch observer. Each increment attaches its accepted revision
through managed delivery; completion owns the final bounded CI wait.

One note-scoped decision chooses a field when a fresh recording starts; that
session owns its captured target until completion or departure. Toolbar focus
is a continuation of editing intent, not a global last-focused-field cache.
Keep the destination rule independent of how the action is reached: pointer,
keyboard, or the portaled overflow menu. Existing Stop and content Retry operate
their active journey rather than making a new title/content choice.

PFE covered frontend editors/toolbar/recording/saving, tests/E2E support, docs and CLI/MCP/API:

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

## Proof

[Baseline observations, outside-in proof ownership, and preparation review](PROOF.md)
retain the original evidence and the owning slice for each promise.

## Story obligations

### G1. Capture before microphone startup
Reported: slice 1 — "Startup still establishes insertion only after successful microphone startup, as before."
Story clause: "Capture whether the title has focus immediately before the toolbar voice input action."
Disposition: receiving slice 2

### G2. Persist the toolbar title journey
Reported: slice 1 — "These observations prove mounted behavior and request payloads, not hardware capture or persisted reload."
Story clause: "click toolbar Voice input, dictate "harvest" → "Orchard harvest notes", saved after reload."
Disposition: receiving slice 2

## Ordered slices

### 1. Share the existing title recording lifecycle

Type: Structure
Status: done
Accepted execution proof: [PROOF.md — Slice 1](PROOF.md#slice-1).
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
