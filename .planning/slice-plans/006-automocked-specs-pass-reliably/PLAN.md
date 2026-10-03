# Stop the MainMenu resume specs from failing intermittently in CI

**Identity:** SEED-039#mainmenu-mock-flake

## Source

[Stop the MainMenu resume specs from failing intermittently in CI](../../seeds/SEED-039-faster-ci-feedback.md#mainmenu-mock-flake),
refined on 2026-10-03. The story records the observed failure
([run 37108823838](https://github.com/nerds-odd-e/doughnut/actions/runs/37108823838),
frontend shard 2/2) and the unproven hypotheses.

## Goal and scope

Contributors can rely on the frontend unit-test job: the cause of the failed
automock in `frontend/tests/toolbars/MainMenu.resume.spec.ts` is known and
removed.

- **Included:** a recorded way to produce the failure; the cause, including
  why this file failed while `MainMenu.spec.ts` passed in the same shard; a
  fix that covers every spec the cause can reach; for a Vitest defect, a
  minimal reproduction, a drafted upstream report, and a project workaround.
- **Excluded:** raising the retry count, rerunning on failure, skipping or
  quarantining specs, or moving them between shards. A change to the mocks
  without a reproduced cause. Posting the upstream report (owner's action or
  instruction). Extra diagnostic output for a future occurrence. A general
  review of how specs mock modules. CI-time savings.
- **Considered, not added:** rerunning the frontend shard many times on a
  hosted runner to catch the failure there. It spends CI minutes and publishes
  a branch, so it needs the owner's instruction. Slice 1 stops and asks before
  using it.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The failure appears when the CI shard command runs with CI's settings on this machine | Slice 1 | `env -u NODE_ENV CI=true CURSOR_DEV=true nix develop -c pnpm frontend:test --shard=2/2`, one run at `a18969b16d` (2026-10-03) | **Not reproduced.** 159 files and 1037 tests passed in 37.7s; `MainMenu.resume.spec.ts` 9 tests in 32ms (1458ms in the failed CI run). The story records nine earlier local passes. An unaided local run is not a reproduction; slice 1 owns finding one |
| CI runs the shard with files one at a time and one retry | Slice 1 | `frontend/vitest.config.ts:21,79,82`: `isCI = process.env.CI === "true"`, `fileParallelism: !isCI`, `retry: isCI ? 1 : 0`; `.github/workflows/ci.yml:100` runs `pnpm frontend:test --shard=${{ matrix.shard }}/2` | Confirmed |
| A bare automock is served by a per-session route that answers the module request with a redirect | Slice 1 | `@vitest/browser-playwright@5.0.3` `dist/index.js:1060-1181` (`createMocker`) | Confirmed. `register` adds a `page.context().route(predicate, …)` that answers `302` to the same URL with `?mock=automock`. `delete` and `clear` call `unroute` and remove the map entry in a `.finally`. `register` and `clear` share one map keyed by session and module URL, so a `clear` that finishes after the next file's `register` for the same URL removes the new entry from the map. Whether this, or a module request that arrives before the route exists, produces the failure is unobserved |
| The failing file's order in the shard is known | Slice 1 | Log of run 37108823838 | `MainMenu.spec.ts` (same automock) passed at 08:11:59; `pdfBookViewerGeometryResample.spec.ts` ran directly before `MainMenu.resume.spec.ts`, which failed at 08:12:05. `MainMenu.resume.spec.ts` is the only one of the three MainMenu specs that also automocks `@/composables/useResumeRecall` and imports `useRecallData` itself |
| Mocker code under `node_modules` can be instrumented without touching other checkouts | Slice 1 | `stat -f %l` on the provider's `dist/index.js` in this worktree | Link count 1: the file is this worktree's own copy. A temporary edit stays local and must be undone before the slice ends (`git status` does not show it) |
| Twelve spec files declare `vi.mock("@/composables/useRecallData")` | Slice 2 | `grep -rl 'vi.mock("@/composables/useRecallData")' frontend/tests` | 12 files: three MainMenu specs, six RecallPage specs, three AssimilationPanel specs |

## Outside-in proof

- **Reproduction command:** recorded by slice 1 under *Learnings*. It runs the
  real Vitest browser runner on the real spec files. It is the red/green
  signal for slice 2.
- **Focused local check:**
  `env -u NODE_ENV CI=true CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/toolbars tests/pages/RecallPage tests/components/recall/AssimilationPanel`
  (the twelve files that share the automock).
- **Hosted CI:** both `Frontend Unit Tests` shards pass after publication.
  One green run does not prove an intermittent failure gone; the proof is the
  reproduction turning green.

| Promise | Slice | Proof |
| --- | --- | --- |
| Failure produced on demand, with CI's settings | 1 | Recorded command and condition; the observed error text |
| Cause established, including why `MainMenu.spec.ts` passed | 1 | Recorded explanation that the reproduction supports |
| Cause removed for every spec it can reach | 2 | Reproduction red before, green on repeated runs after; focused check green |
| Failure not hidden | 2 | Diff leaves `retry`, shard count, and skip markers unchanged |
| Vitest defect: minimal reproduction, drafted report, workaround | 3 | Minimal spec fails the same way on Vitest 5.0.3; report text beside this plan |
| Not reproduced: nothing changed, evidence returned | 1 | `git status` clean of product and test changes; attempts listed under *Learnings* |

## Slices

### 1. The failed automock is produced on demand and explained
Type: Behavior
Status: planned
Proof: a literal command and condition under which
`MainMenu.resume.spec.ts` fails with
`vi.mocked(...).mockReturnValue is not a function` at `setupMainMenuTests`,
plus the explanation it supports, both recorded under *Learnings*.

Behavior: given the unchanged tree and CI's settings, when the recorded
command runs under the recorded condition, then `MainMenu.resume.spec.ts`
fails with the observed error while `MainMenu.spec.ts` passes.

Sizing exception: reproducing an intermittent failure is a search. It is
bounded by the listed attempts, not by the five-minute target. Each attempt is
one observation; record its result before the next.

Steps, cheapest first. Stop at the first that produces the error:
1. Make the mocker's work visible in a passing run: for
   `MainMenu.resume.spec.ts`, record the order of `register`, the browser's
   request for `useRecallData`, and `clear` (temporary logging in this
   worktree's provider copy, or `DEBUG=vitest:*`). This confirms or corrects
   the route premise before forcing anything.
2. Run the observed file order alone with CI's settings: `MainMenu.spec.ts`,
   `pdfBookViewerGeometryResample.spec.ts`, `MainMenu.resume.spec.ts`.
3. Force each suspected ordering with a temporary delay in the provider copy:
   delay `route()` inside `register`; delay `unroute()` inside `clear`.
   Run step 2's files after each.
4. Slow the machine instead of the code: run shard 2/2 with CI's settings
   under CPU contention, ten runs.
5. If none produces the error, stop. Record each condition tried and what it
   rules out, restore the provider copy, and report to the owner. Slices 2 and
   3 do not start. Do not push a branch to rerun the shard on a hosted runner
   without the owner's instruction.

After a reproduction: state the cause and why `MainMenu.spec.ts` is not
affected, decide whether the cause is in Donut's tests or configuration or in
Vitest, restore the provider copy
(`pnpm install --force` or the saved original), and replace slices 2 and 3
below with the concrete change that the cause calls for.

### 2. Specs that automock a module pass under the triggering condition
Type: Behavior
Status: planned
Proof: slice 1's reproduction fails before the change and passes on ten
consecutive runs after it; the focused local check passes; `retry`, shard
count, and skip markers are unchanged in the diff.

Behavior: given slice 1's triggering condition and the fixed tree, when the
reproduction command and the focused check run, then the twelve specs that
automock `@/composables/useRecallData` pass every time.

The change removes the cause where it lives. When the cause is in Vitest, the
change here is the project workaround. Report production and test line counts
before and after, and whether a special case was added or removed.

### 3. A Vitest defect has a minimal reproduction and a drafted report
Type: Behavior
Status: planned
Proof: a spec that uses no Donut component fails the same way on Vitest 5.0.3
under slice 1's condition; `vitest-report.md` beside this plan holds that
spec, the versions, and the observed and expected results.

Behavior: given slice 1 placed the cause in Vitest's browser mocker, when the
reduced spec runs under the triggering condition, then it fails with the same
error, and the drafted report is ready for the owner to post.

Applies only when slice 1 places the cause in Vitest. Otherwise remove this
slice when slice 1 updates the plan. The reduced spec is not committed to
`frontend/tests`.

## Current decisions

- No fix without a reproduction. A mock change made on a guess is not a
  delivery of this story.
- Temporary instrumentation lives only in this worktree's `node_modules` copy
  or in uncommitted files, and is removed before a slice ends.
- Run frontend tests with `env -u NODE_ENV`; the owner's shell sets
  `NODE_ENV=production`.
- Local gates follow the `unit-testing` and `frontend` skills and the
  execution wrap-up in `CLAUDE.md`. No broader local suite is required: the
  change is expected to touch test files or test configuration only. Revisit
  this if slice 1 points at shared configuration.

## Learnings

None yet.
