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

Three sequential Behavior slices, each exposing one evaluable learning outcome.
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

### 2. Reproduce a measured publication on disposable data
Type: Behavior
Status: planned
Proof: An opt-in small-scale run through the real HTTP publication boundary
produces a readable recording, timing/result metadata, and a verified accepted
head/content; reset and rerun establish the same logical starting state.

Use existing CLI E2E setup, Git fixtures, target ownership and cleanup. Extend
only the missing deterministic workload and capture support. Record literal
commands, Git revision, JVM/MySQL versions, fixture fingerprint, warm-up and
logging settings, backend identity, output locations and reset instructions.
Keep setup outside the timed publication. Store scripts and concise summaries
in the repository; retain raw recordings in a documented persistent artifact
location linked from the story, not an expiring temporary directory.

The concrete uncertain assumption is that JFR can record the owned SUT JVM
while this E2E path submits and observes publication. Prove it here before a
large run: after resolving the owned PID and output directory, use
`CURSOR_DEV=true nix develop -c jcmd <owned-backend-pid> JFR.start name=publication settings=profile`
and after the request
`CURSOR_DEV=true nix develop -c jcmd <owned-backend-pid> JFR.stop name=publication filename=<absolute-recording-path>`.
Replace placeholders with the verified values and record the exact executed
commands and result. Required postcondition: readable samples spanning the
request plus persisted acceptance. Not yet run. Failure changes the capture
approach in this plan before proceeding, without adopting a shared JVM.

Estimate: 5–10 minutes active work, low confidence because capture integration
is unproven. Safe stop: the small reproducible profiling workflow works; no
large-performance claim. This owns reusable infrastructure and repeatability.

### 3. Establish the large baseline and supported improvement areas
Type: Behavior
Status: planned
Proof: A maintainer can follow story links to completed valid/rejection baseline
data and see which improvement areas are supported by those measurements.

Run the same workflow at approximately 10,000 additions and repeat the valid
run after reset, documenting warm-up and variation. Run the late-invalid variant
from the same logical baseline. Confirm its actual rejection point and unchanged
accepted head, stored content, existing note identities and learning records.
Verify accepted head and added content on success. Preserve raw recordings and
timings with the exact revision and fixture; later code changes require a fresh
matching baseline, not silently mixed results.

Distinguish client timeout from server completion and retain both observations.
If CLI timeout prevents a complete measurement, a documented harness-only
request with a longer wait may observe the same HTTP operation; label it as
HTTP/server timing rather than successful CLI timing. Do not change production
timeouts. A stopped server request is incomplete evidence and cannot close this
slice. Record profiler/logging overhead limitations and avoid unsupported
quantitative speedup predictions.

Compare measurements with slice 1's hypotheses. Put the supported improvement
areas, evidence and uncertainties in story 3, linking the shared infrastructure
and data there and in story 2. No optimization implementation or execution plan.

Estimate: 5–10 minutes active analysis/documentation plus potentially long
baseline runs (the prior observation included about 807 seconds of request CPU).
Safe stop: a reusable baseline and evidence-based next-story statement exist.
This owns both large-run outcomes, preserved-state proof and the story handoff.

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
