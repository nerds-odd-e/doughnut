# Source review — SEED-039#candidate-resolution

**Launch:** Fresh general-purpose reviewer (no plan, product, proof, or conversation inheritance).
**Input:** Story Goal, Scope, key examples, architecture, and execution-boundary text from
`.planning/seeds/SEED-039-faster-ci-feedback.md#candidate-resolution` only.
**Returned:** Numbered commitments 1–14 (with compound sub-ids) as retained below.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Shorter trustworthy local test feedback by removing the three named avoidable setup costs; keep only changes whose ordinary command measurably improves | slice 4 | observed: all three kept under warm ordinary-command averages (LFS wall 133.0→77.3s; `cli_notebook_*` 148.5→135.5s; BookReadingPage 10.72→6.69s); recorded in candidate Latest pass |
| 2 | A change whose ordinary command shows no comparable improvement (including added proof cost) is undone and recorded as no gain | slice 4 | observed: no undo required; Latest pass states no-gain path unused; no claim from historical numbers, fewer files, or summed durations |
| 3 | Work is limited to the three transferred obligations `e2e-batch-startup`, `cli-git-lfs-hook-installation`, and `frontend-sibling-spec-setup` | slice 4 | observed: only those three closed; cold-start and remaining sibling families returned pending; Transferred obligations = None. |
| 3a.1 | `e2e-batch-startup` — change only the readiness polling cadence for owned E2E stack readiness | slice 1 | observed: `scripts/sut-start-health-wait.mjs` default `POLL_MS` 3000→500; LFS `cy:run` after runs noticed healthy sooner (avg 23.0s vs 47.9s) |
| 3a.2 | `cli-git-lfs-hook-installation` — address LFS hook cost only on test-owned fixture commits | slice 2 | observed: shared `fixtureCommit` / `commitCheckout`/`amendCheckout` only; `git add` and `git lfs` / CLI unchanged; batch 40/40 |
| 3a.3 | `frontend-sibling-spec-setup` — consolidate setup only for the reading-panel visibility and target specs | slice 3 | observed: target spec merged into visibility spec; other sibling families untouched |
| 4 | Fresh owned stack healthy between polls → runner notices within a shorter gap than today's 3s → Cypress starts only after a real healthy check | slice 1 | observed: after LFS runs poll counts 15–22 at 500ms; Cypress proceeded only after healthy; three consecutive passes |
| 5a.1 | Real health checks stay | slice 1 | observed: `pnpm test:sut-start` (health wait paths); LFS feature uses real healthcheck |
| 5a.2 | Deadline stays; never-healthy stack still times out with log tail | slice 1 | observed: `pnpm test:sut-start` timeout-with-log-tail cases (80 pass) |
| 5a.3 | Cancellation still cleans up (Ctrl-C) | slice 1 | observed: `pnpm test:sut-start` cancellation cases |
| 5a.4 | Failure cleanup stays | slice 1 | observed: `pnpm test:sut-start` child-exit / failure cleanup cases |
| 6 | E2E runner keeps owning its fresh disposable stack (ADR 0007); no ownership move | slice 1 | observed: LFS `cy:run` owned stack start/cleanup; no ownership change in diff |
| 7a.1 | Real LFS filters stay | slice 2 | observed: `cli_notebook_lfs.feature` scenarios pass in AFTER batch (filters still produce pointers) |
| 7a.2 | Real LFS transfer stays | slice 2 | observed: LFS publish/hydrate scenarios in AFTER `cli_notebook_*` batch 40/40 |
| 7a.3 | Real pointer history stays; commit stores a real LFS pointer | slice 2 | observed: pointer history / historical-fetch scenarios in AFTER batch |
| 7a.4 | Installed CLI still hydrates the exact bytes | slice 2 | observed: exact hydrated bytes scenarios in AFTER batch; `pnpm cli:test` 440 pass |
| 7a.5 | Test-owned fixture commit no longer runs LFS `post-commit`/`post-checkout` hooks | slice 2 | observed: `fixtureCommit` passes `-c core.hooksPath=/dev/null` on commit/amend only (no hook-count test per plan) |
| 8a.1 | Eight visibility + three target cases from one spec file with one setup | slice 3 | observed: visibility spec runs 11 tests under one `beforeAll`/`beforeEach`; BookReadingPage suite 39/39 on 5 files |
| 8a.2 | Every case still observes its distinct visible behavior | slice 3 | observed: original `it` names and assertions retained in merged file; focused 39/39 |
| 8a.3 | Per-file browser isolation stays | slice 3 | observed: still one Vitest file per remaining panel concern; no cross-file browser sharing introduced |
| 8a.4 | Consolidated file stays within the 250-line limit | slice 3 | observed: merged visibility spec 245 lines |
| 9 | Existing profile exclusions stay | slice 4 | observed: profile-only exclusion inventory unchanged; no new tags; confirmed in Latest pass Limits |
| 10 | Owner-selected local HMR stays | slice 4 | observed: `e2e-vite-first-page-load` remains accepted necessary cost; no built-frontend substitution |
| 11 | Hook bypass only via `-c core.hooksPath=…` on that commit command; never a saved checkout setting the CLI later reads | slice 2 | observed: `E2E_FIXTURE_COMMIT_CONFIG_ARGS` command-scoped; LFS scratch prep no longer saves `user.name`/`user.email` |
| 12 | Readiness cadence is one shared default in `scripts/sut-start-health-wait.mjs` for `dev-start.mjs`, `sut-owned-restart.mjs`, and `sut-owned-lifetime.mjs`; not an E2E-only value | slice 1 | observed: single `POLL_MS` default in `sut-start-health-wait.mjs`; LFS `cy:run` + `test:sut-start` + `test:development-stack` (`Development healthy after 1 poll(s)`) |
| 13a.1 | No other startup cost (backend boot, Gradle check, Cypress launch, database provisioning); costs found there return to the candidate record | slice 4 | observed: only `POLL_MS` changed; cold-start returned as pending `e2e-batch-cold-start`; other startup costs not claimed solved |
| 13a.2 | No other sibling-spec families; costs return to the candidate record | slice 4 | observed: only reading-panel pair merged; remaining families pending under `frontend-sibling-spec-setup` |
| 13a.3 | No shard or worker changes | slice 4 | observed: none; Latest pass Limits |
| 14 | Experiment plan belongs to this story; each change takes its own comparable before/after measurement; landing preparation starts no execution | slice 4 | observed: per-slice warm before/after recorded in Latest pass and plan Accepted; recording only, no delivery in this slice |

