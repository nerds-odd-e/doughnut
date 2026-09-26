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
`vue-tsc`). The coordinator’s earlier delivery step, `format:changed`, selects
components from unstaged and untracked files too, and the frontend `format`
script also runs a whole-tree `vue-tsc`, so it can stop on another slice’s
unfinished types before the commit is even tried.

## Alternatives and Decision

Queue a correction to the commit gate. Committing only when every agent in the
checkout has stopped is the current workaround, and it removes the benefit of
parallel slices. Requiring one worktree per concurrent slice would work around
the gate at the cost of setup and merging for every parallel slice, and would
not help a contributor with unrelated unstaged edits. Dropping type checking
from the gate would reopen the problem DD-073 fixed. Checking the tree as it
will be committed keeps the gate as strict as it is now for the committed
content.

Refinement decisions (owner, 2026-09-26):

- The hook checks a temporary copy of the index, always, not only when the
  working tree differs from it. Stash-style approaches are rejected: they
  change the working tree while other agents are writing to it.
- Checking only the staged files is rejected: type checking needs the whole
  program as it will be committed, and a staged file may depend on an export
  that exists only in another slice’s unstaged file.
- The frontend `format` script stops running `vue-tsc`. Frontend proof has
  required `vue-tsc` since the DD-073 correction, the hook still type-checks
  the committed content, and CI runs it again in `lint:all` and
  `frontend:build`. No finding since that correction shows the format step
  catching a type error that proof missed; the one type error that reached CI
  since then (`1af34728d9`, TS2614) reached CI while that step was already
  in place.

## Story Decomposition

<a id="story-1"></a>

### Commit a finished slice while other slices are still in progress

**Identity:** SEED-043#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/045-commit-gate-checks-committed-content/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"10f13b47735834fb9aed49052c53e3c61041a966011ac97c01ab305199fd8e7a","plan":"2745b28d06f3efde13f7a6c6af39f4d9071d52cb4a663d77f710106f7c9986b4"}}
```

- **Slice plan:** [Commit a finished slice while other slices are still in progress](../quick/045-commit-gate-checks-committed-content/PLAN.md)

- **Goal:** A coordinator or contributor can format and commit a finished,
  staged frontend change while other agents’ unfinished edits sit unstaged or
  untracked in the same checkout, so parallel slices are delivered as each one
  finishes. The committed content is checked as strictly as today.
- **Scope:**
  - When the staged changes select the frontend, the pre-commit gate runs the
    frontend’s Biome check and `vue-tsc` against a temporary copy of the index
    (the last commit plus staged changes), not the working tree. It does this
    on every such commit.
  - The hook still leaves the working tree and index unchanged, still selects
    components from staged files, and still works in worktrees.
  - The frontend `format` script runs Biome only; `vue-tsc` leaves it.
    Formatting behaviour is otherwise unchanged.
- **Excluded:**
  - Backend, CLI, mcp-server, test-fixtures, root and OpenAPI checks: they keep
    checking the working tree. Neither DD-121 failure involved them.
  - Which files `format:changed` selects, and Biome rewriting other slices’
    files while formatting; neither was observed failing.
  - The checks CI runs.
  - The shared Open Dough rule allowing concurrent slices, and the other
    shared-checkout findings (ODF-081 to ODF-084, DD-118), which have other
    causes.
  - Making `vue-tsc` faster or caching it for the copy.
- **Key examples:**
  1. A finished spec is staged; another slice’s unstaged spec has a Biome
     formatting error (DD-121: `RichMarkdownEditor.propertyEntry.spec.ts`)
     → the commit succeeds.
  2. Another slice’s unstaged spec has `TS2305 … has no exported member
     'mountMarkdownTextarea'` (DD-121:
     `NoteEditableContent.debouncedSave.spec.ts`) → the commit succeeds, and
     `format:changed` no longer stops on it either.
  3. A staged file has a type error or a formatting error → the commit is
     rejected, as today (DD-073).
  4. A staged change removes an export that an unchanged committed file uses
     → the commit is rejected.
  5. A staged file imports an export that exists only in an unstaged file
     → the commit is rejected, although the working tree type-checks.
  6. After the hook runs, pass or fail, the working tree and index are exactly
     as before, and no temporary copy is left behind.
- **Value / learning:** Removes the demonstrated delivery bottleneck for
  parallel slices and removes a duplicate type check from formatting.
- **Cost (measured 2026-09-26):** copying the index took 0.8 s, Biome on the
  copy 0.8 s, and a cold `vue-tsc` on the copy 19.5 s, against 2–7 s for a warm
  `vue-tsc` in the working tree: about 15–20 s more per frontend commit, which
  the owner accepted. The copy type-checked without untracked generated files.
- **Effort hypothesis:** S (30–90 minutes), medium confidence now that the cost
  is measured.
- **Depends on:** None.
- **Safe stopping point:** The hook change alone fixes both DD-121 failures at
  the commit; the `format` change alone stops the formatting step from failing
  on another slice’s types.

## Ordering and Scope Reduction

One selected story, queued first on owner instruction to prioritize the top
project retrospective findings. DD-121 is the only unresolved project finding:
one execution (quick/041, 2026-09-26) with two failed commits, and the only
execution in history that ran slices in parallel in one checkout. Its likely
recurrence is test-optimization work with many file-disjoint specs; together
with the measured small cost, that keeps it first. Scope is the frontend
because both observed failures were there.

## Open Decisions

None.

## Breadcrumbs

- [Project finding and retained occurrence](../../DonutRetrospectiveFindings.md#dd-121).
- DD-120 (`DearDough.md`) records the same execution; its failed commit is the
  same Biome failure as DD-121’s first one.
- `1e6e69cc64`: made the pre-commit hook lint-only; the last change to the gate.
- `7b1d80b4e8`: DD-073 correction; frontend proof must still typecheck.
- `1af34728d9`: the one type error that reached CI after the DD-073 correction.
- The preparation announcement’s own commit took minutes in a fresh worktree,
  because a staged agent-profile `.json` selects the root component, whose lint
  runs an install and a whole-root Biome check; left out of scope.
- Checked against `7b05bb9df8`; no matching queued or taken story exists.
