# Observe CI throughout execution with one setup

Source: [SEED-011 Story 1](../../seeds/SEED-011-efficient-ci-failure-attention.md#story-1).
Status: planned.

## Outcome and boundaries

For the developer supervising plan execution, one coordinator setup observes
the repository's main-branch CI across successive pushes and delivers actionable
failures without model polling or repeated per-revision launches. Startup is
nonblocking; observation ends with execution.

Representative behavior: execution begins while one older CI run is unfinished
and the latest completed run has failed → the coordinator starts observation
once and continues bounded work while further commits are pushed → it receives
the startup failure and newly failed jobs, including in older unfinished runs,
without waiting for their workflows to complete or launching another observer.
Stopping execution stops local observation and preserves unresolved evidence.

This plan selects Story 1, the first independently useful increment. It adopts
the researched startup baseline and the developer's explicit shutdown policy.
Story 2 retains the broader change to repair scheduling, durable repair ownership,
cause-equivalence decisions, and compaction/resumption of interrupted work. Keep
the existing safe repair workflow usable; make only the event-identity and
revision-scope adjustments necessary to consume this story's notifications.
Never stash under a live writer or introduce forced interruption. The recommended
bounded-task attention policy remains Story 2's planning input.

CI means `.github/workflows/ci.yml`, named `donut CI`, for push events on `main`.
Exclude CD, workflow dispatch/reruns initiated by the observer, other branches,
AI-based polling, persistent monitoring after execution, and unrelated product
changes. GitHub access is read-only. No intentionally failing remote commit or
workflow run is needed for proof.

## Execution context

- `watch-ci.mjs` currently polls one SHA, checks earlier run attempts, and returns
  one terminal event. It inspects jobs only after workflow failure. Its existing
  `watchCi` boundary and `watch-ci.test.mjs` are the starting point for observer
  behavior; mock GitHub responses, not internal collaborators.
- `ci-mailbox.mjs` starts a detached worker and writes one terminal `result.json`.
  `ci-host-hook.mjs` consumes the owner's binding on the first result. Both need
  incremental event delivery before a persistent observer can replace them.
- `ci-mailbox.test.mjs` exercises real launcher/worker/hook processes with a fake
  `gh` executable. `ci-host-hook.test.mjs` verifies owner isolation, Cursor
  generation binding, readiness, cancellation, and host-specific output.
- The Codex adapter in `references/ci-monitor.md` currently accumulates stdout
  and calls `notify` only after the process exits. Continuous observation must
  process complete event lines as they arrive, retaining an incomplete tail.
- `scripts/test/ci_observer.test` already includes the skill's `*.test.mjs`
  files in the CI script-test job. Keep the Node test runner for these existing
  JavaScript tools; `.cursor/rules/script.mdc` governs any shell wrapper changes.
- Follow [ADR 0006, Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  catches need a deliberate outcome. Reporting lost coverage and retaining a
  known failure when additional evidence is unavailable are such outcomes.
  Product Failure report grouping does not define CI failure equivalence.

Paths above are relative to `.agents/skills/execute-plan/scripts/` unless a
different directory is specified. Shared instructions are in
`.agents/skills/execute-plan/{SKILL.md,references/ci-monitor.md,references/ci-notify-hosts.md,references/wrap-up.md}`.
Changes overlap; execute the leaves sequentially.

## Outside-in proof

Use the observer entry point for run/job scenarios and the real process fixture
for launch → GitHub responses → persisted event → owning host delivery → stop.
Reuse existing fixtures; extend them with run creation time, attempt, job ID,
job completion, and a scripted sequence of GitHub snapshots. Test observable
event sequences instead of exact incidental CLI argument lists.

Focused commands, from the checkout root:

```sh
./scripts/run.sh node --test .agents/skills/execute-plan/scripts/watch-ci.test.mjs
./scripts/run.sh node --test .agents/skills/execute-plan/scripts/ci-host-hook.test.mjs .agents/skills/execute-plan/scripts/ci-mailbox.test.mjs
./scripts/run.sh bash scripts/test/ci_observer.test
```

Add a capability-named test file beside these only if the new branch-observation
entry point merits one; the existing shell wrapper discovers it automatically.
Use the specific new filename for focused proof when applicable. Final proof is
the wrapper plus the actual Codex demonstration in leaf 9, not the full product
suite. Host hook process tests are not proof that an installed Cursor or Claude
Code application has loaded its configuration.

## Current decisions

1. Observation belongs to one execution/checkout/coordinator, repository, and
   branch. Routine pushes, including repair pushes, do not require SHA discovery
   or another launch. Repeated setup for that execution reuses its known live
   observer; unrelated coordinators cannot consume its events.
2. Startup includes the newest completed matching run by creation order and all
   unfinished matching runs. Preserve explicitly retained unresolved events.
   Discover runs appearing during execution, including another session's pushes.
   Paginate discovery and retain tracked run IDs independently of the newest
   page. An older unfinished run must remain observable after newer runs finish.
   Do not equate a temporarily empty listing with failed CI or lost coverage.
3. Preserve repository, workflow, branch, SHA, run ID, attempt and job ID. Repeated
   sightings of the same failed job add no notification; a later failing sibling
   or new attempt adds evidence even after the run was first reported. A successful
   rerun does not erase a known earlier failure. Terminal run-level failure remains
   actionable if jobs/history cannot be fetched. Cancellation is incomplete CI.
4. Separate recorded event evidence, host delivery progress, and observer terminal
   status. Store recovery evidence outside the checkout so stashing cannot remove
   it. Delivery must not imply that a failure was repaired. Reuse the mailbox and
   host adapters, with incremental records; do not build a general message broker.
5. Keep 30-second script polling, bounded GitHub calls, and bounded transient-error
   retries. No polling/model turn is needed for pending or successful CI. The
   one-SHA discovery/total-poll limits cannot govern a whole execution. Explicit
   coordinator shutdown governs normal lifetime; retain a documented finite
   eight-hour orphan timeout as a failsafe, with one lost-coverage event if it
   expires. This exceeds the selected story's 1–2 hour hypothesis; expose an
   explicit finite setup override for longer planned executions. The budget is
   declared at setup and is not renewed after pushes.
6. Stop on completion, cancellation, Jidoka stop, or coordinator replacement.
   Confirm the known process exits; drain already-recorded events and report
   pending/unobserved runs without waiting for GitHub. Cancellation must not use a
   stop hook to restart cancelled work. Surviving records are a handoff, not a
   persistent monitoring process.
7. Keep the existing per-SHA command working during the transition. The new
   execution mode is explicitly usable before becoming the default. Change
   default coordinator instructions only once its delivery and shutdown proofs
   pass; then retire unused per-SHA paths in that activation leaf if all callers
   have moved. Do not run old and new observers for the same execution.
8. Preserve unrelated dirty files. At planning time the backlog and SEED-009
   have changes owned elsewhere; SEED-011 contains this discussion's research.
   This plan does not authorize committing those unrelated changes.

## Ordered slices

Each estimate includes focused proof, targets roughly five minutes, and is a
hypothesis rather than a guarantee. Scrutinize after five minutes; apply the
repository's ten-minute refinement rule when needed.

### 1. Reuse CI attempt inspection without changing single-revision observation
Type: Structure
Status: planned
Proof: existing `watch-ci.test.mjs` passes with unchanged observable results.

Internal change: separate GitHub run/attempt inspection and failure event
construction from the single-SHA polling lifecycle. Keep the existing command
and `watchCi` behavior, including earlier-attempt and partial-evidence handling.
This immediately enables leaf 2 to observe multiple runs without duplicating
classification. Extract only that shared concept; no generic scheduler or
transport abstraction. Estimate: approximately 5 minutes, medium confidence.

### 2. Follow failures across successive pushes through one running command
Type: Behavior
Status: planned
Proof: observer/process entry point receives an older pending run, a newer run,
then both completed failures over successive snapshots; one invocation emits
both run identities and remains running until cancelled.

Behavior: an execution-mode observer is running on main → more push CI runs
appear while earlier runs are unfinished → completed failures from each relevant
run are reported without supplying another SHA or starting another process.

Add an opt-in execution mode with incremental output, reusing leaf 1's inspection.
Seed unfinished runs and discover new runs with pagination and a stable startup
boundary; retain tracked IDs when they leave the discovery page. Preserve silent
success, per-attempt identity, basic AbortSignal cancellation, and a declared
finite execution-wide observation budget. The per-SHA path remains the default.
Document the opt-in command and its interim limitation: recently completed
startup failures arrive with leaf 3; in-workflow failures arrive with leaf 4.
Estimate: 5–10 minutes, medium-low confidence; see readiness below.

### 3. Report an existing failure at nonblocking startup
Type: Behavior
Status: planned
Proof: latest-created completed run fails, an older completed run is unrelated,
and a pending run exists → the startup failure is emitted once while observation
continues. An asynchronously held GitHub response permits unrelated caller work;
the detached launch/host proof is completed in leaf 7.

Behavior: execution starts with failed CI already present → startup discovery
runs alongside other work → the newest completed matching run's failure is
reported through the same event stream. Include its earlier failed attempts.
Select by creation order, not whichever old run happened to finish last. Empty
history is valid; the discovery boundary must not drop a run completing during
the initial snapshot. Keep older completed history outside the chosen baseline.
Estimate: approximately 5 minutes, medium confidence.

### 4. Report each failed job before its workflow finishes
Type: Behavior
Status: planned
Proof: one job fails while a sibling remains running, repeated snapshots show
the same failure, then the sibling fails → two distinct job events arrive before
workflow completion, without repeat notifications for the first job.

Behavior: a tracked run remains unfinished → a job reaches a failing terminal
state → its identity and conclusion become actionable immediately. Inspect jobs
of unfinished attempts and emit new evidence by run/attempt/job identity. Retain
attempt-history proof and a run-level fallback when the run is known failed but
job details are unavailable. A later successful job or rerun cannot suppress
recorded evidence. Extend the shared consumer guidance in the same leaf so
deduplication of a run/attempt never discards new sibling-job evidence.
Estimate: approximately 5 minutes, medium confidence.

### 5. Surface lost observation coverage once
Type: Behavior
Status: planned
Proof: an event is already recorded, followed by persistent GitHub errors or the
declared observation budget expiring → exactly one coverage-loss event follows
the retained failure and the observer terminates without a success claim.

Behavior: execution observation can no longer provide coverage → its bounded
recovery allowance is exhausted → the coordinator gets one actionable lost-
coverage result. Keep transient recovery quiet and incomplete/cancelled CI
distinct. Catch for this explicit outcome; do not silently skip tracked runs
after pagination/job/history errors. No model-driven retry loop or recurring
status turns. Estimate: approximately 5 minutes, medium confidence.

### 6. Separate mailbox evidence from worker completion
Type: Structure
Status: planned
Proof: existing mailbox/hook tests retain single-event delivery, owner isolation,
readiness, and terminal stop behavior.

Internal change: give the mailbox an appendable sequence of atomically published
event records and separate terminal status/delivery progress. Route the existing
single terminal event through that representation while preserving caller
behavior. Reuse this representation for both foreground Codex output and native
detached workers. This immediately enables leaf 7; avoid separate per-host event
stores or a second failure-classification implementation.
Estimate: approximately 5 minutes, medium confidence.

### 7. Deliver successive events to the same owning coordinator
Type: Behavior
Status: planned
Proof: real launcher/worker/hook fixture publishes two failures at different
times before worker completion → the owning coordinator receives each once;
an unrelated task or child cannot consume either, and empty delivery is silent.

Behavior: an execution-mode mailbox is attached once → another failure arrives
after the first was delivered → the same coordinator receives the new event
without rebinding or launching a replacement. Persist before delivery; consuming
one event must leave the active binding available for subsequent events. Support
the existing Cursor and Claude output shapes and generation/owner protections.
Extend the process fixture's scripted fake GitHub source rather than bypassing
the launcher with fabricated hook output. Preserve one-shot caller compatibility.
Estimate: 5–10 minutes, medium-low confidence; see readiness below.

### 8. Stop execution observation without losing unread failures
Type: Behavior
Status: planned
Proof: a failure is persisted but unread and another GitHub request is blocked →
stop the exact observer → its subprocess exits promptly, the unread failure is
still available, and the remaining run is reported as pending/unobserved.

Behavior: execution ends while observation is active → the coordinator requests
shutdown → local polling stops and accumulated evidence remains available for
handoff. Exercise abort during a request and publication/stop races through the
existing process test boundary. Stop must not replace prior evidence with an
empty terminal result. Return a terminal receipt with coverage state, keep stop
idempotent, and preserve cancellation's no-auto-continuation contract.
Estimate: approximately 5 minutes, medium confidence.

### 9. Attach execution observation once in Codex
Type: Behavior
Status: planned
Proof: actual Codex yielded-cell bridge receives two labelled synthetic events
from one running observer command while ordinary read-only coordinator work
continues; stopping confirms process exit and retains unread evidence.

Behavior: a Codex coordinator starts execution → it establishes one observer and
bridge → later failures reach its active context without per-push model setup.
Use the shared mailbox worker in a foreground/streaming command mode for Codex,
with the same persisted records and stop semantics as detached mode. Keep the
small yielded-cell adapter host-owned; process complete JSON lines incrementally
and retain chunk tails, including events before the first yield. Retain one
execution key and exact process/storage handles for shutdown and resume.

Update Codex launch instructions and the coordinator startup/wrap-up/repair-push
references together: pushes do not relaunch; native hosts retain their existing
path until leaf 10. Accept startup/other-session SHAs for diagnostic scope, then
check ancestry/current-code applicability before repair; never silently ignore
them because this coordinator did not push them. No repair ownership redesign.
Estimate: 5–10 minutes, medium-low confidence; see readiness below.

### 10. Use one setup throughout execution on native hook hosts
Type: Behavior
Status: planned
Proof: the real CLI/hook process fixture for Cursor and Claude performs one
setup, multiple run/job failures, and shutdown through the documented command.
On each actual native host used for execution, require its readiness probe and
one labelled event delivery before claiming connected observation.

Behavior: a native-host coordinator begins execution → it starts and binds one
execution observer → successive pushes use that observer until execution ends.
Update `ci-notify-hosts.md`, shared startup/wrap-up instructions, and command
validation for repository/branch/execution identity. Reuse a known live handle
on repeated setup in the same execution; preserve existing owner protections.
Remove obsolete per-SHA default instructions/call paths once callers are migrated.
An unavailable actual host is reported as unverified; process fixtures do not
justify a claim of installed-host delivery. A missing bridge reports lost
coverage once and allows execution to continue as the existing contract requires.
Estimate: approximately 5 minutes, medium confidence.

## Readiness and wrap-up

Refinement recommended: leaves 2, 7, and 9. Each has one externally evaluable
outcome, but moving from terminal to continuous observation and proving the live
streaming adapter crosses lifecycle boundaries that can plausibly exceed ten
minutes. Leaf 1 and leaf 6 isolate immediately necessary Structure; further
splitting must preserve a usable command/event-delivery path rather than produce
unused layers. Refine in this same PLAN before execution, informed by the
existing command and host boundaries recorded above.

The initial plan has split startup replay, early job reporting, lost coverage,
unread-event shutdown, and per-host activation into separate outcomes. Only the
`<refinement_triggers>` gate was read during planning; refinement and feature
execution have not been run.

During execution, use the existing observer for this plan's pushes until the
new host path passes its activation proof; changing its source does not hot-swap
an already-running process. At activation, explicitly stop the old known handles
before starting the replacement, preserving their pending evidence. The one-time
migration is distinct from repeated routine per-push setup.

Per leaf: pre/post Jidoka, fresh post-change-refactor agent, focused proof,
coordinator's one `./scripts/run.sh pnpm format:changed`, plan update, commit and
push. No generated API change is expected. Keep permanent contracts in the
existing skill references; remove spent execution history when complete.

Final acceptance: one setup across several simulated pushes; zero model turns
for successful/pending polls; distinct early failures delivered; startup and
shutdown boundaries honored. Record setup/lifecycle model calls and available
output-token usage against the seed's 14 launches / 5,716 launch-response output
tokens, separately from diagnosis/repair. Synthetic snapshots establish behavior,
not a claim of equivalent real-world timing or cost. Record notification and
repair-attendance timestamps separately if a real CI failure occurs naturally.

## Learnings

- Planning baseline at `2950fb9095`: all 34 existing observer/mailbox/hook tests
  passed using the three-file Node command above; Node reported about 2.6 seconds
  for the tests, excluding Nix startup. No product test suite was needed.
- A harmless `seed-011-planning` notification reached this Codex coordinator
  after a yielded cell while another tool call ran, and the cell was reaped.
  This verifies the exposed `notify` path only; process streaming, GitHub
  discovery, compaction, and native installed-host delivery remain unverified.
- The old single-SHA adapter and mailbox terminate delivery at the first result.
  Simply removing the SHA filter would not satisfy this story.
