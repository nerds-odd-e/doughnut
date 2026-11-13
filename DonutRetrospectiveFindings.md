# Donut Retrospective Findings

Donut owns findings about its product, repository tooling and local conventions.
[DearDough](DearDough.md) covers shared planning, proof acceptance and delivery,
even when the product symptom was repaired. Keep the original identity and
evidence when moving a finding. Retire only with attributable repair and outcome
evidence or a satisfied observation criterion. Reopen the same lineage when new
evidence contradicts an adopted repair, and link any bounded follow-up.

## Ownership and priority assessment

The 2026-10-12 review (Donut `8ac439b16b`, Open Dough `v0.3.60`) verified that
[shared execution wrap-up](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-execute-plan/references/wrap-up.md)
and [refactor checks](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-post-change-refactor/references/refactor-checks.md)
allow a negative assertion when absence is the promise and defer to project
test style. Donut's principle 7 and `unit-testing` skill supply the stricter
removal convention. The 2026-11-03 review (Donut `afc47ea75c`, Open Dough
`0.3.82`) separated the shared acceptance decision (former DD-334) from the
local OpenAPI collision (DD-335).

DD-317 and DD-318 retired after SEED-081#story-1 and SEED-081#story-2 completed
2026-10-21. Both were source-backed assessments without live occurrence
counts. For DD-318, `bae8f72e04` and `8ee3143db6` preserve per-tracker pending,
failed and loaded state with same-tracker Retry and explicit Self-review
(`RecallPage.questionRecovery.spec.ts`, [current behavior](docs/recall-question-loading.md)).
Recover the full DD-317 and DD-318 records at
`eae596d487:DonutRetrospectiveFindings.md` and
`1348cea71a:DonutRetrospectiveFindings.md`, respectively.

