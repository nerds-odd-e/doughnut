---
name: dough-execute-plan
description: >-
  Executes one selected story or bounded retrospective correction through an
  executable plan, or an explicitly selected simple story as one quick slice
  without a plan, with independent refactoring, delivery, and asynchronous CI
  repair. Use to execute a plan, run a plan, execute slices, or execute a
  canonical story when the caller explicitly skips slice planning; does not
  decide story scope or whether work qualifies for the quick path.
---

# Execute planned or quick story work

Execute one selected story or bounded retrospective correction through the
slices in its existing plan. When the current instruction explicitly selects a
canonical feature story for execution without slice planning, instead treat that
understood story as one quick slice. Ordinary invocation still requires an
executable plan. The coordinator owns delivery; implementation agents return
uncommitted changes.

## Establish execution context

Identify the execution source before changing project state:

- For ordinary planned execution, require the executable plan and identify
  whether its source is a selected feature story or a bounded retrospective
  correction. For a feature story, read the story in its seed. For a correction,
  require the complete correction input defined by [planning scope and
  lifecycle](../dough-story-refinement/references/planning.md#choose-the-planning-level)
  in the plan itself; do not require or create a seed.
- For quick execution, require both an explicit current instruction to execute
  without slice planning and the canonical selected feature story. Read that story and
  require an understood goal, scope, and key examples with no unresolved
  decision blocking execution. The instruction supplies execution authority;
  the story supplies the single-slice contract. A seed alone is never an
  execution instruction. Do not use this path for a bounded correction.

Name missing authority, story context, plan, or correction input and stop before
backlog changes, delegation, or implementation. Do not infer quick selection
from a story's apparent size. A successful quick execution creates no plan,
completion note, or substitute execution record; retain its scope, decisions,
progress, and proof in the conversation.

Use [planning scope and
lifecycle](../dough-story-refinement/references/planning.md) for source ownership
and proof, and for plan updates when a plan exists. Keep a completed plan and
its review evidence for retrospective and
[dough-story-wrap-up](../dough-story-wrap-up/SKILL.md); do not delete the
completed plan or its source history here. For quick execution, retain the
canonical story and conversation as the later review inputs. Use
[slice decomposition](../dough-story-decomposition/references/problem-decomposition.md#decompose-slices)
for Behavior and Structure slices, sizing, and learning escalation.

Resolve from this project:

- execution-source kind, slice target, hard limit, and exceptions; for planned
  execution also resolve the plan path, slice-status vocabulary, and whether
  Story Branch Mode applies or the caller selected execution on the current branch;
- product backlog path and selected entry when this work was selected from
  **Backlog list**;
- navigation, focused test commands, runtime wrapper, and workflow precedence;
- selective formatting command, commit hook contract, and authorized push destination;
- generated-artifact triggers and commands when affected; and
- context required by [dough-post-change-refactor](../dough-post-change-refactor/SKILL.md).

Name missing context and stop the affected work before delegation. When another
execution tool such as GSD invokes this skill, keep the same slice delivery
contract. Do not treat that tool's phase or task as an alternative to a slice.

Read [execution decisions](references/execution-decisions.md),
[delegation](references/delegation.md), and [wrap-up](references/wrap-up.md)
before implementation. Read [CI observation](references/ci-monitor.md) before
the first push and load only the current host's notification adapter. For a
bounded investigation, use [disposable research](references/disposable-research.md).

## Take queued work

After resolving the execution source and current execution authorization, inspect
the product backlog before changing plan status, recovering or starting a CI
observer, delegating, or implementing. When the selected work is under
**Backlog list**, follow
[dough-product-backlog](../dough-product-backlog/SKILL.md#take-queued-work-for-execution)
to move its existing entry to **Taken**. This backlog update is execution's
first project-state change. Stop before implementation if the queued entry
cannot be moved unambiguously.

An entry already in **Taken** means execution is resuming; do not duplicate or
reorder it. Work absent from both active lists was not selected from the
backlog; do not fabricate an entry. Refinement and planning do not invoke this
transition. Leave taken work there through pauses, failures, completion, and
retrospective; story wrap-up owns completed-work removal.

For planned execution in Story Branch Mode, perform a read-only preflight in the
originating checkout before moving the entry. Resolve its current branch and
backlog path, inspect tracked and staged changes, and confirm that the exact
backlog transition can be committed without including or disturbing unrelated
work. If branch identity or change ownership is ambiguous, stop with the queue
unchanged. Do not stash, reset, overwrite, or silently unstage existing work.

After the transition, stage only the backlog path, inspect the staged change,
and commit the Taken transition locally on the originating branch as a
Taken-only commit. Do not push this commit separately. Create the
execution branch or worktree only after that commit succeeds, based on the
committed claim. If staging or commit fails, do not begin isolated execution;
preserve and report the actual backlog and index state. If later worktree setup
fails, leave the committed entry in **Taken** for a retry. The no-change cases
above produce no empty Taken-only commit.

## Choose the execution location

Planned execution uses Story Branch Mode unless the caller explicitly selects
the current branch. Story Branch Mode gives the selected story or bounded
correction its own execution branch and Git worktree. After its claim is
committed, create that branch and worktree from the claim commit before
delegation. When backlog inspection established that no claim applies, use the
verified current HEAD instead.
Resolve names and location from this project's conventions and the current
host's ordinary Git facilities; do not invent a parallel registry,
configuration format, or worktree manager. If a required convention or safe
location cannot be resolved, or creation fails, stop with the claim and any
created resources intact and report their actual state.

Retain one planned-execution identity in the existing plan and conversation.
Its selected execution location is the checkout and branch used for all slice
work:

- the originating checkout and its branch, where the claim was recorded;
- the execution checkout and branch used for implementation and delivery; and
- the integration target established by the caller or this project, defaulting
  to `main` only when neither establishes another target.

Record that identity after successful setup and before delegation. For an
explicit caller-selected current-branch execution, record the current checkout
and branch as both the originating and execution location; do not create an
execution worktree. Quick execution also stays in the current checkout and
branch. Never extend the planned-execution default to the planless quick path.

On resume, do not treat **Taken** alone as a location identity. Read the retained
identity and verify it against actual Git branch, HEAD ancestry, and worktree
state. Reuse the identified execution checkout when it still matches. If the
identity is missing, ambiguous, contradictory, or points to an unsafe or
partially created setup, preserve all resources and report the exact recovery
decision needed; do not guess, create a nested worktree, or silently select a
different branch.

Run delegation, post-change refactoring, generators, formatting, staging,
commits, normal pushes, and CI repair from the selected execution location.
Push the execution branch to the authorized destination in Story Branch Mode. Pass
the planned-execution identity and selected location explicitly whenever
handing work to another agent or host adapter so that a tool's own default
working directory cannot redirect the execution. For quick execution, pass the
selected current checkout and branch from the conversation instead.

## Execute the next slice

1. In the selected execution checkout, for planned execution read the plan's
   current slice statuses, decisions, learnings, and proof and use it as
   execution and resume state, including the selected existing-solution finding
   and its evidence when the plan contains one. For quick execution, reread the
   canonical story and the conversation's current scope,
   decisions, progress, and proof; the story is the only slice. Do not create or
   update a separate state artifact. Confirm the current instruction still
   authorizes execution. Recover an existing CI observer before considering a
   new one.
2. For planned execution, select the next unfinished slice whose dependencies
   are complete. For quick execution, select the canonical story as the one
   unfinished slice. Apply
   [execution decisions](references/execution-decisions.md). For a slice that
   removes or disables behavior or state, also run the
   [destructive later-outcome check](references/destructive-later-outcome-check.md).
3. For planned execution, if refinement is needed and learning escalation
   permits it, invoke
   [dough-slice-plan-refinement](../dough-slice-plan-refinement/SKILL.md) on the
   same plan, then restart at step 1. If a quick attempt no longer fits one
   coherent slice, follow the quick-attempt transition under
   [execution decisions](references/execution-decisions.md#refine-an-oversized-slice):
   stop the attempt safely, then use
   [ordinary slice planning](../dough-slice-planning/SKILL.md) for its remaining
   work and restart at step 1 as planned execution. Otherwise delegate under
   [delegation](references/delegation.md).
4. On return, recheck execution decisions. For an incomplete or oversized slice,
   follow that reference before delivery. Otherwise inspect the proof under
   [wrap-up](references/wrap-up.md#accept-proof) and confirm uncommitted work
   or an explained empty change.
5. Run [wrap-up](references/wrap-up.md#deliver-the-change) end to end. After a
   successful push, resume at step 1 for remaining planned slices; the delivered
   quick slice has no further slice.

Run planned slices concurrently only when their file changes, mutable state, and
plan writes do not overlap. A quick execution has exactly one slice and is not
concurrent. Each slice completes its own coordinator-owned delivery before a
dependent slice starts.

## Finish or stop

On completion, a stop requiring human judgment, or cancellation, close the CI
observer through the current host adapter. Handle delivered failures, then
stop observers without waiting for CI. Report pending CI as unobserved.

When all planned slices are done, leave the completed plan and review evidence
in place for retrospective and story wrap-up. Retain a planned execution's
recorded checkout and branch through pauses and completion; do not invoke
retrospective or story wrap-up automatically and do not remove its worktree or
branch here. When the quick slice is done,
leave its story and conversation available instead. Report completed work,
retained evidence, and observer shutdown. End with `## PLAN EXECUTION COMPLETE`
for planned execution or `## QUICK EXECUTION COMPLETE` for quick execution only
after required delivery and shutdown succeed.

Otherwise report the execution source, the active plan and next unfinished slice
or the canonical story and current quick-slice state, preserved work and observer
state, and the decision or recovery action needed. Do not emit a completion
marker.
