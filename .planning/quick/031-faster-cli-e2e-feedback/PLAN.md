# Faster CLI E2E feedback

## Source

- Identity: test optimization of the CLI E2E family, requested by the owner on
  2026-09-24 through `/dough-test-optimization for the cli related e2e test`.
  No story or backlog entry; the request is the execution authority.
- Execution: Story Branch Mode, worktree `.claude/worktrees/cli-e2e-speed`,
  branch `test-opt/cli-e2e-speed`, base `origin/main` `337d271899`.

## Goal and scope

A developer running the CLI E2E features waits less for the same behavioral
confidence.

Scope: the 14 active (non-`@ignore`) features in `e2e_test/features/cli/`, their
CLI support code, and the CLI checkout-readiness check that every pull and
publish runs. Excluded: the `@ignore` interactive/Gmail features, the
`@skipOptimizationDueToKnownNecessarySlowness` existing-note-edits feature, the
shared `I am logged in as an existing user` step (whole-suite step), and
Mountebank/SUT startup.

Preserved promises: every user journey the family documents (install, clone,
publish, pull, web↔local reconciliation, moves, renames, trash, LFS, history
reset) keeps at least one integrated E2E path; every retired scenario names its
surviving proof below.

## Baseline

Command (ordinary local mode, 14 active features, comma-separated because the
runner refuses globs):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec "<14 active cli features>"
```

| Revision | Wall | Cypress | Cases | Summed scenario time | Load avg |
|---|---|---|---|---|---|
| `b481ee7de3` (primary checkout) | 5:09 | 4:31 | 51 | 271.2s | — |
| `337d271899` (this worktree) | 5:15 | 4:36 | 52 | 275.5s | ~9 |

Wall minus Cypress (~38s) is SUT startup; scenario bodies are the cost.

## Family analysis

Step-level profile (temporary uncommitted `AfterStep` timing hook, three
features, 12 scenarios, 72.8s of steps):

| Step | Calls | Mean | Share |
|---|---|---|---|
| CLI pull (step) | 12 | ~2.0s | 33% |
| Login (API session + first SPA load) | 12 | ~1.0s | 16% |
| CLI publish | 11 | ~0.9s | 13% |
| CLI clone | 16 | ~0.7s | 15% |

CLI pull, instrumented (4 samples, consistent): whole task ~1.58s; bundle
download ~22ms; the rest is local `git` subprocesses at ~20–25ms each.
`assertLocalMainIsReadyToReceive` runs 3× per pull and spawns 8 gits each
(6 separate `rev-parse --git-path <marker>` lookups + branch + status), ~180ms a
time. `merge --ff-only` ~255ms and `lfs install --local` ~110ms are LFS
filter/config costs, left alone. The E2E pull step also reads full checkout
state (~10 gits) before and after (~350ms); not addressed now.

Redundant journeys (scenario → surviving proof):

- `cli_notebook_folder_relocation` "Publishing a committed folder relocation…"
  (3.7s) → same feature "Publishing relocate then descendant edit and add
  commits…" publishes the same relocation and shows Pasta at the new path.
- `cli_notebook_folder_relocation` "Pulling a clean checkout receives an
  accepted folder relocation" (5.6s) → same "relocate then descendant edit"
  scenario pulls the relocation into the second checkout (exact tree + A-to-C
  history, stronger than ancestor).
- `cli_notebook_web_local_reconciliation` "Pulling a web-created Shopping list
  while retaining a local Pasta edit then publishing" (5.9s) → "…web-created
  then saved Shopping list…" (same rebase/publish path, one more web save);
  title-only pull is in `cli_notebook_web_created_note`; the addition-only
  rebase shape is in `cli/tests/notebookPull.addition.suite.ts`.
- `cli_notebook_web_note_moves` "…from a folder to root…" (6.5s) → backend
  `NotebookGitWebNoteMoveRootNestedControllerTest.nestedFolderNoteMovesToRootViaBareRootEndpoint…`
  proves the root projection; the folder-target and empty-folder scenarios prove
  pull-then-edit-then-publish.
- `cli_notebook_web_note_renames` "Pulling a web note rename into a clean
  checkout" (5.3s) and the `UPDATE_VISIBLE_TEXT` example (4.3s) → the
  `KEEP_VISIBLE_TEXT` rename-with-reference journey (rename pull + rewritten
  reference); both choices' rewrites are in backend
  `TextContentControllerUpdateNoteTitle*WikiReferencesTests`, and
  `NotebookGitDerivedTreeOracleControllerTest` projects the rewrite into Git.
- `cli_notebook_web_folder_moves` "Pulling a web folder move places the folder
  exactly once…" (5.1s) → "Pulling successive web folder moves…" pulls the same
  move (and one more); **retire only if** backend
  `NotebookGitWebFolderMoveControllerTest` proves the moved subtree's bytes and
  the destination's `.keep` removal; otherwise keep it.

Not redundant (kept): web-trash's two recoveries (web vs local), the four
publish-to-clean-clone journeys, web-created nested authoring, LFS journeys.

## Hypotheses and experiments

Each experiment: focused baseline → smallest change → proof → focused re-time →
retain/undo decision.

### 1. Retire CLI journeys whose proof survives elsewhere
Type: Structure
Status: done
Proof: the edited features pass under
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec <edited features>`; each
retired scenario's surviving proof (above) is inspected, not assumed.
Expected saving: ~31s (≈36s if the folder-move scenario also goes).

