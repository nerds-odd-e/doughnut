# Cheaper local test feedback

**Source:** [Shorten local test feedback by reducing repeated setup](../../seeds/SEED-039-faster-ci-feedback.md#candidate-resolution)
**Source review:** [source-review.md](./source-review.md)
**Identity:** SEED-039#candidate-resolution
**Instruction:** first planned by `dough-test-optimization --resolve` on
2026-10-30 (no measurement); refreshed by story refinement and slice planning
on 2026-11-01 with fresh premise observations. Planning grants no execution
authority.

## Goal and scope

Give contributors shorter trustworthy local feedback by removing three small,
avoidable setup costs. Each experiment keeps a change only when its ordinary
local command improves under comparable conditions, with the same behavioral
protection and simpler or equal support code. No numeric speed target was
supplied; a measured no-gain result is an acceptable outcome.

Included: the readiness polling cadence of owned stacks, test-owned LFS fixture
commits, and the BookReadingPage reading-panel visibility and target specs.
Excluded: other startup costs (backend boot, Gradle check, Cypress launch,
database provisioning), shard or worker changes, product synchronization,
production or user Git configuration, browser isolation, built-frontend
substitution for local HMR, new profile exclusions, and other sibling-spec
families. Costs found there go back to the candidate record.

## Fresh observations (2026-11-01)

Workspace `/Users/terryyin/git/doughnut/.worktrees/shorten-local-test-feedback-by-reducing-repeated`,
revision `ee6ecf2549`, macOS arm64 laptop, Nix shell, git-lfs 3.7.1 in Nix.
Raw output stayed outside Git. These settle the plan's premises; they are not
the slices' before/after baselines, which each slice takes itself.

| Premise | Observation | Result |
| --- | --- | --- |
| Readiness polling waits for an avoidable gap, and healthchecks are cheap enough to poll more often | `CURSOR_DEV=true nix develop -c bash -c 'SUT_POLL_MS=<ms> pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature'`, timestamped, alternating 3000/500/3000/500 after one cold run | All runs 10/10 passing. "Starting SUT" → "SUT healthy": 3000ms: 31s (9 polls), 23s (7 polls); 500ms: 27s (22 polls), 17s (16 polls), plus 13s (15 polls) in a separate warm run. With 500ms, average time per poll was ~0.6–1.2s, so a healthcheck during boot costs well under 1s. Cypress run time varied 35s–1m between runs. The cold first run took 4m18s overall (18 polls), so cold runs are not comparable with warm ones. |
| Git LFS still installs hooks that slow fixture commits, and a command-scoped bypass still writes real pointers | Scratch repository in the Nix shell: `git lfs install --local --skip-repo`, `git lfs track '*.bin'`, then six timed commits with hooks and six with `-c core.hooksPath=/dev/null`, then one `.bin` commit each way | `--skip-repo` installed no hooks; the next `git lfs` command added `post-checkout post-commit post-merge pre-push`. Hooked commits took 314–1035ms (typically ~400ms); bypassed commits 71–223ms. A `.bin` commit with the bypass still stored `version https://git-lfs.github.com/spec/v1` (a real pointer). |
| Fixture commit plumbing and its consumers | Source reading | Test-owned commits come from `commitCheckout`/`amendCheckout` (`e2e_test/config/cliE2eNotebookCloneGit.ts`, reached through `cliE2eNotebookCloneCommitTasks.ts`), `commitWithE2eAuthor` (`cliE2eNotebookLfsHelpers.ts`, used by the LFS publish and historical-fetch tasks), and the `lfs payload` commit in `cliE2eNotebookLfsTasks.ts`. They repeat the E2E author identity in two shapes. Consumers are the CLI notebook features `e2e_test/features/cli/cli_notebook_*.feature`. |
| The two panel specs can share one setup | Scratch file joining both specs with merged imports beside the originals, run with `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/pages/BookReadingPage`, then deleted | Both specs use the same `beforeAll` (`loadBookReadingPageFixtures`) and `beforeEach` (`vi.restoreAllMocks`, `mockBookReadingPageDefaults`), with no `vi.mock` or fake timers. The merged file passed 11/11 (76ms). The originals take 62ms (8 cases) and 40ms (3 cases). A plain join is ~280 lines, over the 250-line limit, so the target spec's local mount helper has to move into the existing panel support. |
| Focused frontend command | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/pages/BookReadingPage`, twice | 6 files, 39 tests, 10.0s and 7.6s wall; Vitest reported 5.6s/5.2s, mostly import and setup. |

Earlier dated observations (2026-09-25: ~33s E2E startup, ~213ms vs ~15ms
hooked commits; 2026-09-26: ~70–250ms per removed spec file) stay in the
[candidate record](../../TEST-OPTIMIZATION-CANDIDATES.md) as history.

Consequences for the slices: every saving here is small (≤3s per stack start;
~0.2–0.3s per fixture commit; one spec file's setup) and the machine's noise is
larger than any single saving. Each slice therefore measures with alternating,
repeated runs of its ordinary command and accepts "no measurable gain" as a
real result.

## Outside-in proof

| Promise | Owner | Observable proof retained |
| --- | --- | --- |
| Readiness noticed sooner without losing ownership or failure reporting | 1 | `pnpm test:sut-start` (health wait: timeout with log tail, cancellation, child exit before healthy); the LFS feature batch passes and cleans up; Development start still reports readiness |
| Fixture commits skip hooks while LFS stays authentic | 2 | Every `cli_notebook_*.feature` scenario passes, including the LFS round trip, refusal, exact hydrated bytes, pointer history and historical fetch in `cli_notebook_lfs.feature` |
| Panel behavior survives one shared spec file | 3 | All 11 existing cases (READ/SKIMMED borders, successor and last-block visibility, obstruction, anchored coordinates, scrolling into the successor) pass in the merged file |
| Ordinary local feedback improves, including added proof cost | 4 | Comparable whole-command wall times and case counts for the commands above |

No case is removed. When the two spec files become one, sweep the old file
names once at acceptance; add no test that the old files are absent.

## Ordered slices

### 1. Notice an owned stack's readiness sooner
Type: Behavior
Status: done

Behavior: A fresh owned stack becomes healthy between two polls → the wait
notices it within a shorter gap than today's 3000ms → Cypress starts only after
a real healthy check, and timeout, cancellation and child-exit reporting are
unchanged.

Change: lower the one shared `POLL_MS` default in
`scripts/sut-start-health-wait.mjs` (starting point 500ms, from the
observation above). Do not change the health predicate, deadline, lifetime, or
ownership. The default also serves `dev-start.mjs`, `sut-owned-restart.mjs`
and `sut-owned-lifetime.mjs`; keep one value for all of them.

Proof and measurement: before and after, alternate at least three runs each of
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
after one discarded warm-up run, recording "Starting SUT" → "SUT healthy" and
whole-command wall time. Run `CURSOR_DEV=true nix develop -c pnpm test:sut-start`.
Start and stop Development start once and confirm it reports readiness. Three
consecutive focused E2E passes cover the changed waiting.

Decide: keep the change only if time-to-healthy improves across the
alternating runs without a slower or failing command; otherwise undo it and
record no gain.

Accepted (2026-11-01): **kept** `POLL_MS` 500. Warm LFS before avg
time-to-healthy 47.9s / wall 133s (polls 7–16); after avg 23.0s / wall 77s
(polls 15–22); three after passes. `pnpm test:sut-start` 80 pass;
`pnpm test:development-stack` 19 pass (`Development healthy after 1 poll(s)`);
live `pnpm dev` refused in linked worktree (G1).

### 2. Commit E2E fixtures without running Git LFS hooks
Type: Behavior
Status: planned

Behavior: E2E setup has a real LFS checkout → a test-owned step commits a note
or attachment change → the commit stores a real LFS pointer without running the
LFS hooks, and the installed CLI still publishes and hydrates the exact bytes.

Change: give test-owned fixture commits one shared commit helper that passes
the E2E author identity and `-c core.hooksPath=<empty location>` on that one
command, and use it from `commitCheckout`, `amendCheckout`,
`commitWithE2eAuthor`'s callers and the `lfs payload` commit. Remove the
duplicated identity shape. Keep `git add`, `git lfs push/fetch/checkout`, and
every installed-CLI command unchanged; save nothing in the checkout's config.

Proof and measurement: before and after, alternate at least two runs each of
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec "$(ls e2e_test/features/cli/cli_notebook_*.feature | paste -sd, -)"`
after one discarded warm-up run. In a worktree the runner refuses globs, so
the shell expands them into an explicit comma-separated list; on 2026-11-01
`assertSupportedIsolatedCypressSpecs` accepted that list of 13 features. All
scenarios pass, including the
profile-excluded `cli_notebook_existing_note_edits.feature`. Run
`CURSOR_DEV=true nix develop -c pnpm cli:test` for CLI compatibility.

Decide: keep the change only if the whole command improves; otherwise undo it
and record no gain. No test counts or checks hooks.

### 3. Prove reading-panel behavior from one spec file
Type: Structure
Status: planned

Change: merge `BookReadingPage.readingPanelTarget.spec.ts` into
`BookReadingPage.readingControlPanel.visibility.spec.ts` under one setup,
keeping all 11 cases and their names. Move the target spec's
`mountFirstBlockWithContentBbox` into the existing
`bookReadingPagePanelTestSupport.ts` so the file stays within 250 lines. Add
no new fixture framework or test-registration helper. This directly owns the
`frontend-sibling-spec-setup` correction for this pair.

Proof and measurement: before and after, alternate at least three runs each of
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/pages/BookReadingPage`
(39 tests today); then run `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`
once before and once after, and `pnpm -C frontend exec vue-tsc --noEmit` plus
`pnpm -C frontend exec tsc --noEmit` through Nix.

Decide: keep the merge only if the focused command does not get slower and
every case still observes its own behavior; a cleaner single file with an
unmeasurable saving is acceptable only if the full-suite run is not slower.

### 4. Record the local-feedback result
Type: Behavior
Status: planned

Behavior: A contributor runs each selected ordinary command after the kept
experiments → gets the same behavioral confidence with a measured shorter
wait, or a recorded no-gain result.

Proof: report, per slice, before/after wall times, case counts, runs compared,
and kept or undone changes. Update the candidate record: resolve the three
transferred entries for what was done; return any newly seen removable cost
(for example cold-start time, or other sibling-spec families under the
original `frontend-sibling-spec-setup` slug) as pending candidates. Claim no
gain from historical numbers, fewer files, or summed durations.

## Current decisions

- Follow [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  (the runner owns its disposable stack) and
  [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  (authentic Git/LFS history). No new architectural decision.
- Keep owner-selected HMR and existing profile exclusions; normal verification
  runs every scenario.
- Slices are independent; any order works, and an undone slice does not block
  the others.
- Each slice follows hypothesize → measure before → change → measure after →
  keep or undo. Measure before delivery; passing tests alone keep nothing.
- Delivery uses Execute Plan: independent refactor, coordinator
  `./scripts/run.sh pnpm format:changed` once, plan update, check-only commit
  hook, push.

## Concerns and readiness

Slice-plan refinement was not needed: the three slices are separate, each is
one small edit with its own proof, and slice 4 only reports. Editing per slice
is ~5 minutes; the repeated E2E measurements (about 1–2 minutes per run) are a
stated verification-time exception to the 10-minute limit. No remaining
slice-specific concern was found; the small size of each saving relative to
machine noise is handled by the alternating measurement and the no-gain
outcome, not left open.

## Story obligations

### G1. Live Development start refused in linked worktree
Reported: slice 1 — "Live `pnpm dev` / `dev:stop` on this linked worktree — actively refused; Development readiness covered by `test:development-stack` instead."
Story clause: "Readiness cadence is one shared default in `scripts/sut-start-health-wait.mjs`, also used by `dev-start.mjs`"
Disposition: proved by slice 1: `CURSOR_DEV=true nix develop -c pnpm test:development-stack` observed `Development healthy after 1 poll(s)` via the same `waitForSutHealthy` path `dev-start.mjs` uses

## Learnings

2026-11-01: Cold and warm E2E runs differ by minutes on this machine (4m18s vs
under 1 minute for the same feature), so only warm, alternating runs are
comparable. The readiness wait is 13–31s, of which polling cadence can remove
at most ~3s.

2026-11-01 execution: Slice 1 kept `POLL_MS` 500. Warm LFS before avg
time-to-healthy 47.9s / wall 133s (7–16 polls); after avg 23.0s / wall 77s
(15–22 polls). Host load rose during measurement from other worktrees'
Playwright; direction still favored keep. Linked worktrees refuse live
`pnpm dev`; Development consumer covered by `test:development-stack`.
