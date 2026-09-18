# E2E top-5%-slowest test optimization

## Source

Request: `/dough-test-optimization for top 5% e2e test`. Additional in-session
instruction: fix any failing e2e tests surfaced while profiling.

## Goal and scope

Shorten the local E2E feedback path (`pnpm cy:run` over the full admitted
81-spec suite) by removing evidenced avoidable cost in the slowest ~5% of
specs, while preserving behavioral coverage. Out of scope: the six
non-admitted specs (interactive CLI, `cli_gmail`, live-OpenAI, `epub_book`),
which this project's isolated-checkout selection already excludes from this
local profiling scope.

## Baseline

Command: `pnpm cy:run --spec <81 admitted feature files>` (no working `--reporter`/
`--expose` override exists in `scripts/e2e-runner.mjs`; it only forwards
`--spec`/`--browser`, so timing is per-spec, not per-scenario). Revision:
`5c84f205b6` (after the correctness fix below). Environment: primary checkout,
isolated worktree identity `wt_d9db2098469f4feda1a05f2e1216828a`, MySQL/Redis
already running.

Result: 81/81 specs, 315 tests, 12:55 wall time (315 tests / 775s ≈ 2.46s/test
average).

## Correctness fix (prerequisite, already committed)

The first baseline run surfaced 3 real E2E failures in
`assimilation/note_refinement.feature` (`EntityExistsException: Detached
entity passed to persist` in `AiController.createExtractedNote`). Root cause:
`AuthoredNoteDocumentPersistence.persist()` discarded the managed/merged
`Note` that `EntityPersister.save()` returns for an already-existing
(detached) note, then passed the stale detached reference into
`NoteReferenceService.refreshDerivedIndexesForNote()`, which calls
`entityManager.persist()` directly. Fixed by using the managed instance
throughout `persist()`. Commit `5c84f205b6` (fix +
`AuthoredNoteDocumentPersistenceTest`, both backend and E2E proof green).
This also incidentally cut `note_refinement.feature` from 00:27
(with retries) to 00:14 clean — no longer in the top 5%.

Two further spec durations from the first run (`note_creation.feature`,
`note_deletion.feature`) were contaminated by a mid-run backend hot-restart
triggered by editing production code while the suite was running against the
same live backend. Re-measured cleanly in a follow-up run (all still green):
`note_creation.feature` 00:19, `note_deletion.feature` 00:27.

## Family analysis

Corrected top 5% by spec duration (~top 5 of 81):

| Spec | Duration | Tests | s/test |
| --- | --- | --- | --- |
| `learning_session/commissioned_learning_session.feature` | 00:48 | 15 | 3.2 |
| `cli/cli_notebook_existing_note_edits.feature` | 00:29 | 6 | 4.8 |
| `note_creation_and_update/note_edit.feature` | 00:27 | 12 | 2.25 |
| `note_creation_and_update/note_deletion.feature` | 00:27 | 13 | 2.08 |
| `note_topology/wiki_link.feature` | 00:25 | 11 | 2.27 |

`note_edit.feature`, `note_deletion.feature`, and `wiki_link.feature` sit at
or below the 2.46s/test suite average — they rank in the top 5% by *file*
duration purely because they hold more scenarios than most files, not because
any scenario is disproportionately costly. No redundant setup or duplicated
proof found in their Backgrounds. Not pursued further; legitimate density.

`commissioned_learning_session.feature` (15 tests, one Background, two
`Scenario Outline`s contributing 7 of the 15 executed tests) is ~30% above
average. Its Background is minimal (one notebook, three notes); each scenario
exercises a distinct commissioned-tracker/tutor-feedback path that can't share
more setup without losing the behavior it proves. No evidenced waste; the
elevated cost is proportional to real scheduling-algorithm and
feedback-parsing variation coverage. Recorded as a candidate, not an
experiment.

`cli_notebook_existing_note_edits.feature` (~95% above average) genuinely
issues several real CLI subprocess + git operations per scenario (clone,
commit, pull, rebase-continue, publish — up to 6 in one scenario), which is
the actual protocol behavior under test (ADR-covered CLI git-sync). No
hardcoded `cy.wait(ms)` sleeps exist anywhere in `e2e_test/` (checked
suite-wide). Recorded as a candidate for the file's own per-scenario cost.

**Evidenced cross-file opportunity:** all CLI scenarios installing a fresh
Donut CLI binary from the running backend via
`cli.installation().installFromLocalhost()` →
`cy.task('installCli', baseUrl)`, which fetches `/install` over HTTP and runs
the real install script as a subprocess. `cli_install_and_run.feature`
correctness-tests this mechanism itself. Eleven other `cli_notebook_*.feature`
files use it purely as incidental setup (in their `Background`, so Cucumber
re-runs it before every scenario) to obtain a working `donut` binary before
testing notebook/git behavior unrelated to installation:

| File | Scenarios |
| --- | --- |
| `cli_notebook_clone.feature` | 4 |
| `cli_notebook_existing_note_edits.feature` | 6 |
| `cli_notebook_git_history_reset.feature` | 1 |
| `cli_notebook_folder_relocation.feature` | 5 |
| `cli_notebook_publish_to_clean_clone.feature` | 3 |
| `cli_notebook_web_created_note.feature` | 8 |
| `cli_notebook_web_folder_moves.feature` | 3 |
| `cli_notebook_web_local_reconciliation.feature` | 3 |
| `cli_notebook_web_note_moves.feature` | 4 |
| `cli_notebook_web_trash.feature` | 2 |
| `cli_notebook_web_note_renames.feature` | 2 |

