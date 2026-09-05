# Preserve promised outcomes through planning and execution

Source: [SEED-010 retrospective and selected response](../../seeds/SEED-010-execute-plan-wrap-up-token-efficiency.md#cross-project-retrospective-findings-and-possible-responses-2026-09-05),
including the developer's relayed Pygardon recommendations.
Status: planned — ready for direct execution.

## Outcome and boundaries

The developer needs a slice accepted as complete to have observable evidence for
its promised outcomes, including the final behavior replacing an interim path.
Small changes to existing planning and execution instructions will carry those
obligations from the selected contract through refinement, delegation, and proof
acceptance. Refactor review retains its structure-only role.

Example: the selected clone contract promises a checkout without a temporary
bundle remote → plan, optionally refine, and execute the clone slice → its named
proof observes the absence of remotes before the slice can be accepted. A passing
branch/tree check alone does not establish that promise.

Replacement example: publication supersedes an interim refusal → execute the
replacement → the affected CLI path reports final success and actual rejection
reasons, with fixtures, assertions, and guidance consistent with that contract.

Scope is instruction changes and their focused evaluation. No product feature or
defect implementation, Pygardon edits, tooling changes, CI issues or policy
changes, context/token optimization, transaction redesign, new review agent, or
generic proof framework. Existing product corrections are evaluation material.

## Outside-in proof and evaluation

The evaluator is the developer inspecting the produced plan, implementer proof,
and coordinator acceptance. A text search for new rule wording is insufficient.
Use compact before/after contract replays anchored to the original escaped
defects; then apply the new obligations during the actual execution of this
plan. Record replay evidence separately from that live workflow trial. This is
a process-change execution trial, not a claim of prospective product validation;
carry the latter as a follow-up observation for the next relevant product run.

| Promise in this plan | Owning slice | Observable verification |
|---|---|---|
| Promises anywhere in the selected contract reach execution acceptance | 1. Accept only evidence that covers the promised result | Clone replay identifies the missing remote-state observation; exact existing proof is accepted when it covers that state |
| Refinement/replacement/resume preserves those obligations | 2. Preserve proof ownership when slices change | Before/after mapping assigns the same clone promise to its replacement leaf; an orphan remains incomplete |
| An interim replacement delivers the final affected user path | 3. Accept a complete interim replacement | Publication replay and focused CLI execution cover final success, actionable rejection, and retained local state |
| Changed asynchronous ownership requires lifecycle-level evidence | 3. Accept a complete interim replacement | Telegram scenario rejects direct propagation alone as evidence for timely observation and cleanup; unchanged async ownership adds no obligation |
| Behavioral gaps return to implementation rather than refactor | 1. Accept only evidence that covers the promised result | Incomplete handoff cannot become complete through a structure-only refactor pass |
| Evidence is transferable and limitations are explicit | Each slice | Append compact case/result and focused commit IDs to SEED-010; final handoff identifies live trial versus replay |

Existing proof may cover several promises. Keep only selected, applicable
obligations; do not import every aspiration from a source seed or create tests
for arbitrary wording. No blanket suite runs or automated tests of Markdown
phrasing. Demonstrations may use temporary scratch artifacts, removed afterward.

## Ordered slices

### 1. Accept only evidence that covers the promised result
Type: Behavior
Status: planned
Proof: replay the original clone contract against an incomplete branch/tree-only
handoff and the corrected remote-state assertion in
`cli/tests/notebookClone.test.ts`; record which promise the first handoff misses
and why the second covers it. In this plan's live execution, hand off and accept
the demonstrated mapping itself before marking this slice complete.

Behavior: a selected contract contains a checkable final-state promise anywhere
in its text → plan and execute its owning slice → completion requires named,
observable evidence for that promise, reusing adequate existing proof.

Put the canonical obligation in `.cursor/rules/planning.mdc`; integrate it at
the existing steps in `slice-planning/SKILL.md` and the `execute-plan` delegation
and proof-acceptance instructions. Inspect `execute-plan/SKILL.md` and
`references/{delegation,wrap-up}.md` and use brief references rather than copies
of a new checklist. Missing behavioral proof returns to implementation before
acceptance; a refactorer may report a gap but does not introduce new behavior.
Do not change the existing proof-reuse/invalidation policy.

Sizing hypothesis: about five minutes including the focused contract replay;
medium confidence because the instruction owners are already identified.

### 2. Preserve proof ownership when slices change
Type: Behavior
Status: planned
Proof: refine a scratch clone slice into replacement leaves, then resume from
that revised plan. Show the remote-state promise still points to a named leaf
and observation. Deliberately omit its replacement assignment and demonstrate
that the revised plan cannot be declared ready; restore it before completion.
One existing proof can retain coverage for several promises.

Behavior: a plan already connects a promise to proof → refine, replace, or resume
its slices → the connection survives and an uncovered promise stays unfinished.

Extend `.agents/skills/slice-plan-refinement/SKILL.md` and the existing
`execute-plan` resume/pre-slice check to consume the same canonical obligation.
Preserve completed evidence unless a changed boundary invalidates it. Repoint
proof when renaming/replacing leaves; do not silently drop a selected promise or
duplicate tests. Apply this rule to any real plan edits during this execution.

Sizing hypothesis: about five minutes including one refine/resume demonstration;
medium confidence. Depends on slice 1's mapping and acceptance contract.

### 3. Accept a complete interim replacement
Type: Behavior
Status: planned
Proof: replay publication's interim-to-final transition using the repaired
clone guidance and `cli/tests/notebookPublish.submission.suite.ts`. Trace the
affected callers, fixtures, assertions, and documentation; identify which
observations would have exposed the escaped refusal/rejection gap. Run once:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.test.ts tests/notebookClone.test.ts`.
Reuse that exact result through wrap-up unless subsequent edits invalidate it.

Behavior: a slice replaces interim behavior → plan and execute the replacement
across its affected user path → acceptance establishes the final contract,
including relevant success/rejection outcomes and lifecycle obligations.

Add a conditional replacement obligation at the canonical planning proof owner
and consume it in execution delegation/acceptance. Identify affected callers,
fixtures, assertions, and documentation without requiring unrelated repository
cleanup. Searches establish scope; final observable behavior establishes proof.
Missing behavior is implementation work, not a refactor assignment. Keep the
refactor skill's structure-only remit unchanged.

For a change in asynchronous ownership, name the lifecycle owner, what counts as
timely failure observation, and resource cleanup after failure or shutdown as
applicable. Evaluate Pygardon's reported Telegram counterexample: an awaited
coroutine throwing proves propagation but does not prove background failure is
observed while the owner is running or that cleanup happens after failed work.
Require appropriate owner-boundary evidence; do not prescribe an arbitrary
timeout or duplicate propagation tests at unchanged callers. This is a
conditional instance of the same completeness rule, not a CI workstream.

Use this slice's actual delegation, focused proof, and acceptance as the live
workflow trial of the updated contract. Record original defect references
(`cfa5c7483d`, `86713e161c`), replay results, the exact command/result, new focused
commit IDs, and any limits in SEED-010 for Pygardon to adapt. Do not represent
existing repaired product code as a new correction or a replay as new product
execution. No automatic message or cross-project adaptation is required.

Sizing hypothesis: about five minutes of focused edits and evaluation; medium
confidence. One focused test's runtime may justify elapsed time beyond target;
other overruns use the existing refinement rule. Depends on slices 1–2.

## Current decisions and execution notes

- Keep one canonical rule and small integration points. No new mandatory
  artifact: inline proof links or a compact table can express the connection.
- This changes workflow instructions, not product architecture; no ADR change
  or API generation is indicated. Keep repository tooling and wrap-up policies.
- A suspected gap is an investigation claim until evidence establishes it.
  Do not present the historical examples as unfixed current product defects.
- Preserve unrelated working-tree changes. At planning time these included
  `PRODUCT-BACKLOG.md`, SEED-009, and SEED-011; recheck ownership on execution.
- Preserve SEED-010 as the requested retrospective after this plan is complete;
  keep findings and possible responses there rather than a new story queue.
- Each leaf has one acceptance decision and a bounded proof loop. The multiple
  conditional observations in slice 3 evaluate completeness of one replacement
  contract; they do not authorize separate product changes. No extra refinement
  pass is needed before starting. Enforce the existing five/ten-minute rule.

## Learnings

- Original clone and publication product gaps already have focused corrections
  and boundary assertions. Reuse them as evidence rather than adding historical
  removal tests or changing product code for this instruction improvement.
- Pygardon supplied the lifecycle example; this plan evaluates the instruction
  against that reported scenario without claiming a local Telegram runtime test.