The 2026-11-08 review at `5f2be9874c` queues zero stories. MinerU setup and
the single repaired OpenAPI collision rank below current learning/refinement
work, and the Development failure still has no established cause. In the new
SEED-108#spread-out-recall-backlog report, `5002ad7856` repaired the test
builder's due-time override. The remaining unverified-premise observation
belongs with ODF-074 in [DearDough](DearDough.md#odf-074).

The 2026-11-10 review at `f61aeafdf4` read the DearDough
additions DD-347, DD-348 and DD-438 to DD-447. DD-439 moved here: the failed
responsibility is Donut's command wrappers, and the briefs' repeated exports
show recurring cost, so it queues
[SEED-116](.planning/seeds/SEED-116-agent-shell-independent-commands.md#run-repository-commands-without-interactive-shell-setup).
DD-438's leftover check failed through a Nix-shell temporary folder, but no
false reading occurred and its finding is about deriving a proof location from
the environment, so it stays shared. The other additions concern shared
planning, delegation, proof and delivery. DD-161, DD-159 and DD-335 have no
new occurrences.

## Implemented, awaiting observation

<a id="dd-439"></a>

### DD-439 — Agent shells lacked the environment that repository commands rely on

The coordinator's and agents' non-interactive shells do not load `~/.zshrc`, which exports `NIX_CONFIG` (flakes) and `OPENAI_API_TOKEN`. `./scripts/run.sh` refused to start Nix, and mocked-OpenAI E2E features failed because the backend reads its key only from `OPENAI_API_TOKEN`.

Moved from [DearDough](DearDough.md) on 2026-11-10; the source entry is recoverable at `f61aeafdf4:DearDough.md`. Ownership: Donut's `./scripts/run.sh` and mocked-OpenAI E2E stack depend on interactive-shell exports; no shared Open Dough contract failed.

#### Occurrences
- Execution: SEED-113#assimilate-notes-with-a-clear-default-and-result (first implementation commit 9868d40b3c)
  - Timestamp: unknown (2026-11-09 Asia/Singapore, execution start and slice 1 E2E)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.92
  - Evidence: `./scripts/run.sh bash scripts/worktree_setup.sh` → "experimental Nix feature 'flakes' is disabled"; slice 1 E2E `sut.log`: `IllegalStateException: This request requires apiKey` (8 refinement, 10 question-generation errors); reruns with an exported placeholder passed.
  - Observed effect: one refused setup command and a diagnosis at startup; two E2E features failed and were rerun; every later brief had to carry both exports.
  - Inference: the wrappers depend on interactive-shell environment; a wrapper-supplied flag and mock token would remove the dependency.
- Verified response (2026-11-10, SEED-116#run-repository-commands-without-interactive-shell-setup): `scripts/run.sh` supplies its required additive Nix features for population and reuse (`12e1f925a5`). The E2E profile supplies a non-secret credential fallback while preserving supplied-token precedence and the held-stack paid-call choice; delivered in [`be62cc90a4`](https://github.com/nerds-odd-e/doughnut/commit/be62cc90a427fc7663fb481a06e1f15a582fe99b).
  - Current execution proof: six runner cases, real Nix cold/reuse with host features disabled, and public worktree setup passed. The corrected full backend suite ran 3,093 tests with no failures (two existing skips), including production-YAML E2E unset/supplied and Development-unset configuration cases. All six held-stack cases passed, including both synthetic-token choices. A clean-shell invocation with neither token nor Nix-feature exports passed all seven refinement/question-contest scenarios; its owned services shut down, and its new `sut.log` had no credential-precondition error.
  - This verifies the repair under the selected reproductions. A later agent execution must supply non-recurrence evidence; none is claimed here. The original dated observations and Open Dough 0.3.92 provenance above remain unchanged.

<a id="dd-485"></a>

### DD-485 — CI jobs waited about 50 minutes each on a package mirror that did not resolve

Two story-branch CI runs failed in every job that installs system packages, each after about 50 minutes inside `apt-get`. The failures arrived as separate notifications over about 90 minutes. [The shared observer facet](DearDough.md#dd-485) covers the same execution. This entry owns Donut’s system-package setup bound; the shared CI-observation facet stays in DearDough.

#### Occurrences
- Execution: SEED-124#save-pronunciations-as-note-properties / e9f109c3c25b0355a897161f8a8180fdfb22a1a5
  - Timestamp: 2026-11-11T23:32:09+08:00
  - Tool: Claude Code
  - Model: claude-fable-5-2
  - Open Dough release: 0.3.96
  - Evidence: CI runs 43049319481 and 43053073660 (attempts 1 to 3); failed step durations of about 3,046 seconds; `Temporary failure resolving 'azure.archive.ubuntu.com'`; attempt 2 lost one runner during the same step.
  - Observed effect: Eleven failed jobs per run with no test executed; five diagnosis rounds for one cause; green evidence for the product commit came about eight hours after its push.
  - Inference: The mirror outage was external. At `4059ac2353`, Donut’s [Node/Cypress setup](.github/setup_nodejs_with_cache/action.yml) and [MySQL setup](.github/mysql_binaries/action.yml) run package installs without an explicit shorter bound. The backend unit-test job passed in both affected runs without using these package steps. See `aa17aab044:.planning/slice-plans/001-save-pronunciations-as-note-properties/PLAN.md` for the job and attempt distinctions; the full combined source is at `4059ac2353:DearDough.md`.
- Verified response (2026-11-13, SEED-129#bound-ci-system-package-installation): shared `scripts/ci-system-packages.sh` bounds canvas and MySQL system-library phases to 600s with 10s kill-after and two acquisition retries; both setup actions call it. Disposable Linux probe `43410926122` showed bad-mirror DNS failure in 3s with phase/budget diagnosis and skipped downstream, stalled TERM-ignore cleanup at 25s (`status=137`), transient recovery, and healthy install in 11s. Surviving shell fixtures and push CI on product SHA `281dd13820f8aa2d6c426fdac20ee153171c524c` (run `43418396664`) accepted the wiring. Intent Coverage Caught on phase/budget enrichment (`281dd138`).
  - This verifies the repair under the selected reproductions. A later agent execution must supply non-recurrence evidence against a real mirror outage; none is claimed here. The original dated observations and Open Dough 0.3.96 provenance above remain unchanged.

## Queued

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

## Repaired in the originating execution

<a id="dd-480"></a>

### DD-480 — An HTTP fixture invented the meaning of an empty assimilation value

The frontend treated any supplied propertyValue, including empty string, as a list item and wrote a passing blank-item fixture. The existing backend explicitly uses empty string for scalar units.

#### Occurrences
- Execution: SEED-119#highlight-and-link-to-a-specific-property-item / 3349feafb0163ea7c0a72d54562b5638a9fa1b52
  - Timestamp: unknown
  - Tool: Codex
  - Open Dough release: unknown; execution-time snapshot fa51f486fca13f9f3c85eaebbec2ed93be8e8522
  - Evidence: 2026-11-10; CI run42762324814/attempt1 E2E job129650701015, Skip expected /n6/p/topic but received /n6/p/topic/i/1/~; backend PropertyFocus/AssimilationUnit documentation; repair c371659ac7121d1e83128c616fa1c0b391ac0867.
  - Observed effect: selected mocked proof and list continuation E2E passed before the existing scalar walkthrough failed. Positive scalar-empty red reproduced the defect; nonempty raw values retain item routing and manual blank selection remains a separate input path. The corrected workspace fixture,37 focused tests, both types and10 real E2E scenarios passed.
  - Inference: reuse of the key/value shape did not establish the wire sentinel. Read the producing contract before choosing a branch or manufacturing fixture variants; one observed execution does not establish recurrence.

Ownership: Donut’s frontend fixture contradicted its backend wire contract. The exact scalar failure was reproduced and repaired in the originating execution. Recover the combined source at `4059ac2353:DearDough.md`; no follow-up is supported by this one repaired case.

## Useful project practice

### DD-230 — An environment-only Flyway callback with a MySQL named lock held a real startup migration open for full-stack proof

SEED-067#start-e2e-after-migrations / `f297a916c5`, 2026-10-11T09:18:00+09:00,
Claude Code/claude-opus-5-5, Open Dough 0.3.58. An external `beforeMigrate.sql`
on `SPRING_FLYWAY_LOCATIONS` waited for a lock held by a separate session, which
released it. Both `pnpm e2e:hold` and `pnpm cy:run` returned 503 while held and
reset test data only after the first 200. Each slice took about five minutes
including startup. See plan 066 Learnings for slices 3–4 at `af62c7cd0a` and
`sut.log` lines 148–234, including 41 applied migrations. `ps eww` confirmed
environment forwarding. The observation required no product/test changes or
delay option.
