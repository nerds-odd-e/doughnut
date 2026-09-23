---
name: dough-execute-plan
description: >-
  Executes one selected story or bounded retrospective correction through an
  executable plan, or one authorized planless slice from a selected simple story
  or a contextual instruction, with independent refactoring, delivery, and
  asynchronous CI repair. Use to execute a plan, run slices, execute a canonical
  story when the caller explicitly skips slice planning, or execute a small
  instruction from context without a story or plan. Does not decide story scope
  or quick-path eligibility. `--trunk` selects Trunk Mode; omitted mode keeps
  Story Branch Mode. `--skip-retro` skips the automatic planned-execution
  retrospective. `--replan` and `--no-replan` choose whether an oversized attempt
  may continue through planning.
---

# Execute planned or planless work

Execute the selected work through its existing plan. An explicit current instruction
may instead authorize one planless quick slice: an understood canonical feature story,
or a contextual instruction with no story or plan. The coordinator owns delivery;
implementation agents return uncommitted changes.

## Establish execution context

Identify the execution source before changing project state:

- **Planned:** require an executable plan. For a feature story, read its seed section.
  For a bounded correction, require the complete correction input in the plan under
  [planning scope and lifecycle](../dough-story-refinement/references/planning.md#choose-the-planning-level);
  do not require or create a seed.
- **Quick:** require an explicit current instruction that authorizes planless
  execution. The source is either a canonical feature story with understood goal,
  scope, key examples, and no blocking decision, plus the instruction to skip slice
  planning; or a contextual instruction that supplies the objective, known
  expectations, remaining uncertainty, and authority. A seed alone or apparent story
  size cannot select this path. Corrections require plans. Do not fabricate a story,
  plan, or queue entry when the instruction is otherwise sufficient.

Name missing authority or source context and stop before backlog changes, delegation,
or implementation. Successful quick execution keeps scope, decisions, progress, and
proof in the conversation; create no plan, completion note, or substitute record.
A contextual instruction may leave expectations unresolved. Carry its goal and
uncertainty, resolve what a behavior change requires, and allow an evidenced
no-change conclusion through the explained-empty-change path.

Use [planning level](../dough-story-refinement/references/planning.md#choose-the-planning-level)
for source ownership, [proof ownership](../dough-story-refinement/references/planning.md#own-executable-proof)
when mapping or accepting proof, and [active-plan refinement](../dough-story-refinement/references/planning.md#refine-the-active-plan)
for plan updates. Retain completed plans, source history, and review evidence for
retrospective and [story wrap-up](../dough-story-wrap-up/SKILL.md); quick work retains
its source, conversation, and execution identity. Use [slice decomposition](../dough-story-decomposition/references/problem-decomposition.md#decompose-slices)
for Behavior/Structure types and [slice sizing](../dough-story-decomposition/references/problem-decomposition.md#size-and-escalate-slices)
for budgets and learning escalation.

At startup, obtain enough authoritative context to select and delegate the first slice.
Reuse instructions while their sources and applicability assumptions remain unchanged.
Resolve project context at the first boundary that needs it:

- execution-source kind, slice target, hard limit, and exceptions;
  [replanning permission](references/execution-decisions.md#choose-replanning-permission);
  integration checkout and branch for a queue claim, using the project's configured integration branch or `main` when none is supplied;
  execution mode and location: default Story Branch Mode; `--trunk` or a clear
  equivalent selects Trunk Mode; explicit caller selection uses the current
  branch. Resolve contradictions before changing state. Mode never creates
  execution authority; for planned work, plan path and status vocabulary;
- backlog path and selected entry for work selected from **Backlog list**;
- selective formatter and commit hook contract before taking queued work and its claim commit;
- navigation, focused tests, runtime wrapper, and workflow precedence for the selected slice;
- authorized push destination before delivery; for Story Branch or Trunk Mode, also
  [trunk publication's Preconditions](references/trunk-publication.md#preconditions) before
  selecting the owned workspace and taking queued work, and before publishing a
  queue claim, validated increment, or owned repair — those preconditions resolve
  publication inputs and defer shared-checkout access and preservation to
  [maintain the default checkout](references/maintain-default-checkout.md);
- generation triggers and commands when affected; and
- [refactor context](../dough-post-change-refactor/SKILL.md) before refactor delegation.

Missing context stops its affected boundary, including first-slice delegation when
needed there. Other invoking tools, including GSD, retain this slice delivery contract;
a phase or task cannot replace a slice.

Before first implementation delegation, read [delegation](references/delegation.md)
and the common reassessment/human-judgment rules, [replanning
permission](references/execution-decisions.md#choose-replanning-permission), plus
currently triggered sections of [execution decisions](references/execution-decisions.md).
Before accepting a return, read [proof acceptance](references/wrap-up.md#accept-proof);
before delivery, read [delivery](references/wrap-up.md#deliver-the-change). Before
arming observation, read [CI observation](references/ci-monitor.md) and only the
current host's notification adapter. Arm from the execution checkout against the
authorized target branch; do not wait for CI. Before creating the execution workspace, read
[execution location](references/execution-location.md). Before a queue claim,
validated increment, or owned repair publication, read
[trunk publication](references/trunk-publication.md).
Use [targeted retrieval and disposable research](references/disposable-research.md)
for omitted/truncated passages or bounded investigations; another step alone needs no reload.

## Take queued work

After resolving execution source and authority, inspect the backlog before
plan-status changes, observer recovery/startup, delegation, or implementation.
For Story Branch and Trunk Mode, select or reuse the owned workspace under
[execution location](references/execution-location.md) before the Taken commit,
using fetched remote trunk as the base. Current-branch work uses the checkout
it already recorded and creates no worktree. The claim is a same-branch backlog
commit in that checkout; it needs no backlog Git integration adapter.

Before the Taken commit, resolve selective formatting and that commit's hook
contract. An absent or understood check-only hook permits the transition. An
unknown, mutating, failing, or disputed hook stops it with the queue unchanged
until safely resolved through execution decisions. Resolution runs neither
delivery formatting nor hook-owned lint; obtain push/CI context only when
another current boundary needs it. Keep a workspace already selected when the
hook stops the commit.

For Story Branch or Trunk Mode, also resolve
[trunk publication's Preconditions](references/trunk-publication.md#preconditions) —
the claim's publication authority and authorized push destination — before
selecting the workspace or moving the entry. Reuse permission already
established for this execution. An unresolved precondition stops the move with
the backlog, index, and refs unchanged, and starts no implementation.
[Default-checkout access](references/maintain-default-checkout.md#establish-access-before-local-mutation)
applies when this claim or a later refresh mutates that checkout. Caller-selected
current-branch work keeps its existing contract and gains no new publication
authority here.

For a queued planned feature story, invoke the installed writer from the
product-backlog skill directory in the checkout that holds the claim:

```text
node <installed>/scripts/product-backlog.mjs take --identity <story-id> --plan <plan-path>
```

The writer owns the move and canonical plan link. Follow its
[take contract](../dough-product-backlog/SKILL.md#take-queued-work-for-execution)
and [execution and resume](../dough-product-backlog/references/record-preparation.md#execution-and-resume);
do not construct the entry or record preparation state during Take. A refusal
or ambiguous move stops implementation.

Resume a remote **Taken** entry when retained execution context and publication
provenance agree this execution owns it. Preserve its position without
duplication. Work absent from both active lists needs no fabricated entry.
Planning/refinement never takes work. Leave taken work through pauses,
failures, completion, and retrospective; wrap-up removes it.

For a queue claim, preflight the checkout that will hold the commit: verify its
branch, backlog path, tracked/staged changes, and that the isolated claim commit
is the only staged backlog change. Ambiguous branch or ownership leaves the
queue unchanged; apply
[preserve pending local work](references/maintain-default-checkout.md#preserve-pending-local-work)
rather than stashing, resetting, overwriting, or silently unstaging existing
work. Queued current-branch execution requires the resolved integration branch;
otherwise stop before changing the backlog.

After moving, stage only the backlog path, inspect the staged diff, and commit
the claim in that checkout. Claim setup may record provisional identity;
complete it before dispatch. Staging/commit failure stops isolated execution:
preserve and report backlog/index state and any workspace already selected.
No-change cases produce no empty claim commit.

Story Branch Mode and Trunk Mode both publish that commit's SHA from the owned
workspace per
[trunk publication](references/trunk-publication.md#publish-a-queue-claim)
before implementation; do not invent a second publication procedure for either
mode. Publication may require its defined rebase conflict procedure. Only a
confirmed published revision starts implementation. Prepare required project
commands after that confirmation under
[execution location](references/execution-location.md). Publication and later
preparation failure follow those references: no implementation and no new claim.

## Choose the execution location

Follow [execution location](references/execution-location.md) for mode,
workspace creation, project-command readiness, reuse of host-established
preparation for the selected checkout, retained identity, execution resume, push
destination, and checkout-bound runtime. That reference applies the shared
checkout ownership lifecycle for selection, local checkout role, and target
selection.

## Continue or recover at an execution boundary

Planned work uses the existing plan and conversation under
[execution and resume state](../dough-story-refinement/references/planning.md#write-an-executable-plan).
Quick work uses its source and conversation, without a recovery artifact. Retain the
resolved replanning permission with that context. During uninterrupted
work, reuse decisions, accepted proof, and delivery progress while their sources,
assumptions, and covered boundaries hold. After confirmed push, obtain only newly relevant
next-slice detail; a slice transition alone needs no full recovery read.

After interruption or a changed observation from Git, agents, or observer coverage, verify
execution identity first and reconcile only affected worktree/index, branch/commits,
implementation/refactor return, and exact observer identity. Preserve unrelated or ambiguously
owned work. Reuse proof only while promise, boundary, implementation, setup, and observations match.

Resume at the first delivery obligation not established by evidence. An incomplete
or oversized return still needs [oversized-slice handling](references/oversized-slice.md)
before proof acceptance. Otherwise implementation returns still need proof
acceptance/refactoring; completed refactors need remaining delivery;
uncommitted plan edits need staging/commit. Classify an interrupted claim,
increment, or repair with
[interrupted publication](references/trunk-publication.md#resume-an-interrupted-publication)
before any further commit or push. Plan status or a compact report proves none
of those later boundaries. When pushed commit, retained delivery result, and
required registration agree, select the next dependency-ready slice.
Missing/contradictory execution identity requires the recovery decision above.

## Execute the next slice

1. In the execution checkout, use the plan's current statuses, decisions, learnings,
   proof, and selected existing-solution finding/evidence when present. Read retained state
   on initial entry/recovery; reuse valid readings during continuation. For quick work,
   reread the source and conversation's scope, decisions, progress, proof, and remaining
   uncertainty; it is the only slice, with no separate state artifact. Confirm current
   execution authority. Recover an existing CI observer before considering a new one.
2. Select the next unfinished dependency-ready planned slice, or the one quick slice. Apply
   [execution decisions](references/execution-decisions.md); for behavior/state removal or
   disablement, also run the [destructive later-outcome check](references/destructive-later-outcome-check.md).
3. When planned refinement is needed and learning escalation permits, invoke
   [slice-plan refinement](../dough-slice-plan-refinement/SKILL.md) in place, then restart
   at step 1, unless replanning is disabled; then apply
   [oversized-slice decisions](references/oversized-slice.md)
   and stop without retry. If quick work no longer fits one coherent slice, apply
   [oversized-slice decisions](references/oversized-slice.md).
   A no-replan return stops without planning or retry. When replanning is allowed, use
   [ordinary slice planning](../dough-slice-planning/SKILL.md) for remaining work,
   and restart as planned execution. Before
   delegating a change that invalidates a required
   pre-change observation, apply [proof ownership](../dough-story-refinement/references/planning.md#own-executable-proof):
   reuse an adequate baseline with known matching revision/environment/selection conditions,
   or obtain it first. Missing/failed prerequisites stop only dependent work. Apply on entry
   and resume without a new startup audit or repeated recovery read. Otherwise delegate
   under [delegation](references/delegation.md).
4. On return, recheck execution decisions; handle incomplete/oversized work there before
   delivery, including a no-replan overrun. Otherwise [accept proof](references/wrap-up.md#accept-proof) and confirm
   uncommitted work or an explained empty change.
5. Run [delivery](references/wrap-up.md#deliver-the-change) end to end. That
   delivery publishes through
   [increment and repair publication](references/trunk-publication.md#publish-an-execution-increment-or-repair).
   After successful delivery, restart for remaining planned slices; a delivered
   quick slice has no successor.

Planned slices may run concurrently only with disjoint file changes, mutable state, and
plan writes. Quick execution has one slice. Each slice completes coordinator-owned delivery
before a dependent slice starts.

## Finish or stop

Follow [finish or stop](references/finish-or-stop.md) for the completion
operation, reporting and its markers, automatic retrospective invocation, and
incomplete-work reporting.