41 scenarios total → 30 redundant installs (41 minus one first install per
file) against the same running backend/base URL for the whole `cy.run`
invocation. `cli_install_and_run.feature`'s own Background measured ~1s per
install+verify cycle (3 scenarios in 00:03), so the redundant-install cost is
real but modest (~30s of the 12:55 baseline, ≈4%).

## Preserved promises

- `cli_install_and_run.feature`'s own scenarios (installed-CLI version,
  interactive session, update-to-newer-version) must keep performing a
  genuinely fresh install/update each time — this is the literal behavior
  under test (ADR/CLI install mechanics). The experiment must not touch its
  step or share its cache.
- All 41 CLI-notebook scenarios keep a working, correctly-configured `donut`
  binary pointed at the running backend.
- No reduction in scenario count, assertions, or CLI/git protocol coverage
  anywhere in this plan.

## Experiments

### 1. Cache the incidental CLI install across scenarios in a run

Type: Structure
Status: done
Expected saving: ~30s of the 12:55 baseline (≈4%), from skipping 30 of 41
redundant install+verify cycles.
Measured: all 12 `cli/*.feature` files together, same 40 tests before and
after, all green. Combined duration 171s baseline → 169s after (≈1%,
`git diff` scoped exactly to the intended files). The ~30s estimate,
extrapolated from `cli_install_and_run.feature`'s own ~1s/scenario average,
overstated the real per-install cost inside a full scenario (clone/commit/
open-page/etc. dominates); the actual redundant-install cost recovered is
closer to ~1-2s per file with 2+ scenarios, and one file
(`cli_notebook_clone.feature`) measured 2s slower, within normal run-to-run
noise at this magnitude (each file is 2-30s; MySQL/browser warmup varies
between separate invocations). Kept because it removes genuinely redundant
subprocess+HTTP work with zero coverage cost, not because of a proven
wall-clock win — reported here as a small, largely noise-level improvement,
not a meaningful speedup.
Smallest meaningful change:
- Add a distinct `Given the CLI is installed from localhost` step (new
  wording, since behavior differs from the existing `When I install the CLI
  from localhost without affecting my system` per e2e-authoring's
  different-steps-for-different-intent rule) that memoizes the resolved
  `donutPath` for a given base URL at the Node task level
  (`e2e_test/config/cliE2ePluginTasks.ts`'s `installCli` process lifetime,
  which already spans the whole `cy.run` invocation) and reuses it on
  subsequent calls instead of re-fetching `/install` and re-running the
  install script.
- Replace `And I install the CLI from localhost without affecting my system`
  with `And the CLI is installed from localhost` in the `Background` of the
  11 setup-only files listed above. Leave `cli_install_and_run.feature`'s
  Background and step untouched.
Surviving proof for removed work: none removed — every scenario still
receives a working, verified `donutPath`; only the redundant fetch+subprocess
execution is skipped. `cli_install_and_run.feature` keeps proving fresh-install
correctness through its own untouched step.
Verification: run the 11 changed files plus `cli_install_and_run.feature`
together (`pnpm cy:run --spec <12 files>`), confirm all scenarios still pass,
and compare their combined duration against the sum of their baseline
durations.

Behavior: Given the isolated backend is already serving the CLI install
script and a prior scenario in the same `cy.run` invocation already installed
it from that same base URL, when a later scenario's `Background` reaches "the
CLI is installed from localhost", then it reuses the already-verified
`donutPath` without an additional HTTP fetch or subprocess install, and the
scenario proceeds identically to today.

## Current decisions

- Do not pursue `commissioned_learning_session.feature` or
  `cli_notebook_existing_note_edits.feature`'s own per-scenario real-process
  cost further: both are evidenced, legitimate integration cost with no
  identified redundant proof. Recorded in
  `.planning/test-optimization-candidates.md`.
- `note_edit.feature`, `note_deletion.feature`, `wiki_link.feature`: no
  action; at/below suite average per-test cost, ranked in the file-level top
  5% only by scenario count.
- Experiment 1 landed despite a smaller-than-estimated measured win (see its
  Measured note): it is a correct simplification (removes real redundant
  work, preserves all coverage) even though the wall-clock evidence for this
  specific suite slice is inconclusive at its magnitude. No further CLI-family
  experiments are planned — the remaining cost in these files is real
  clone/commit/pull/rebase subprocess work, which is the behavior under test.

## Result

Baseline 81 specs / 315 tests / 12:55. Correctness fix
(`AuthoredNoteDocumentPersistence`) delivered separately (commit `5c84f205b6`)
and also removed `note_refinement.feature`'s retry-driven slowness (00:27 →
00:14), dropping it out of the top 5% incidentally. One optimization
experiment delivered (CLI install caching); measured effect on its own
12-file slice was ~1%, smaller than hypothesized. No other redundant work
found in the top-5%-by-duration specs; their cost is proportional scenario
density or genuine real-process (CLI/git) integration cost.
