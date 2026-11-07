# Publish Doughnut test execution times in Intent

Work item: **SEED-106#publish-ci-test-execution-times**.
**Source:** [refined story](../../seeds/SEED-106-publish-ci-test-execution-times.md#publish-ci-test-execution-times).

**Source review:** [Independent source review](source-review.md).

## Goal and scope

For a judged `donut CI` attempt, Intent's CI proof for `e2e_test/features` and
`frontend/tests` shows cumulative recorded test execution time (`durationMs`)
and a per-file breakdown that agree with the runner reports and the downloaded
`ci-proof-summary-attempt-<n>` artifact. Missing shards stay visible; failed
checks still contribute available timings; unrecorded durations stay untimed.

**Included:** refresh skill-owned collector copies into `scripts/ci/`; extend
`ci-proof-outcomes.mjs` to emit `--times` lines; wire
`.github/ci_proof_fragments` with `--times` and `--by-file`; update
`docs/ci-proof-summary.md`.

**Excluded:** `scripts` / `infra` / backend proof entries; wall-clock job or
setup time; Intent/dashboard product changes; CI-adapter publishing.

## Architecture

- **PFE:** Reuse the installed Open Dough CI proof recipe. Change the existing
  project conversion (`scripts/ci/ci-proof-outcomes.mjs`) and the fragment
  composite action. Refresh collector modules as **unchanged copies** of
  `.agents/skills/dough-test-optimization/scripts/ci-proof-{summary,contract,fragments,collect,figures}.mjs`
  (figures is currently missing; project copies lack `--times` / `--by-file`).
  Do not fork collector behavior.
- **Boundary:** Runner-specific conversion and workflow wiring stay
  project-owned; collector math stays skill-owned copies.
- **ADRs:** No Accepted ADR constrains this path. Loud conversion failure
  continues to match [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  is untouched (CI remains an execution location).
- **North Star:** No new topic; this is CI publishing plumbing, not notebook
  domain structure.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| Timed Vitest entry + file breakdown | 2, 3 | `ci-proof-outcomes.test.mjs` (Vitest durations → seconds); local `fragment`/`summarize` on fixture outcomes+times with `--by-file`; slice 3 CI artifact / Intent |
| Timed Cucumber entry + file breakdown | 2, 3 | same tests for Cucumber final-attempt step duration sums; slice 3 |
| Missing shard stays missing; present parts keep times | 2 | collector already encodes missing parts; fixture summarize with one part absent still carries other parts' `durationMs` |
| Failed tests still timed | 2 | outcomes test: failed Vitest/Cucumber checks with durations appear in times |
| Untimed load-failed Vitest file | 2 | outcomes test: failed empty `assertionResults` → outcome, no times line; fragment marks untimed |

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Installed collector supports `--times` and `--by-file` and emits `durationMs` / files | Structure slice copies those modules | Read `.agents/skills/dough-test-optimization/scripts/ci-proof-{summary,fragments,figures,collect,contract}.mjs` (2026-11-07); `fragment` option list includes `times` and flag `by-file` | Settled true |
| Project collector copies are stale (no times/by-file; no figures module) | Structure refresh | `diff` skill scripts vs `scripts/ci/`; `scripts/ci/ci-proof-figures.mjs` absent (2026-11-07) | Settled true |
| Vitest JSON assertions expose optional `duration` in milliseconds | Conversion writes `--times` seconds as `duration/1000` | Vitest JSON reporter docs / `JsonAssertionResult.duration?: Milliseconds` (Context7 `/vitest-dev/vitest`, 2026-11-07). Local Vitest run in this workspace failed on tooling; converter tests use the documented shape | Settled for field shape; absent duration → untimed |
| Cucumber `testStepResult.duration` is `{seconds, nanos}` | Conversion sums final-attempt step durations | `@cucumber/messages` Duration schema (Context7 `/cucumber/messages`, 2026-11-07) | Settled for field shape |
| `--times` lines are seconds (×1000 → `durationMs`) | Conversion unit choice | installed `ci-proof-fragments.mjs` `checkTimes` | Settled true |
| Intent already displays optional `durationMs` and `--by-file` breakdowns | Story excludes Intent code changes | Recipe [ci-proof-summary.md](../../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md) | Settled as publishing contract; live Intent agreement is slice 3 |

## Slices

### 1. Collector copies accept timed by-file fragments
Type: Structure
Status: done
Proof: After copying the five skill modules into `scripts/ci/`,
`node scripts/ci/ci-proof-summary.mjs fragment --outcomes <fixture> --times <fixture> --by-file --entry frontend/tests --part 1 --of 2 --attempt 1 --revision deadbeef --out <tmp>`
writes a fragment whose JSON includes `durationMs` and `files` (and existing
`pnpm test:ci-proof` / collector consumers still load).

Structure: Replace `scripts/ci/ci-proof-{summary,contract,fragments,collect}.mjs`
with the installed skill copies and add `ci-proof-figures.mjs` beside them,
unchanged. The root Biome exclusion lists all five copies so the formatter keeps their
bytes. The workflow and converter still use the counts-only path.

Accepted proof: all five `cmp` comparisons match the installed recipe.
`./scripts/run.sh pnpm test:ci-proof` passed all eight existing tests.
Using `/tmp/donut-slice-1.XINsup`, the fragment and summarize CLIs emitted
schema 1, counts of one passed and one failed, a visible missing part, and
500 ms on the timed fragment, present part, entry and file. Without the
optional flags, the same CLIs kept the counts and gap. The real producer
and collector actions use these same CLI boundaries. The independent
refactor consolidated the five Biome exclusions in one exact brace pattern.
The Biome search selected the project converter and tests and skipped all
five copies. The post-refactor byte comparisons still match.

### 2. Vitest and Cucumber proof fragments carry durations and file breakdown
Type: Behavior
Status: done
Proof: Extend `scripts/ci/ci-proof-outcomes.test.mjs` for the five key-example
conversions; extend `scripts/ci/ci-workflow.test.mjs` (or the fragment action
fixture) so `.github/ci_proof_fragments` invokes `fragment` with `--times` and
`--by-file`; run `pnpm test:ci-proof`. Optional local
`fragment`+`summarize` on the new fixtures shows entry `durationMs` and file
paths.

Behavior: Pre — producer jobs still write Vitest JSON / Cucumber messages as
today. Trigger — convert report → outcomes + times, then
`fragment --outcomes … --times … --by-file`. Result — each present part's
fragment carries summed `durationMs` when times exist, repository-relative
file breakdown, failed checks' available times, and untimed load-failed Vitest
files without invented durations. Update `docs/ci-proof-summary.md` to describe
timing and breakdown. Missing-shard visibility remains the collector's existing
rule.

Accepted focused proof: the real fragment action shell in
`scripts/ci/ci-proof-outcomes.test.mjs` verifies the timed Vitest and Cucumber
producer boundaries, including a skipped check in a timed file, a failed
load, a zero duration and a failed final Cucumber retry. The actual producer
and collector shells in `/tmp/donut-timed-producers.wokOfC/probe.mjs`
retain 1875 ms from five Cucumber parts, flag missing part 6, and retain
375 ms across two Vitest parts. A malformed report exits before writing a
fragment. `./scripts/run.sh pnpm test:ci-proof` passes all ten tests.
The checkout compares all five modules byte for byte with a verified,
isolated installation of Open Dough v0.3.89.

For slice 3's hosted comparison, this branch runs a disposable, read-only
audit of each runner report immediately after producing its fragment. It
compares all local file figures and the shard totals, and logs only aggregate
figures and one repository-relative sample. GitHub-hosted Ubuntu runs the
existing two Vitest and six Cypress shards. The previous Doughnut run
42378134511 succeeded in about six minutes. Remote resource headroom is
unknown. The coordinator owns the ordinary branch run and will reconsider
after setup failures or contention. Slice 3 deletes the audit after using
the exact run's logs, artifacts and Intent view.

### 3. Judged CI attempt shows timed proof in the summary and Intent
Type: Behavior
Status: done
Proof: Early probe after slices 1–2 are on the judged workflow: download
`ci-proof-summary-attempt-<n>` for one completed attempt; confirm both entries
carry `durationMs` and file breakdown for present parts, and that Intent's CI
proof for that attempt agrees on entry totals and one file from each (story
acceptance). Failure stops here and revises the plan; no further product scope
is assumed.

Behavior: Pre — slices 1–2 published on the revision the attempt builds.
Trigger — the attempt finishes and the developer opens that attempt in Intent.
Result — cumulative times and file breakdown match the artifact and runner
reports for the checked files; a missing part remains visible if one exists on
that attempt.

Hosted acceptance on 2026-11-07: story-branch revision
`7091f30d020a9a82c9b5226f0d00d4abf86e67d9` (suffix base `cd1f1794bd`),
GitHub Actions run 42379804192 attempt 1, passed. Downloaded the exact
`ci-proof-summary-attempt-1` and all eight fragments. The shard logs confirm
direct comparison of every runner-report file with its fragment.
`/tmp/doughnut-timing-hosted-proof.RqC0f4/compare.py` independently checked the
summary identity, compared every part with its raw-report audit, and verified
the rollup of all 486 file rows. The hosted run had no failed or missing
checks; slice 2's fixture proves the failed, untimed and missing cases.

| Entry | Artifact total | Intent CI status | Sample artifact and Intent file proof |
| --- | --- | --- | --- |
| `e2e_test/features` | 372 passed, 737252 ms, 6/6 parts | 12 min 17 s | `bazaar/bazaar_subscription.feature`, 3 checks, 7090 ms → 7.1 s |
| `frontend/tests` | 2457 passed, 63256.4 ms, 2/2 parts | 1 min 3 s | `DonutApp.loadingThinBar.spec.ts`, 1 check, 20.4 ms → 20 ms |

Intent was checked in Chrome at the selected story. The CI status panel
named run 42379804192 attempt 1 at `7091f30`. Each file's Proof tab named
Branch `7091f30` and showed the counts and rounded time in the Branch column.

## Current decisions

- Cucumber scenario time is the sum of `testStepResult.duration` on the **final**
  attempt only (same attempt the status rule already uses).
- Vitest times convert milliseconds → seconds for `--times`; do not invent
  values when `duration` is absent.
- Wall-clock CI cost stays deferred; published figures are recipe `durationMs`.
- Slice 1 found that the installed collector groups times by file and clears
  the whole file's untimed count when any timing is recorded. On the
  2026-11-07 execution checkout based on `16fda13bd5`, a Vitest JSON probe
  recorded a duration for a passed assertion and omitted it for a skipped
  assertion in the same file. The collector CLI probe with that report
  shape and a known 125 ms timing kept both counts and 125 ms, and emitted
  `untimed: 0` instead of 1. See G1. The existing counts-only producers
  do not enable this path. Slice 2 needs a corrected recipe while keeping
  the story's verbatim-copy constraint. The developer authorized the upstream
  repair. Open Dough source `169b8b8ac` fixes the aggregation and release
  `v0.3.89` at `611dbad88` supplies the corrected module. The worktree's
  generated Claude aliases prevented an in-place update. The official
  installer created a verified isolated installation, from which the five
  product collector modules were compared and refreshed.

## Concern evidence

- Slice-plan refinement: not needed — two commit-sized delivery slices plus one
  authorized early probe, one proof loop per Behavior, Structure immediately
  before its Behavior, no competing outcomes or special-case accumulation.
- Remaining slice-specific concerns: none identified. Slice 3 is an authorized
  early probe for live CI/Intent agreement; its failure revises the plan rather
  than blocking preparation readiness.

## Execution resume

- Story Branch Mode in the established checkout `/Users/terryyin/git/doughnut/.worktrees/publish-doughnut-test-execution-times-in-intent` on `codex/publish-doughnut-test-execution-times-in-intent`. Publisher `dashboard-territory.local-doughnut`, agent Eimi-chan. Replanning uses the existing planning authority.
- Starting revision `f752f369a80a07257d7f56121dd9c995e96105fb`. Published claim `16fda13bd5ed3010ea9cf58b7941d16a04b4f36b` on `origin/main` and the remote story branch. Claim CI is unobserved. Final target is `origin/main`.
- Checkout setup and baseline `./scripts/run.sh pnpm test:ci-proof` passed (8 tests).
- GitHub Actions `ci.yml` (5208694, donut CI). Slice 1 published as
  `cd1f1794bd1343572cd514d1a2d32d5fe0c9f150` on the story branch with
  suffix base `16fda13bd5`. That run 42378134511 passed. The earlier
  observer stopped at the approval boundary. The resumed story-branch
  observer is `eimi-chan-f1d68709`, cell `g70:13`, shell session `54255`,
  directory `/tmp/dough-ci-501/watch-zl5HVz`, PID 38176.


## Story obligations

### G1. A timed check masks untimed checks in the same file
Reported: slice 1 — "The independent slice 1 refactor review and the collector CLI both observed a file with one timed check and one untimed check publish untimed: 0. The two check counts and the recorded duration are present."
Story clause: "Do not invent durations the runner did not record; those checks stay untimed and still count."
Disposition: proved by slice 2: Open Dough v0.3.89 collector in the real Vitest action test reports the mixed file’s 125 ms and 1 untimed; the real summarize probe retains both figures in its entry file.
Commitment: R02

### G2. Retire the branch-only report audit after observing CI
Reported: slice 2 — "Slice 3 needs the CI runner inputs for direct report parity. The disposable shard audit reads them before the hosted runners discard them."
Story clause: "Keep the existing CI proof path"
Disposition: proved by slice 3: Deleted the disposable audit and its invocation. The ten focused CI-proof tests passed through the actual producer shell. The one-time whole-product reading for `ci-proof-runner-audit|CI_PROOF_RUNNER_AUDIT` returned no references outside planning history.
