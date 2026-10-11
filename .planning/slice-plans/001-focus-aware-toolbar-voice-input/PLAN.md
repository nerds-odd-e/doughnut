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

Existing-solution decisions and Accepted ADR constraints remain in [CONTEXT.md](CONTEXT.md).

## Proof

[Baseline observations, outside-in proof ownership, and preparation review](PROOF.md)
retain the original evidence and the owning slice for each promise.

## Story obligations

### G1. Capture before microphone startup
Reported: slice 1 — "Startup still establishes insertion only after successful microphone startup, as before."
Story clause: "Capture whether the title has focus immediately before the toolbar voice input action."
Disposition: proved by slice 3: mounted delayed-start toolbar case captures the title caret before microphone startup and moved focus.

### G2. Persist the toolbar title journey
Reported: slice 1 — "These observations prove mounted behavior and request payloads, not hardware capture or persisted reload."
Story clause: "click toolbar Voice input, dictate "harvest" → "Orchard harvest notes", saved after reload."
Disposition: proved by slice 3: clean toolbar caret E2E saves “Orchard harvest notes” and observes it after reload.

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

### 2. Own the existing title session at the note-page boundary

Type: Structure
Status: done
Accepted execution proof: [PROOF.md — Slice 2](PROOF.md#slice-2).
Proof: Retained title/New note/departure suites and both typechecks preserve
conversion on Stop, insertion, cleanup, read-only/marker and recovery.

Place the existing single title session where the note page can share it with
its toolbar, retaining the existing title control. Capture the insertion range
synchronously when starting, before microphone startup, using `DictationTarget`;
failed startup restores editing. Do not add another recorder, splice algorithm
or app-wide registry. This only enables slice 3's immediate toolbar consumer.
Use existing mounted title observations and one delayed-start title case to
observe the captured range. Target ~5 minutes; hard 10 except measured proof wait.

### 3. Dictate into a clean title from the visible toolbar

Type: Behavior
Status: done
Accepted execution proof: [PROOF.md — Slice 3](PROOF.md#slice-3).
Proof: Mounted `NoteShow` real toolbar/editor/store plus mocked-audio caret E2E
and reload; both typechecks. Reuse the parked red examples in [ATTEMPT.md](ATTEMPT.md).

A clean existing title caret/partial/whole selection → visible toolbar Voice
input → Stop inserts at the frozen range with ordinary saving. Pointer focus
transfer and delayed startup do not redirect it; read-only/marker and finishing
feedback remain attached to the title. Stop continues that session after focus
moves; a new content-focused or initially unfocused recording chooses content.
Both current controls share the active title owner. Keyboard/overflow journey
and typed referenced drafts remain owned by slices 4–5; do not claim them here.
Use actual focus transitions, no target preseed/in-process mocks. Extend only
the clean-title E2E case to “Orchard harvest notes” with reload; retain body and
New note scenarios. Run focused baseline + new page spec, E2E feature and both
typechecks. New cases must be observed red before implementation.
Target 5–8 minutes; hard 10 except measured focused-test/stack wait.

### 4. Preserve the typed referenced-title draft and recovery

Type: Behavior
Status: planned
Proof: Mounted direct toolbar draft/save-choice/recovery cases and linked rename
E2E with reload, plus baseline consumer suites and both typechecks.

Typed referenced-title draft + selection → direct toolbar start/Stop → preserve
surrounding typed text and show ordinary explicit reference-save choice. Failed
microphone startup, silent speech, empty response and failed conversion restore
editing and preserve the draft; a fresh recording succeeds. Conversion keeps
read-only/marker/finishing feedback until words arrive, with accessible control
and reduced-motion marker behavior. Leaving the journey normally retains the
existing discard rule. Extend the linked E2E toolbar case with explicit choice,
reload and visible reference assertion; New note/body/retry remain preserved.
Add red cases first; reuse existing title feedback rules through the actual
note-page toolbar. No title retry/retained audio or wording/cadence changes.
Target 5–8 minutes; hard 10 except measured focused-test/stack wait.

### 5. Keep title intent while reaching the toolbar action

Type: Behavior
Status: planned
Proof: Real mounted focus/keyboard/portaled overflow transitions, typed linked
 draft and failed-start recovery; next content session, baseline + typechecks.

Select “Spring” in “Spring orchard notes” → reach Voice input by keyboard or
more options → Enter/Space or pointer activation → Stop yields “Autumn orchard
notes”. One targeting rule owns all routes. Preserve draft/range through menu
open/close and startup failure; pin active Stop in a narrow toolbar. Content or
unrelated focus clears intent; another note has its own intent. Observe new
cases red first, then final title/save-choice and next content session, without
an internal target assertion. Safe stop: all supported toolbar routes work.
Target 5–8 minutes; hard 10 except measured focused-test/stack wait.

### 6. Use the toolbar as the existing-note voice entry point

Type: Behavior
Status: planned
Proof: Full frontend suite, both typechecks, mocked-audio E2E feature; one-time
product removal reading. Regenerate component inventory through Vite build.

Delete the separate existing-title microphone and recursively delete exclusive
code/styles/helpers/fixtures/tests. Retain New note's spoken-title control and
shared behavior. Move surviving title coverage to the actual toolbar boundary;
no tests/guards asserting the removed control's absence. Sweep backend/frontend,
CLI/MCP/docs/comments/guidance/generated artifacts for deleted exclusive symbols
and obsolete descriptions. Update `docs/voice-input.md` and
`docs/voice-input-spoken-title.md` with final focus/toolbar/field feedback rules.
Record the sweep as a reading. No new behavior hides in cleanup. Target ~5
minutes plus measured proof wait; hard 10 for implementation/cleanup.

## CI repair

[New note startup readiness](CI-REPAIR.md) records the owned failure and repair;
slice 3 was restored without conflicts after the repair was published.

## Refinement decision

Original slice 2 stopped before production edits; its red spec and sizing
failure are retained in [ATTEMPT.md](ATTEMPT.md). Six total slices replace the
original four. The single title session and one destination rule remain the
cumulative design. Separate ownership/capture, clean-title use, referenced-draft
policy, toolbar navigation and removal each have one proof loop. Slice 1 stays
delivered. No source scope, ADR or North Star decision changed. Execution can
resume after publishing this refinement; the existing Taken claim is retained.

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
and wrap-up records under the ordinary lifecycle.