Change: delete the listed scenarios; turn the renames outline into a plain
`KEEP_VISIBLE_TEXT` scenario. Remove step definitions/page-object methods left
without callers.

Result: all seven retired (the folder-move condition held:
`NotebookGitWebFolderMoveControllerTest.webFolderMoveAppendsAcceptedChildAndRetainsNoteAndLearningIdentity`
asserts the exact path set `Study/Biology/Cells.md` and the moved bytes). The
rename scenario now uses the existing "keeping visible reference text" step and
the `(KEEP_VISIBLE_TEXT|UPDATE_VISIBLE_TEXT) reference handling` regex step was
deleted. Focused: the five edited features went 109.4s/18 cases → 69.0s/11
cases (edited-features `cy:run`, 11/11 pass; renames re-run 1/1 after refactor).

### 2. Readiness check resolves Git operation markers in one call
Type: Structure (behavior-neutral CLI speedup)
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm -C cli test` (readiness suites,
including active merge/rebase refusals, stay green); focused E2E re-time of
`cli_notebook_web_note_moves.feature` pull step (~1.58s task before).
Expected saving: ~0.33s per pull, ~0.1s per publish/clone ⇒ ~15–20s family.

Change: `cli/src/commands/notebook/notebookCheckoutReadiness.ts`
`gitOperationIsActive` asks `git rev-parse --git-path A --git-path B …` once
instead of once per marker.

Result: `pnpm -C cli test` 466 passed, including the pull/publish readiness
refusals (active merge, unfinished rebase). Isolated measurement: one check
57.5ms/6 spawns → 9.5ms/1 spawn, ~145ms per pull. Folding the branch read into
the same call was rejected (fails differently on an unborn repo).

### 3. Re-profile
Type: Structure
Status: done
Proof: full baseline command above, same 14 features; report wall, Cypress
time, cases before/after.

| Run | Wall | Cypress | Cases | Summed | Load avg |
|---|---|---|---|---|---|
| before (`337d271899`) | 5:15 | 4:36 | 52 | 275.5s | ~9 |
| after (this change) | 4:09 | 3:40 | 45 | 219.9s | ~5 |

The 44 scenarios present in both runs: 233.3s → 215.4s (−7.7%), consistent with
the readiness saving but partly flattered by lower machine load; the retired
cases accounted for 42.2s.

## Current decisions

- Login step and SUT startup are out of scope (shared across the whole suite).
- LFS `merge --ff-only`/`lfs install` costs are product LFS behavior; not
  changed here.

## Learnings

- The E2E runner refuses glob spec selections; list features comma-separated.
- `--reporter json` is not honored through the runner; per-scenario times come
  from the spec reporter's `✓ … (Nms)` lines.
- Remaining untried cost recorded as a candidate in
  `.planning/test-optimization-candidates.md` (E2E pull step's before/after
  full checkout-state reads).
