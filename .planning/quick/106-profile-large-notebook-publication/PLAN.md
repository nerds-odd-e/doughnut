# Profile large notebook publication

Source: [SEED-018 story 2](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-2).
Status: executing. User authorized execution of plan 106 on 2026-09-11.

## Execution identity

- Origin: `/Users/terryyin/git/doughnut`, branch `main`.
- Taken-only claim: `3548fb8bc3`.
- Execution: `/Users/terryyin/git/doughnut-106-profile-large-notebook-publication`,
  branch `codex/106-profile-large-notebook-publication`, based on the claim.
- Integration target: `main`; delivery: `origin/codex/106-profile-large-notebook-publication`.
- CI observer: unavailable for this branch. `.github/workflows/ci.yml`
  (`donut CI`) runs only on pushes to `main`; no eligible branch-push workflow.
  No observer started; pending CI is unobserved.

## Outcome and boundaries

Maintainers can reproduce a large-publication baseline and choose improvement
areas using runtime evidence. Deliver focused static findings, reusable local
profiling infrastructure, baseline data for valid and late-invalid proposals,
and evidence-backed areas in story 3. Do not implement optimizations or write
story 3's execution plan. No speed target is needed for this learning outcome.

Use a deterministic existing-notebook fixture plus roughly 10,000 additions,
with bodies, aliases, properties, references, and folder placement. Document
distribution and representativeness limits. Read-only jap3 inspection may inform
the fixture; do not publish to the real notebook or modify that checkout.
Use a deliberately invalid alias shape, independent of pipe-name support.

## Reuse and constraints

- Reuse the public notebook Git-bundle publication/download boundary. Existing
  `cli/src/commands/notebook/notebookPublishSubmission.ts` owns bundle submission;
  `e2e_test/features/cli/cli_notebook_web_created_note.feature` already exercises
  installed-CLI publication. `e2e_test/start/testabilityNotebookGit.ts` owns
  fixture binding snapshots. Reuse those responsibilities rather than implementing
  another publication or acceptance path.
- Reuse `scripts/sut-runtime-target.mjs`, `scripts/sut-owner.mjs`, and existing
  worktree E2E lifecycle for the disposable target. The current isolated Cypress
  allowlist includes the existing CLI feature. Keep the opt-in workload at this
  supported boundary; do not broaden isolation policy for benchmarking.
