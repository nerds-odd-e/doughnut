# Donut Retrospective Findings

Donut owns findings about its product, tooling and conventions. [DearDough](DearDough.md) owns shared guidance findings. Keep original identities and evidence on moves. Retire with attributable outcome proof or a satisfied observation criterion. Resume the same lineage if later evidence contradicts an adopted repair.

## Ownership and priority assessment

- Donut's principle 7 and unit-testing skill impose the removal-test convention. The 2026-10-12 review at `8ac439b16b` verified that Open Dough v0.3.60 permits an absence assertion when absence is the promise and defers to project test style. [Shared wrap-up](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-execute-plan/references/wrap-up.md), [refactor checks](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-post-change-refactor/references/refactor-checks.md).
- DD-317 and DD-318 retired when SEED-081 stories 1 and 2 completed on 2026-10-21. Both assessments were source-backed with no live occurrence count. DD-318 proof at `bae8f72e04` and `8ee3143db6` preserves per-tracker pending, failed and loaded state, same-tracker Retry and explicit Self-review. Full records at `eae596d487:DonutRetrospectiveFindings.md` and `1348cea71a:DonutRetrospectiveFindings.md`.
- `5002ad7856` repaired the due-time override in SEED-108#spread-out-recall-backlog. The remaining premise finding is [ODF-074](DearDough.md#odf-074). DD-438 remains shared: the Nix temporary folder changed, but the proposed remedy concerns deriving proof locations from the environment. It caused no false reading.
- DD-161 and DD-335 rank below current learning work. DD-159 has no established cause. DD-439 moved here on 2026-11-10 and its response shipped through [SEED-116](.planning/done/SEED-116_run-repository-commands-without-interactive-shell-setup.json). Earlier review and attribution notes are recoverable at `05b47c062f8d0d2f23c4d7eb5739c6bdacde3873:DonutRetrospectiveFindings.md`.

<a id="records-only-refactor-reviews"></a>

