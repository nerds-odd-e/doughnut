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

Six sequential Behavior slices, each exposing one evaluable learning outcome.
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
Status: planned
Proof: A parameterized small-scale run creates the documented existing-note
baseline, publishes deterministic additions, verifies accepted content and head,
and repeats after reset with the same logical fixture fingerprint.

Extend the same fixture/capture workflow to the documented content distribution,
fixed Git identity/dates, existing learning record and parameterized counts.
Record exact implemented distribution and representativeness limits. Capture
request timing separately from setup and JFR interval, result/head, fixture
fingerprint, revision, MySQL/JVM versions, logging and warm-up settings.
Keep scripts/summaries in Git and raw recordings in the documented persistent
location. Record reset commands and links from story 2.
Estimate: 5–10 minutes active; fixture setup/request/test runtime exempt.
Safe stop: reusable representative valid baseline, no 10,000-note measurement yet.

### 4. Measure rejection and verify preserved baseline state
Type: Behavior
Status: planned
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

### 5. Establish repeated valid large-publication baseline
Type: Behavior
Status: planned
Proof: Approximately 10,000 additions publish successfully twice from the same
reset logical baseline, with accepted head/content proof and comparable profiles.

Use the proven workflow, preserve recordings and exact revision/fingerprint,
record warm-up and variation, and summarize CPU/allocation/GC and wait evidence.
Distinguish client timeout from completed server request. An interrupted request
is incomplete evidence. Later code changes require a fresh matching baseline.
Estimate: 5 minutes active orchestration/analysis plus potentially long requests
(prior observation about 807 seconds request CPU), an explicit runtime exception.
Safe stop: reproducible success baseline without optimization claims.

### 6. Establish large rejection baseline and improvement-area handoff
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