- No publication profiler was found in the inspected scripts/build/docs. Add
  only opt-in workload/capture support and its run instructions. Keep large runs
  outside ordinary test runs. Use JFR on the owned backend JVM and record elapsed
  time, CPU samples, allocation/GC and relevant wait evidence; SQL counts alone
  do not establish database time. Add finer measurements only if sampling cannot
  distinguish the candidate costs.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  requires proven disposable ownership and separate processes/ports; never use
  Development or Production data or reset another task's SUT.
  [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs fixture content and lossless publication.
  [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
  requires visible failures; a harness timeout must not become a success result.
  ADR 0002 remains Proposed. No new architecture or North Star topic is needed.

## Ordered slices

Nine Behavior slices, each exposing one evaluable learning outcome. Slices 6
and 7 may proceed independently: persistent-profile analysis and a disposable
runner probe share neither files nor mutable SUT state. Slice 8 waits for both
to be delivered; all other dependencies remain sequential.
Target about 5 minutes active work per leaf; 5–10 minute estimates below rely on
the existing lifecycle and publication helpers. At 10 minutes active work stop
and finer-decompose remaining work. Long measured requests and required test
runtime are explicit elapsed-time exceptions, recorded separately; they do not
excuse additional implementation scope.

### 1. Identify what the baseline must distinguish
Type: Behavior
Status: done
Proof: A maintainer can inspect a short candidate table with code references,
repetition/scaling hypotheses, and the runtime evidence needed to rank them.

Given the large-addition workflow, statically trace publication from request to
acceptance and identify a bounded shortlist. Start with per-note creation,
validation, index refresh, queries/possible ORM flushing, Git blob reads, and
final projection. Treat each as a hypothesis, not a measured bottleneck.
Record the selected representative workload and measurement boundaries in the
same findings document. Stop once this guides capture; no exhaustive audit.

Estimate: 5 minutes active work. Safe stop: useful inspection findings exist
even before runtime measurement. This owns static inspection and workload rationale.

Delivered [static findings](../../../docs/notebook-publication-profiling.md):
six candidate groups with source links, scaling hypotheses and runtime
discriminators, deterministic workload proposal, and capture boundaries.
Coordinator inspected HTTP/application and both explicit index-flush boundaries.
Approximately 5 minutes active implementation. Independent refactor: no edits,
`REFACTOR COMPLETE`; documentation-only proof, no runtime claim.

Environment preparation: first `CURSOR_DEV=true nix develop -c pnpm sut`
failed startup. `sut.log` showed concurrent `backend:watch` and `backend:sut:ci`
compilation writing `backend/build/classes/java/main`, causing deletion failure.
Owned startup timed out and cleaned up. Serial
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes --no-daemon`
passed; retrying ordinary owned startup. This is a pre-existing cold-build race,
not a publication measurement or a repaired tooling defect.

### 2. Capture a small accepted publication
Type: Behavior
Status: done
Proof: The opt-in existing CLI E2E feature publishes 20 deterministic additions,
produces a readable JFR recording spanning publication, and receives the accepted
head and every authored document through the existing second-clone helpers.

Reuse the parked harness and correct JFR filename quoting. Verify owned PID and
isolation before attachment. Retain recording, exact commands, JVM/process and
revision metadata in a persistent artifact directory. Label recording interval
separately from request timing. Failed scenarios stop capture and preserve partial
recordings as incomplete; cleanup must not claim successful publication.
Run the same small scenario after reset to establish capture repeatability.
Document the initial fixture limitations and successful capture commands.
Estimate: 5 minutes active completion of the parked attempt, test runtime exempt.
Safe stop: useful small capture mechanism and verified acceptance, no large claim.

Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile`
passed twice with reset (1/1, 6s and 8s). Accepted head, second clone's clean
head and all 20 authored bytes verified. Readable request execution samples and
accepted results persist in `2026-09-11T10-15-59.526Z` and
`2026-09-11T10-16-29.353Z` under the documented profile directory.
Controlled assertion failure before publication saved `incomplete.jfr` through
composed after:run; `jcmd 2406 JFR.check` confirmed no active recording. Temporary
assertion removed. Details and commands: [capture evidence](../../../docs/notebook-publication-profiling.md#small-capture-smoke-test).
Approximately 6 minutes active completion, test runtime excluded. Fresh refactor
reused `expectCheckoutFileAt`, reran the same focused Cypress command (1/1),
and returned `REFACTOR COMPLETE`. No product/API changes.

### 3. Repeat a representative publication from a fingerprinted baseline
Type: Behavior
Status: done
Proof: A parameterized small-scale run creates the documented existing-note
baseline, publishes deterministic additions, verifies accepted content and head,
and repeats after reset with the same logical fixture fingerprint.

Extend the same fixture/capture workflow to the documented content distribution,
fixed Git identity/dates, existing learning record and parameterized counts.
Record exact implemented distribution and representativeness limits. Capture
CLI execution timing separately from setup and JFR interval, result/head, fixture
fingerprint, revision, MySQL/JVM versions, logging and warm-up settings.
Keep scripts/summaries in Git and raw recordings in the documented persistent
location. Record reset commands and links from story 2.
Estimate: 5–10 minutes active; fixture setup/request/test runtime exempt.
Safe stop: reusable representative valid baseline, no 10,000-note measurement yet.

Proof: same focused `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile`
passed twice, with reset. Captures `2026-09-11T10-25-25.854Z` and
`2026-09-11T10-26-02.055Z` have accepted 20-document byte checks, CLI intervals
591/702 ms, JFR intervals 951/1127 ms, MySQL server 8.4.11, and identical:

- Baseline fingerprint `a5cc67ce633434bdee57221f3b040912b3773d021668c4e96898d2ae4af0b3c8`;
- Proposal fingerprint `6b977c13512adb8990aa0b1b707acd081584ad8968f412ce9219ede798b244b5`;
- Baseline head `650445c7f56dde3ed3afbf4f1ca8e94794757b0c`;
- Accepted head `62940858301bcc5c0f4bc7ab32e16a979bf311bd`.

20 existing + 20 additions across 20 folders, plus original feature background.
Fixed backend fixture clock and proposal dates, UNDERSTANDING tracker captured.
Approximately 10 minutes active; test runtime exempt. Diagnosed and corrected
default import, tracker enum and eager resnapshot SDK invocation (now queued
after seeding). Failed attempts retained incomplete; repeated successful runs
prove the corrected ordering. Fresh refactor changed documentation precision
only, returned `REFACTOR COMPLETE`; original proof remains valid.
Timing wording clarifies the original planned CLI boundary: HTTP-only timing
belongs to a longer-wait harness if the existing 60-second CLI E2E wait is exceeded.

### 4. Measure rejection and verify preserved baseline state
Type: Behavior
Status: done
Proof: The small representative invalid variant rejects at the intended final
addition and leaves accepted head, stored content, existing note identities and
learning records unchanged, with complete timing and readable recording.

Change only the last processed document to invalid recognized-alias YAML shape.
Verify processing order and actual late rejection using request evidence; do not
infer it merely from filename. Reuse public publication boundary and existing
E2E fixture/observation facilities. Preserve successful baseline proof. A longer
wait harness for the same HTTP operation is permitted if needed, clearly labeled
HTTP/server timing; do not change production timeouts or claim CLI success.
Estimate: 5–10 minutes active; required request/test runtime exempt.
Safe stop: both outcome measurements reproducible at small scale.

Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose 'tags=@publicationProfile or @publicationProfileRejection'`
passed 2/2 (13 seconds), including after fresh refactor. Rejection recordings
`2026-09-11T10-31-50.438Z` and `2026-09-11T10-32-37.440Z` rejected
`group-19/Added-00019.md`; note auto-increment advanced by exactly 19 while all
note rows, tracker rows, binding head/bundle hash/timestamps remained unchanged.
Public receiver pull retained clean baseline head/tree. JFR readable request
samples retained, no active recording. CLI intervals 683/574 ms; capture intervals
1174/1022 ms. This allocation observation proves late processing, not DB duration.
Approximately 8 minutes active. Independent refactor extracted cohesive
`notebookPublicationState.ts` and shared CLI timing; required marker returned.

### 5. Observe complete publication through a longer-wait benchmark request
Type: Behavior
Status: done
Proof: The same representative small valid/rejected fixtures use the same public
Git-bundle HTTP operation, record request-only elapsed time and HTTP outcome,
and pass existing acceptance/preservation observations with readable JFR.

The installed CLI E2E wait is 60 seconds (`cliE2eManagedPty.ts`), whereas the
motivating workload consumed about 807 seconds of CPU before rejection. That
known runner bound cannot observe the expected large request. Use the originally
permitted benchmark-only HTTP transport with a longer wait; keep bundle setup
outside request timing and use existing fixture/bundle and owned target facilities.
Retain installed CLI smoke scenarios; distinguish HTTP measurement in explicit
steps/scenarios and metadata. Do not change product transport, timeout, acceptance
or authorization. Preserve incomplete outcomes and recording cleanup on failure.
No independent acceptance implementation: submit the existing public operation
and reuse its accepted-head/download and preserved-state observations.
Estimate: 5–10 minutes active, required runtime exempt. Safe stop: longer-wait
measurement path proven small before expensive runs. This refinement addresses
the observed runner boundary, without changing story outcome or fixture design.

Proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=3660000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'`
passed 2/2. Captures `2026-09-11T10-39-22.583Z` (HTTP 200, 127.517 ms) and
`2026-09-11T10-39-28.585Z` (HTTP 400, 131.847 ms) verify full response, accepted
content or complete baseline preservation and 19 preceding additions. Request
JFR stacks readable. Configurable deadline branch proved with 1 ms: failed,
retained incomplete timing/JFR (`2026-09-11T10-40-27.983Z`), no active recording.
Approximately 9 minutes active implementation; runtime exempt.

Optional four-scenario regression did not run: Electron failed its CDP connection
on port 55320 with ECONNRESET after 62 retries, before scenarios/capture. No
lingering Cypress process; root cause unestablished, not an application failure.
Selected installed Chrome 153.0.8010.36 for large runs and proved that runtime:
`CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=3660000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'`
passed 2/2 (12 seconds).

Host limitation: fresh coordinator and child agent creation both returned
`agent thread limit reached`. Reused the prior rejection implementer as an
independent reviewer of HTTP code written by another agent; no edits or repeated
runtime tests, `REFACTOR COMPLETE`. This is a fresh-agent workflow deviation;
independent review was retained. Subsequent delegation must reuse available
agents while that host limit remains. No observer exists (main-only CI).

Large-run commands must also raise Cypress defaultCommandTimeout for baseline
injection (ordinary default 6 seconds). Request-only timing excludes that setup.
JDK 25 JFR.start help lists default maxage 0 and maxsize 0, but the first large
run reports an effective default maxsize of 250 MB. Verify full start/end coverage
of each completed recording before accepting its profile evidence. Keep the same
JFR profile and JVM flags;
TieredStopAtLevel=1 is a material local-environment limitation.

### 6. Preserve and explain the first valid large-publication baseline
Type: Behavior
Status: done
Run 1 started at source revision `b650a294f644dcdb24a622c3d801621fc881aaee`:
`PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=10000 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=43200000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=43260000,defaultCommandTimeout=600000 --expose tags=@publicationProfileHttp`.
Driver log: `/Users/terryyin/Library/Application Support/Donut/publication-profiles/large-valid-1-driver.log`.
Agent `capture_small` owns exec session 23633; coordinator must not concurrently
read its PTY. Preserve an in-flight server request and recording on interruption.

Run 1 returned HTTP 200 at 12:13:35.494 UTC after 4,987,017.426 ms
(83 minutes 7 seconds), accepted head `766dbfdbc7a32df0db60069a5e12c11d8f651538`.
Cypress failed before JFR-stop/receiver checks; its exposed error is an automatic
screenshot timeout, with original cause still under investigation. Do not call
this a passing Cypress scenario. A manually preserved
`completed-request-recovery.jfr` is readable, 71.7 MB, starts 10:50:27 UTC and
contains request processing through transaction commit and MVC response handling
(no 250 MB truncation). Independent public pull verified clean accepted head/tree
and all 10,000 added document bytes (`recovery-result.json`). Manual JFR.stop saved
`publication.jfr` (72.9 MB); the failed runner was then terminated, leaving backend
PID 2406 running. No second large run starts until the continuation probe finishes.

Material environment finding: `pmset` confirms clamshell sleep from local 19:12:05
to full wake 19:37:27 (UTC 11:12:05–11:37:27), with intermittent dark wakes.
`host-sleep.log` preserves only sleep/wake events. The HTTP elapsed includes sleep;
JFR event timestamps drift about 24 minutes behind wall timestamps by request
completion. Commit/response samples prove the request tail was retained, but
wall-window filtering and an uninterrupted wall-time claim are invalid. Analyze
the observed request-thread span and retain this caveat; do not subtract sleep
as a fabricated server duration. Repeat with a scoped idle-sleep inhibitor and
record whether any further host sleep occurs; lid closure can still suspend it.
Proof: The completed 10,000-addition HTTP response, public receiver head/content
verification, and full request-window JFR evidence establish one inspectable valid
baseline, honestly retaining the original runner failure and recovery commands.

Preserve recordings and exact revision/fingerprint; summarize request-window CPU,
allocation/GC and wait evidence and distinguish the longer recording interval.
The first run is already verified at the public boundary. Complete the bounded
analysis/documentation, without rerunning valid proof or changing product code.
Estimate: 5 minutes active analysis/documentation, profile processing runtime exempt.
Safe stop: one complete valid baseline and reproducible recovery evidence exist.

Delivered analysis and reusable `scripts/profiling/AnalyzePublication.java`:
165,525 observed publication-thread samples, 162,631 (98.25%) containing Hibernate
flush traversal; allocation/GC/wait evidence and sampling limits documented.
The first latency includes confirmed host suspension; no uninterrupted latency
claim. Approximately 9 minutes active analysis plus processing runtime.
Independent refactor made the script readable, reran the documented analysis
against the same recording, and verified identical output and whitespace.
`REFACTOR COMPLETE`; no product tests/API generation triggered. Coordinator
selective format passed with no fixes. Raw profiles and analysis outputs remain
in the persistent capture directory; permanent findings link their exact commands.

### 7. Establish reliable continuation for long measurement
Type: Behavior
Status: planned
Proof: A controlled short delayed task through the same nested Cypress/Cucumber
boundary identifies or rules out the timeout hypothesis, preserves the original
failure before screenshot handling, and demonstrates the chosen repeat command's
completion/failure observation. Do not claim an unobserved root cause or fix.

The first request completed but its browser driver failed before recording stop
and public receiver assertions. An automatic screenshot timeout masked the original
error. Ordinary task timeout was already 12 hours; source inspection alone does
not prove nested then callbacks retained the 10-minute default timeout. Use a
disposable short probe without resetting the measured SUT. If necessary, apply
only the smallest supported benchmark continuation correction and prove both
accepted/rejected small outcomes; preserve the same HTTP and acceptance boundary.
No parallel acceptance implementation or production timeout change. Expose original
runner errors and document recovery when completion has already occurred.
Estimate: 5–10 minutes active, required probe runtime exempt. Safe stop: the
repeat's observation path is evidenced before another expensive request.

### 8. Repeat the valid large-publication baseline
Type: Behavior
Status: planned
Proof: A second successful 10,000-addition publication from a reset logical
baseline has identical fingerprints, public accepted head/content proof, and a
complete comparable profile with recorded timing variation.

Use the evidenced continuation path after slices 6 and 7 are delivered. Keep the
same product revision and fixture/JVM/profile settings; documentation or benchmark
observation changes must be recorded precisely. Analyze the second complete
request against the first. Product code changes require fresh matching baselines.
Estimate: 5 minutes active orchestration/analysis; long request runtime explicitly
exempt. Safe stop: repeatable valid baseline without optimization claims.

### 9. Establish large rejection baseline and improvement-area handoff
Type: Behavior
Status: planned
Proof: The approximately 10,000-addition invalid variant completes rejection with
preserved-state proof; maintainers can follow story links to both outcome profiles
and evidence-backed improvement areas in story 3.

Run from the same logical starting state and settings as valid measurements.
Confirm actual rejection point and unchanged accepted head, content, note IDs and
learning records. Compare evidence with static hypotheses; record supported areas
and uncertainties in story 3 with shared infrastructure/data links in stories 2–3.
Include profiler/logging overhead and sampled-wait limitations; SQL counts alone
do not establish database time. No quantitative speedup predictions, optimization
implementation, or story 3 execution plan.
Estimate: 5–10 minutes active analysis/documentation plus measured runtime exception.
Safe stop: selected story's reproducible evidence and handoff complete.

## Execution learning and refinement

Second refinement: large-run orchestration and recovery reached approximately
10 minutes active work, separately from the completed 83-minute request. The
assumption that a small passing Cypress task would retain reliable browser
continuation for the full large request was not established. No attempt-owned
repository edits exist; completed artifacts are safely persistent, the recording
is stopped, and the failed runner is terminated without touching the backend.
Story boundary reassessed: the delivered public-boundary measurement is useful;
remaining scope is still the same repeatable valid/rejected profiling outcome.
No new product behavior, architecture, or sibling ordering is needed. Separate
bounded profile analysis, a cheap continuation probe, the repeat, and rejection
because those have independently evaluable proof and different runtime risks.
The common model remains the existing public HTTP operation and receiver checks.
Result: 9 slices, no resplit recommendation; execution can resume.


Original slice 2 reached its 10-minute active-work hard limit. Its single-slice
assumption incorrectly combined capture lifecycle, representative fixture,
persisted acceptance and repeatability. Same story outcome remains understood;
no product scope or architecture changes. Remaining work now follows one capture
and workload model; later slices extend proven outcomes, not parallel harnesses.

Attempt owned six E2E files, safely parked in stash
`118c890f201d5773d8a17801f8da512e5e190d0d` before refinement. Restored onto
`cda93cdccc`, verified inventory, and dropped that exact stash before completing
slice 2. The following failure remains prior-attempt evidence.
`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --expose tags=@publicationProfile`
failed at JFR.stop after publication because jcmd requires embedded quoting for
paths containing spaces. The quoting fix is parked, unverified. Head/content
assertions were not reached. No active recording or attempt subprocess remains.
Recovery command succeeded:
`CURSOR_DEV=true nix develop -c jcmd 2406 JFR.stop name=publication 'filename="/Users/terryyin/Library/Application Support/Donut/publication-profiles/2026-09-11T10-11-43.355Z/incomplete.jfr"'`.
That directory retains metadata and the 825.8 kB incomplete recording.

Owned SUT is now healthy at `http://127.0.0.1:50811`, backend 50809, Vite 50810,
identity `wt_539226243fb14a2391240fed882700b2`. The retry after initial build race
failed with missing DonutApplication.class despite up-to-date Gradle status.
Stopped only the verified owner using `beginSutOwnerShutdown`, then
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend clean classes --no-daemon`
and `CURSOR_DEV=true nix develop -c pnpm sut` passed. This repairs local compiled
output, not the underlying pre-existing startup race.

## Verification and delivery

The actual runs above are the outside-in proof; do not introduce timing
assertions into routine CI. Check the opt-in runner with a small fixture and
run focused tests for touched harness behavior. Backend changes, if necessary
for capture, require `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
(the full backend suite). Preserve existing atomic publication controller proof.
No manual browser testing or product API changes are planned.

During execution follow dough-execute-plan: move story 2 to Taken only when
execution starts; Jidoka, fresh dough-post-change-refactor agent, API generation
only if required, one coordinator `./scripts/run.sh pnpm format:changed`, update
plan, commit with check-only hook, push and asynchronous CI repair. Retain plan
and evidence through retrospective; keep reusable infrastructure/data reachable
when later cleaning up story history.

## Assessment

No blocking product questions. Slices 2–3 have sizing uncertainty around JFR
attachment, existing E2E helper reuse, client timeouts and full-workload duration.
The small representative proof limits infrastructure risk before expensive runs.
The three slices share one workload and capture path; no general framework,
dashboard, optimization design, or independent benchmark platform is included.
Planning has not run profiling or tests, and records no measured bottleneck yet.