## Slice 1 acceptance comparison

| Id | Trigger / setup | Source-expected result | Actual observation | Owner |
| --- | --- | --- | --- | --- |
| 3a.1 | Change readiness polling only | Cadence shorter; not other startup mechanics | Only `POLL_MS` default edited | slice 1 |
| 4 | Fresh owned stack becomes healthy between polls | Notice sooner than 3s gap; Cypress after real healthy | Avg time-to-healthy 47.9s→23.0s; three after LFS passes | slice 1 |
| 5a.1–5a.4 | Timeout / cancel / failure paths | Unchanged reporting and cleanup | `pnpm test:sut-start` 80 pass | slice 1 |
| 6 | Owned E2E stack lifecycle | Runner still owns disposable stack | LFS feature owned start/cleanup; diff has no ownership move | slice 1 |
| 12 | Change shared `POLL_MS` default | All listed consumers share one value | One default; consumers import shared wait; live `pnpm dev` refused in linked worktree → G1 / `test:development-stack` | slice 1 |

## Slice 2 acceptance comparison

| Id | Trigger / setup | Source-expected result | Actual observation | Owner |
| --- | --- | --- | --- | --- |
| 3a.2 | Test-owned fixture commit | Hook cost only on those commits | `fixtureCommit` only on commit/amend paths | slice 2 |
| 7a.1–7a.4 | LFS fixture publish/hydrate journeys | Real filters, transfer, pointer, exact bytes | AFTER `cli_notebook_*` 40/40 incl. LFS; `cli:test` 440 | slice 2 |
| 7a.5 | Same commits | Skip LFS post-commit/post-checkout | `-c core.hooksPath=/dev/null` on command | slice 2 |
| 11 | Bypass mechanism | Command-scoped only; no saved checkout setting | Config args on command; removed saved author config from LFS scratch prep | slice 2 |

## Slice 3 acceptance comparison

| Id | Trigger / setup | Source-expected result | Actual observation | Owner |
| --- | --- | --- | --- | --- |
| 3a.3 / 8a.1 | Merge visibility + target specs | One file, one setup, 11 cases | Merged visibility file; 11 tests; suite 39/39 | slice 3 |
| 8a.2 | Each case | Distinct visible behavior | Original case names/assertions kept | slice 3 |
| 8a.3 | Browser isolation | Per-file isolation stays | No cross-file browser share added | slice 3 |
| 8a.4 | After merge | ≤250 lines | 245 lines | slice 3 |
