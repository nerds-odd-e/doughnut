# Cheaper local test feedback

**Source:** [Shorten local test feedback by reducing repeated setup](../../seeds/SEED-039-faster-ci-feedback.md#candidate-resolution)
**Identity:** SEED-039#candidate-resolution
**Instruction:** `dough-test-optimization --resolve`, 2026-10-30. This pass
plans experiments; it authorizes neither profiling nor implementation.

## Goal and scope

Give contributors shorter trustworthy local feedback by addressing three
recorded, avoidable setup costs. Each experiment must improve its ordinary
local command, with equivalent behavioral protection and simpler support code.
No numeric speed target was supplied. Accept an inconclusive or rejected
experiment instead of delivering unmeasured changes.

Include E2E readiness observation, test-owned LFS fixture commits, and the
BookReadingPage reading-panel sibling specs with their shared support. Exclude
shard redistribution, worker increases, product synchronization changes,
production/user Git configuration, disabling browser isolation, built-frontend
substitution for local HMR, and new profile exclusions. Other frontend families
remain discovery inputs only; this plan does not authorize a suite-wide sweep.

## Retained evidence and measurement prerequisites

Inspected source at `bf0bbc01b3b90db7ed4e88b237474e4a7133ba15`, whose product
tree matches admission base `6ad57b7a366e40154c5140e1a79be330213801d8`.
Owned checkout: `/Users/terryyin/git/doughnut/.worktrees/resolve-test-optimization-candidates`,
branch `codex/resolve-test-optimization-candidates`, created for this story.
Originating/integration checkout: `/Users/terryyin/git/doughnut`.
Admission published on `origin/main` at the inspected revision. These checkout
facts describe resolution preparation; future queued execution selects its own
checkout and target during Take. Preparation grants no execution authority.

The [candidate record](../../TEST-OPTIMIZATION-CANDIDATES.md) preserves dated
observations and original evidence links. On 2026-09-25, E2E startup was ~33s
per batch and fixture commits after LFS hook installation cost ~213ms versus
~15ms. On 2026-09-26, cohesive frontend file merges saved ~70–250ms per file
in focused runs; whole-suite runner elapsed means were 49.9s / 39.8s with
1937 / 1900 cases, but comparable command wall totals were unavailable.
These are historical observations, not current baselines or additive savings.

No tests, timing, scratch experiments, or profiles ran during resolution.
Before each change, its owning slice must obtain a successful pre-change
baseline, including command wall time, cases, revision, literal command,
runner mode, worker count, environment, cache/load conditions, and setup versus
test durations. Keep raw output outside Git. Repeat noisy comparisons and
report failed/incomplete runs as diagnostics. No baseline means no dependent
change. Apply existing profile exclusions only to profiling; keep normal
verification coverage intact.

## Family analysis and existing solutions

- **E2E readiness:** `scripts/sut-start-health-wait.mjs` defaults to a 3000ms
  polling gap. `e2e-runner.mjs` already owns a fresh stack and compiles backend
  code once per batch. Reuse its real healthcheck, cancellation, early-exit,
  timeout, and cleanup owners. A shorter bounded cadence may remove up to one
  poll gap, not backend boot or Cypress/spec initialization. Measure healthcheck
  cost before selecting the cadence; avoid busy polling or another supervisor.
- **LFS fixture authoring:** `e2e_test/config/cliE2eNotebookCloneGit.ts` owns
  test-authored commits. `cliE2eNotebookLfsTasks.ts` also creates real LFS
  fixtures; `cliE2eNotebookLfsPublishTasks.ts` and historical-fetch tasks consume
  those checkouts. Standard filters, transfer, pointer history, authorization,
  and hydration require real LFS. Fixture commit hooks do not establish those
  promises. Try a command-scoped `core.hooksPath` override only when authoring
  fixture commits, reusing existing Git helpers. Keep `git add` filters and all
  installed-CLI operations real. Never persist the override in a user checkout.
  The old claim that Donut uses no `git push` does not describe these fixtures:
  standard-client tasks explicitly run `git lfs push --object-id`.
- **Frontend panel:** `BookReadingPage.readingControlPanel.visibility.spec.ts`
  (181 lines) and `BookReadingPage.readingPanelTarget.spec.ts` (112) share
  fixture loading, real PDF mounts, backend-API mocks, and interaction support.
  Preserve every distinct visibility/placement example in one cohesive panel
  suite, using existing support for concise fixtures. Reading-position debounce
  and notebook switching have different responsibilities and stay separate.
  The old `BookReadingPage.snap.spec.ts` observation is historical; current
  support already shares loading/mount/wait responsibilities. No new fixture
  framework or test-registration layer is justified.

## Outside-in proof

| Promise | Owner | Observable proof retained |
| --- | --- | --- |
| Faster readiness without losing ownership or failure reporting | 1 | Real healthcheck success; startup failure/cancel and foreign-process survival through SUT/batch tests; ordinary E2E batch completes and cleans up |
| Faster fixture commits with authentic LFS behavior | 2 | Standard-client round trip, authenticated refusal, exact hydrated bytes, accepted pointer history and explicit historical fetch in `cli_notebook_lfs.feature` |
| Panel behavior survives cohesive spec consolidation | 3 | All READ/SKIMMED load borders, successor/last-content visibility, obstruction, anchored coordinates, and fixed placement after scrolling in mounted-page cases |
| Ordinary local feedback improves, including replacement proof cost | 4 | Comparable complete command wall time and case counts, with all replacements included |

Do not remove a case until its distinguishing signal is mapped to retained or
replacement proof. None is currently proposed for deletion as redundant.
If a spec is relocated, sweep its old path and imports once at acceptance,
separately from behavioral proof; delete obsolete support transitively. Do not
add an assertion that a retired file, helper, or case is absent.

## Ordered slices

### 1. Observe an owned E2E stack promptly
Type: Behavior
Status: planned

Behavior: A fresh owned stack becomes healthy between polls → the runner
observes health promptly → Cypress runs only after real readiness and the
runner retains cancellation, failure reporting, and complete owned cleanup.

Hypothesis: Reduce the current polling gap after measuring healthcheck cost;
expected saving is 0–2.9s per batch, not the historical entire ~33s startup.
Smallest experiment: compare cadences first through the existing
`SUT_POLL_MS` override with no code edit, then change the one shared
`POLL_MS` default without changing the health predicate, deadline, lifetime,
or ownership boundaries. That default also serves `dev-start.mjs` and
`sut-owned-restart.mjs`; keep one cadence for all consumers rather than an
E2E-only value, and confirm Development start still reports readiness.

Proof/measurement: Before and after, time
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`.
Run `CURSOR_DEV=true nix develop -c pnpm test:sut-start`, which includes the
runner lifecycle suite. Retain readiness-failure, timeout, cancellation,
required-child-exit, and foreign-peer observations. Require three consecutive
focused E2E passes for changed asynchronous synchronization.

Decide/reassess: Retain only an ordinary-command improvement without costly
healthcheck churn; otherwise undo this experiment's edits. Record the result
and re-evaluate the remaining startup cost before slice 2; do not weaken fresh
ownership or switch to a built frontend to enlarge the saving.

### 2. Author LFS fixtures without unrelated commit-hook work
Type: Behavior
Status: planned

Behavior: E2E setup has a real LFS checkout → test-owned commit plumbing authors
an attachment change → a real filtered pointer commit is available promptly,
and the installed CLI still publishes and hydrates exact attachment bytes.

Hypothesis: Bypass hooks only on fixture-authoring commit commands; historical
upper estimate is ~198ms per affected commit, with current count/cost unknown.
Capture a fresh baseline first, then try the command-scoped override at the
existing helper. Inspect all its consumers; do not bypass hooks on product
commands, LFS transfers, or standard-client protocol observations.

Proof/measurement: Time the same ordinary `cy:run` LFS feature before/after.
Retain its complete round trip, authorization refusal, current-only download,
multi-version pointer history, web-save, and historical-fetch scenarios.
Also run `CURSOR_DEV=true nix develop -c pnpm cli:test` for CLI compatibility.
If shared commit plumbing affects other features, add those concrete consumer
features to verification and count their replacement cost. Repeat changed
asynchronous E2E paths three times if synchronization changes.

Decide/reassess: If hooks remain necessary for the observed fixture operation,
or the ordinary command does not improve, restore this experiment's edits.
Keep real standard LFS protection; no hook-count or hook-absence test is needed.

### 3. Prove reading-panel behavior with one cohesive browser setup
Type: Structure
Status: planned

Correction: Remove repeated file-level preparation across the two inspected
panel specs while retaining the mounted-page contract and browser isolation.
Hypothesis: One fewer compatible spec may save the historical ~70–250ms file
cost. Current marginal cost and mock/timer compatibility require observation.
This slice directly owns the recorded test-suite setup weakness.

Capture full-suite and focused before timings. Consolidate their setup and
cases using the existing interaction/PDF/panel support; retain each
distinguishing example. Keep every edited file within the 250-line refactor
limit by simplifying cohesive fixtures, not hiding test registration in helpers.
If that cannot stay simple, reject the merge rather than force it.

Proof/measurement: Focused command before/after is
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage`;
normal acceptance also runs `CURSOR_DEV=true nix develop -c pnpm frontend:test`
and both `pnpm -C frontend exec vue-tsc --noEmit` and
`pnpm -C frontend exec tsc --noEmit` through Nix. Include all real-PDF siblings
for shared support changes. Reject the experiment if total feedback does not
improve or any unique behavior is weakened; preserve unrelated work.

### 4. Establish the retained local-feedback result
Type: Behavior
Status: planned

Behavior: A contributor runs each selected ordinary command after retained
experiments → receives the same behavioral confidence with a measured shorter
wait, or an explicit no-gain/inconclusive result.

Proof: Re-run the complete LFS-feature `cy:run` and complete `frontend:test`
commands above against comparable baselines. Include replacement cases and
support costs; alternating comparisons are appropriate when noise dominates.
Report wall time, executed cases, runner/cache/worker conditions, rejected
experiments, and delivered commits. A faster focused spec is insufficient.
Update the candidate record, returning unfinished work to pending. Do not
claim a gain from historical elapsed means, fewer files, or summed durations.
The panel pair does not discharge unexamined sibling families: return those
opportunities under the original `frontend-sibling-spec-setup` slug at closure.
Likewise disposition residual startup and fixture costs from the new evidence;
do not delete a transferred entry just because one experiment finished.

## Current decisions and readiness

- Resolve-only preparation stops here; implementation requires a subsequent
  execution instruction. The same admitted story owns this plan.
- Preserve owner-selected HMR and existing profile exclusions. Accepted costs
  remain in ordinary verification and profiling selection.
- Follow [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  for disposable, checkout-owned state and [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  for authentic Git/LFS history and atomic publication. No new architectural
  decision or North Star change is needed.
- Each slice follows hypothesize → baseline/try/measure → retain or undo →
  reassess. No predetermined speedup or implementation is promised.
- Readiness is **not ready**: current ordinary-command baselines, healthcheck
  cost, real-LFS fixture-hook discrimination, and merged-spec compatibility
  have not been observed. Resolve mode forbids those experiments now; the
  executing coordinator must obtain them before dependent edits and reassess.
- Slice-plan refinement was not needed for these separate family proof loops.
  Expected editing work per experiment is ~5 minutes; repeated normal-command
  measurements are an explicit verification-time exception to the 10-minute
  limit. An oversized implementation still requires finer decomposition.
- Delivery uses Execute Plan: independent fresh refactor, required generation
  only if triggered, coordinator `./scripts/run.sh pnpm format:changed` once,
  evidence update, check-only commit hook, and push. Measure before delivery;
  no experiment is retained merely because tests pass.

## Learnings

2026-10-30: Source inspection supports three bounded experiments, not current
speedup claims. Existing shared Git writers and profile-specific boundaries
make blanket helper/context consolidation unsupported by the retained evidence.