In [ODF-141's tracker-question execution](DearDough.md#odf-141), three
records-only refactor passes returned no edits or product-test runs. The launch
spans were about 222, 205 and 168 seconds and include overlap and waiting. At
`c763797a3c`, Donut's `AGENTS.md` required a fresh wrap-up refactor agent;
installed `deliver-the-change.md` allowed a records-only disposition. Donut
owns its stricter wrap-up rule. This is the same execution as the shared
occurrence and its active cost was unmeasured. No local policy change is queued.

## Implemented, awaiting observation

<a id="dd-439"></a>

### DD-439 — Agent shells lacked the environment that repository commands rely on

Donut's runner and mocked E2E stack relied on interactive exports. In `SEED-113#assimilate-notes-with-a-clear-default-and-result` at `9868d40b3c` (Claude Code Opus 5.5, Open Dough 0.3.92, 2026-11-09 Singapore, time unknown), setup refused disabled flakes; missing mock credentials caused 8 refinement and 10 question-generation errors across two features. Reruns passed with a placeholder and later briefs repeated the exports. Source: `f61aeafdf4:DearDough.md`.

**Adopted 2026-11-10.** SEED-116 supplied additive Nix features on population and reuse (`12e1f925a5`) and an E2E mock fallback preserving supplied-token precedence and held-stack paid-call choices ([implementation](https://github.com/nerds-odd-e/doughnut/commit/be62cc90a427fc7663fb481a06e1f15a582fe99b)). Six runner cases, cold/reused worktree setup, 3,093 backend tests with two skips, all six held-stack cases and seven clean-shell E2E scenarios verified the selected reproductions. Services stopped and the new `sut.log` showed no credential error. Later ordinary non-interactive use remains the observation criterion.

<a id="dd-485"></a>

### DD-485 — CI jobs waited about 50 minutes each on a package mirror that did not resolve

Donut owns the system-package setup bound. [DD-485 in DearDough](DearDough.md#dd-485) owns the observer facet of the same execution.

- `SEED-124#save-pronunciations-as-note-properties` / `e9f109c3c25b0355a897161f8a8180fdfb22a1a5`, 2026-11-11T23:32:09+08:00, Claude Code Fable 5.2, Open Dough 0.3.96. A package mirror DNS outage blocked eleven setup jobs on each of runs 43049319481 and 43053073660 (~3,046 seconds per failed step; attempt 2 also lost a runner). Separate notifications arrived over ~90 minutes, costing five diagnosis rounds and ~8 hours to green. Backend unit tests bypassed package setup and passed. The 2026-11-11 baseline lacked an explicit shorter package-install bound. [Node setup](.github/setup_nodejs_with_cache/action.yml), [MySQL setup](.github/mysql_binaries/action.yml), original `4059ac2353:DearDough.md`, attempt details at `aa17aab044:.planning/slice-plans/001-save-pronunciations-as-note-properties/PLAN.md`.
- **Adopted 2026-11-13.** [SEED-129#bound-ci-system-package-installation](.planning/done/SEED-129_bound-ci-system-package-installation.json) routes both actions through `scripts/ci-system-packages.sh` with 600 s, 10 s kill-after and two acquisition retries. Probe 43410926122 covered 3 s DNS failure, TERM-ignore cleanup at 25 s (137), transient recovery and healthy 11 s install. Shell fixtures, Intent Coverage and push CI 43418396664 accepted `281dd13820f8aa2d6c426fdac20ee153171c524c`. Observe a later real mirror outage.

## Open, not queued

### DD-161 — A `.venv-mineru` whose Python was garbage-collected had to be rebuilt by hand

- SEED-059#story-3 / slice plan 051 / `946e2a70e3`, 2026-09-29 (slice 6 after `6f36cb2952`), Claude Code/claude-opus-5-5, Open Dough 0.3.46. The real PDF CLI `/attach` proof needed MinerU, but `.venv-mineru/bin` had no Python. The agent rebuilt it with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six` (slice 6 report, 1,692 seconds, about 197k tokens and 96 tool uses).
- The roughly 28 minutes includes stack scaffolding and is not the rebuild cost alone. Pinned-version and held-stack changes shipped in `f70984790f` and `ee414fbdf1` through `1ae39ec64f`. No repeat or automatic venv-recovery repair is confirmed.

### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

- SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / `681768a71b`, 2026-09-29T07:25:42+08:00, Claude Code/claude-opus-5-5, Open Dough 0.3.46. `pnpm dev` logged `No qualifying bean of type NotebookGitCutoverService`. The source was absent; deleting `backend/build/classes` let the next start succeed after one failed start and short UAT setup delay. Incremental compilation was suspected, with no cause established.
- SEED-067#stacks-survive-other-builds slice 2, 2026-10-03: after deleting a compiled service and its consumer with the stack stopped, subsequent builds removed the stale class and startup succeeded. The concurrent `classes --rerun-tasks`, build-cache restore and linked-worktree probes also succeeded. No writer was identified.

<a id="dd-335"></a>

### DD-335 — A nested DTO record silently reused an existing entity's OpenAPI schema

- SEED-100#story-1 / `cede42a579`, 2026-11-03 (exact time unknown, between slice 1 acceptance and push), Claude Code/claude-opus-5-5, Open Dough 0.3.82. Springdoc mapped nested `RecallHistoryExport.Answer` to entity `Answer`, losing three fields in a published increment (`cede42a579:open_api_docs.yaml`, `Event.answer`; `1e5c2b8e1b:.planning/slice-plans/001-recall-history-export/source-review.md` row 2a.3 and PLAN.md G1).
- `6932a70eb6` repaired the export with explicit schema names `RecallHistoryAnswer`, `RecallHistoryEvent` and `RecallHistoryDailyProbe`, still present at `5f2be9874c`. The repair cost one implementer round trip and regeneration. A generation-time collision check could protect other nested records with common simple names. No wider collision search was done. The shared proof facet is [ODF-413, formerly DD-334](DearDough.md#odf-413--a-commitment-was-recorded-as-observed-before-its-output-was-inspected).

<a id="dd-481"></a>

### DD-481 — Full local frontend proof lost the browser connection despite fewer workers

A two-worker full frontend run ended before all files loaded. The existing CI configuration already serializes file imports and uses an orchestrator GC reporter; selecting that configuration completed the full required proof without assertion retries.

#### Occurrences
- Execution: SEED-120#export-a-note-from-the-highlighted-property / 401bc9ff4a2bf02f4c4e922f2b87b1fb8b4e030f
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown; execution guidance snapshot 884a4fe8e463a1ea32120c7589cf808a82162285
  - Evidence: 2026-11-10 execution conversation; `/tmp/export-property-frontend-tests.log` (start18:17:41 Asia/Singapore), `/tmp/export-property-frontend-tests-serial.log`, `/tmp/export-ui-frontend.log`; retained acceptance at `7df14ec0c6:.planning/slice-plans/089-export-a-note-from-the-highlighted-property/source-review.md`; `frontend/vitest.config.ts` fileParallelism/orchestratorGcReporter.
  - Observed effect: `./scripts/run.sh pnpm frontend:test --maxWorkers=2` ended after259/408 files and1,950 passing tests with VITEST_BROWSER_CONNECTION_CLOSED (114.11s), without an assertion failure. `CI=true ./scripts/run.sh pnpm frontend:test --maxWorkers=2 --retry=0` then passed408files/2,521tests in262.65s and the next slice passed409files/2,528tests in245.98s. All runs were headless on the shared16CPU/48GiB local host; no production or test behavior was changed to obtain those passes.
  - Inference: worker reduction alone did not prevent this documented orchestrator failure mode. The existing serialized-import/GC mode is a supported bounded alternative for full local proof on a busy host. One failed run does not establish recurrence or a universal memory threshold; consider documenting the invocation in project testing guidance.

Ownership: Donut’s local frontend runner and proof guidance. The serial configuration completed two full runs in the same execution. The failure has not recurred in the evidence reviewed; keep the recorded command as an available route.

<a id="dd-483"></a>

### DD-483 — One backend Intent Coverage probe repeats the complete suite three times

The project backend rule requires the entire suite even for a targeted scratch break check. The measured check caught the selected fault, but three full runs and scratch setup exceeded its original five-to-ten-minute estimate. Assess a supported proportionate proof route with the project owner; this observation changes no installed rule or default.

#### Occurrences
- Execution: SEED-122#prefer-note-titles-in-wiki-links / e257b144d1e9120528922da0f5dc045acb6c9d6d
  - Timestamp: 2026-11-11T17:42:34.664+08:00
  - Tool: Codex
  - Model: gpt-6.1-sol
  - Open Dough release: unknown; guidance retained at f6d89e3470a6ed3064443c324763a074bd5bc884
  - Evidence: Native worker /root/intent_coverage_live, checked implementation e257b144d1; verbatim report and wrap-up disposition in the SEED-122 Done record; raw evidence /tmp/dough-intent-coverage-122-1794389081/.
  - Observed effect: One worker ran baseline/fault/restored commands across3111 maintained tests (plus one temporary independent witness in the fault run). Full command wall times315.751/162.748/218.847s total697.346s. Invocation-to-report1073.664s includes setup, reads, runs and verified retirement. The union fault caused six maintained failures and independently observed ambiguous controller results; baseline/restored suites and cleanup passed.
  - Inference: Whole-suite repetition is required by the current backend instructions, rather than a worker decision to broaden proof. Shared-host contention and cold scratch caches also contributed; the17.9-minute elapsed cannot all be attributed to the suite policy. Native blocking waits and bounded overdue checks retained one worker without restart or duplicate probing.

Ownership: Donut’s [backend skill](.agents/skills/backend/SKILL.md) still requires all backend tests. A scoped exception needs evidence that a focused check preserves the selected promise and a project decision about the full-suite rule. Keep the measured case open.
