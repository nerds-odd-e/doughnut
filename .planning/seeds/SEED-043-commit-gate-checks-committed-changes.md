---
id: SEED-043
status: dormant
planted: 2026-09-26
planted_during: owner-requested retrospective finding triage and backlog prioritization
trigger_when: selecting the highest-priority unresolved project retrospective finding
scope: small
---

# SEED-043: Commit gate checks what is being committed

## Why This Matters

Donut’s execution may run file-disjoint slices at the same time in one
checkout. [DD-121](../../DonutRetrospectiveFindings.md#dd-121) records two
failed commits of a finished slice: the pre-commit gate rejected it for another
slice’s unformatted file and for type errors in a slice still being refactored,
none of which were staged. Finished work waited for unrelated agents, turning
parallel implementation into serialized delivery.

`scripts/git-hooks/pre-commit` runs `pnpm lint:changed`;
`scripts/quality_changed.sh` selects components from staged files and then runs
each selected component’s whole-tree lint (for the frontend, Biome and
`vue-tsc`).

## Alternatives and Decision

Queue a correction to the commit gate. Committing only when every agent in the
checkout has stopped is the current workaround, and it removes the benefit of
parallel slices. Requiring one worktree per concurrent slice would work around
the gate at the cost of setup and merging for every parallel slice, and would
not help a contributor with unrelated unstaged edits. Dropping type checking
from the gate would reopen the problem DD-073 fixed. Checking the tree as it
will be committed keeps the gate as strict as it is now for the committed
content. The mechanism (for example, checking a temporary checkout of the index)
is left for refinement. No implementation is authorized by this backlog entry.

## Story Decomposition

<a id="story-1"></a>

### Commit a finished slice while other slices are still in progress

**Identity:** SEED-043#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Coordinators and contributors can commit finished, staged work
  while unrelated unfinished edits remain in the same checkout, so parallel
  slices can be delivered as each one finishes.
- **Outcome and scope:** The pre-commit gate passes or fails based on the
  content being committed: staged changes together with the committed tree,
  not unstaged or untracked files. Keep the current component selection, the
  lint-only hook that leaves the working tree and index unchanged, worktree
  support, and the same formatting and type checks for committed content.
  `format:changed` and the checks CI runs are out of scope.
- **Evaluation:** With a clean staged change and an unrelated unstaged file
  that has a format error or a type error in the same component, the commit
  succeeds. A staged file with the same error is still rejected. A staged change
  that breaks types in an unchanged committed file is still rejected. The
  working tree and index are the same after the hook as before it.
- **Value / learning:** Removes a demonstrated delivery bottleneck for parallel
  slices and learns how cheaply `vue-tsc` and Biome can check an index snapshot.
- **Effort hypothesis:** S (30–90 minutes), low confidence until the cost of
  type-checking an index snapshot is measured; resplit if the frontend and other
  components need different mechanisms.
- **Depends on:** None.
- **Safe stopping point:** A frontend-only correction is independently useful,
  because the frontend was the component that failed.

## Ordering and Scope Reduction

One selected story, queued first on owner instruction to prioritize the top
project retrospective findings. The evidence is one execution with two failed
commits, not repeated occurrences; its impact is contributor delivery time, not
product behavior. No second unresolved project finding exists. If refinement
finds that checking an index snapshot makes each commit noticeably slower,
reassess against the SEED-035 product stories before expanding scope.

## Open Decisions

How to check the committed tree without touching the working tree or index,
and whether a slower hook is acceptable, need refinement.

## Breadcrumbs

- [Project finding and retained occurrence](../../DonutRetrospectiveFindings.md#dd-121).
- `1e6e69cc64`: made the pre-commit hook lint-only; the last change to the gate.
- `7b1d80b4e8`: DD-073 correction; frontend proof must still typecheck.
- Checked against `7b05bb9df8`; no matching queued or taken story exists.
