# Release backend worktree test ownership after execution

Source: execution retrospective for quick plan 101 and SEED-017 Story 2b,
recoverable from before-cleanup commit `056ddef4ba` at
`.planning/quick/101-publish-compatible-note-moves/PLAN.md` and
`.planning/seeds/SEED-017-cohesive-design-corrections.md`; reviewed through
commits `8983d9db3f`, `65d88f55cc`, `be76f0d471`, and `1833e29ead`.

Status: story refined 2026-09-11; implementation not requested. The existing
execution slice remains provisional pending the lifecycle design concerns below.

## Finding and bounded outcome

Developers and AI tasks using an isolated linked or configured worktree can run
a supported backend test or migration command without manually clearing normal
invocation residue before retiring the worktree. Once the invocation's owned
work has finished, it releases its ownership record. Retirement can still refuse
for other legitimate evidence; this story does not promise that every completed
command makes a checkout eligible for retirement.

Quick plan 101 exposed the current failure after its successful backend suites:
`pnpm worktree:retire --check` refused a dead PID 23796 recorded in
`.worktree.local.lock`. `scripts/backend-test-worktree-owner.sh` writes the
invocation PID, both supported launch paths hand off with `exec`, and no normal
completion path removes the owned lock. The next backend invocation can reclaim
that stale record, but retirement deliberately and correctly refuses to do so.
This weakness predates quick plan 101; it is not a regression in note publication.

### Assumption and solution challenge

Source inspection on 2026-09-11 confirms the missing release path in
`scripts/backend-test-worktree-owner.sh`, `backend-test-worktree.sh`, and
`backend-worktree-gradle-route.sh`. This refinement did not rerun a real backend
suite or reproduce retirement against live databases.

- **Is cleanup necessary if the next run reclaims the lock?** Yes: the next
  useful action may be retirement, not another test run. Requiring an extra run
  or manual removal leaves the observed workflow defect intact.
- **Could retirement simply reclaim dead owners?** That would change the
  existing safety contract: a dead launcher does not prove its database work
  stopped. Keep normal release with the invocation that can observe completion;
  preserve retirement's refusal of ambiguous evidence under ADR 0007.
- **Does the lifecycle start at Gradle launch?** No. The shared preparation
  function records ownership before configuration validation, URL checks and
  MySQL provisioning. The ordinary test route also runs a preliminary migration
  before its final handoff. All these post-acquisition exits belong to this
  correction; pre-acquisition refusals must never release another owner.
- **Does a shell EXIT trap solve it?** Not by itself. The final `exec` replaces
  the shell, while introducing a supervising shell changes signal and wait
  behavior. The existing handoff is an implementation choice, not a product
  constraint. Choose one lifecycle owner that can observe all owned work ending.
- **Does a matching PID make any later deletion safe?** Only with a justified
  ownership/acquisition protocol. Establish why a competing owner cannot replace
  the record between verification and removal; do not assume a detached cleanup
  process or a read-then-delete sequence is sufficient.
- **Do existing green tests prove the outcome?** No. Stale-reclamation tests in
  `backend-test-worktree-lock.test.mjs` currently assert a PID record after exit;
  provisioning and linked-wrapper tests manually remove leftover locks. Those
  assumptions must change while preserving the actual stale-owner recovery proof.

The correction owns normal backend-command ownership release. It must not make
retirement reclaim stale or unverifiable records. It must preserve concurrent
owner exclusion, crash evidence, exit status and cancellation behavior, isolated
database selection, and retirement's fail-closed checks.

Accepted [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires proven worktree ownership and permits retirement to delete only that
worktree's disposable Unit Test and E2E data. The correction removes only an
ownership record still proven to belong to the completing invocation; ambiguity
continues to refuse. [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
keeps child-command and cleanup failures visible. No ADR conflict or exception
was identified.

## Scope and boundaries

Included:

- Give the backend worktree command lifecycle one owner-aware release operation.
- Cover the lifetime from successful ownership acquisition through preparation,
  preliminary migration and final test/migration completion. Release on success,
  ordinary failure, or handled cancellation only after owned work has ended,
  without deleting a successor's or peer's ownership.
- Preserve the documented terminal/process-group interrupt behavior. SIGINT and
  SIGTERM handling must not free ownership while database-using work survives.
  An uncatchable kill, crash, or unverified shutdown retains evidence rather than
  promising automatic release.
- Surface release failures. Preserve an existing command failure/cancellation
  outcome and report any cleanup failure additionally; a successful workload
  must not report overall success when its required ownership release fails.
- Keep abnormal termination recoverable by the existing stale-owner protocol.
- Align process-level tests and the backend-worktree/retirement documentation
  with the completed lifecycle.

Excluded:

- Changing `.sut.local.lock` or isolated SUT shutdown; that distinct issue is
  already recorded as `DearDough.md` DD-003 and needs its own product decision.
- Making `worktree:retire` stop live processes or reclaim stale/malformed locks.
- Changing database naming, allocation, port ownership, retirement targets, or
  primary-checkout defaults.
- A generic lock or service-management framework.
- Database rollback or orphan-database recovery after failed first-use
  provisioning; releasing invocation ownership does not establish a valid
  allocation or undo database administration already performed.

## Key examples and proof ownership

| Promise | Observable proof |
| --- | --- |
| A completed supported backend command leaves no owned checkout lock | Process-level opt-in, ordinary test and migration-only cases observe completion, preserved exit code, and absent `.worktree.local.lock` |
| Post-acquisition preparation or preliminary migration fails | Public-route cases observe the original failure, no later workload launch, and released ownership |
| Cancellation releases only after owned work is finished | Process-level signal cases hold work during shutdown, observe continued exclusion until it ends, then the cancellation outcome and absent lock |
| An overlapping command remains refused while the owner is live | Existing held-owner launcher/wrapper tests remain green |
| Crash evidence and retirement safety remain fail closed | Retirement refuses stale/malformed records without reclaiming; launcher tests preserve existing stale-owner recovery and malformed-owner refusal |
| Cleanup never removes foreign or unverifiable ownership | Public-route ownership-change case preserves the replacement record and reports the release problem |
| Workload success cannot hide failed release | Public-route release-failure case reports a nonzero outcome and visible failure |
| Both opt-in and ordinary wrapper routes share the lifecycle | One test for each public route reaches the same owner release behavior without a parallel cleanup rule |

These examples define lifecycle outcomes, not a separate implementation per
command. One representative retirement `--check` boundary case with otherwise
eligible evidence should show that normal completion no longer produces the
backend-owner veto; retain refusal coverage for other evidence.

## Ordered slices

### 1. Complete the backend worktree owner lifecycle
Type: Behavior
Status: planned (provisional after story refinement)
Sizing: the previous five-minute estimate is not substantiated by the newly
explicit preparation, signal and release-failure cases. Reassess through slice
planning before execution; keep one coherent lifecycle rather than splitting
implementations by launch route.

Behavior: given a linked or configured worktree whose supported backend test or
migration command owns `.worktree.local.lock` → the owned child completes,
fails, or is cancelled → the command returns the child's outcome and removes
only its own ownership record, so a subsequent retirement check is not blocked
by normal-completion residue; live, malformed, crashed, successor and peer
ownership remain protected.

Use one owner-aware lifecycle for both `backend-test-worktree.sh` and
`backend-worktree-gradle-route.sh`, covering acquisition through preparation and
workload completion. The existing handoff may change as needed. Establish
signal propagation, completion observation and safe release under the actual
process topology before choosing the mechanism. Preserve the child outcome and
retirement's evidence rules. No generic process framework is required by this
story; reuse existing mechanisms if they fit the proven ownership contract.

Primary proof:

```bash
CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree
CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement-evidence.test.mjs scripts/worktree-retirement-checkout-processes.test.mjs
```

Inspect the process-level cases to confirm they assert completed, failed and
cancelled ownership release at the public launcher/wrapper boundaries, not an
internal cleanup helper. Keep the existing active-owner concurrency and stale
retirement refusals as preserved-behavior proof. Update the two worktree guides
only to state current product behavior after the proof exists.

Safe stop: supported backend commands no longer strand normal ownership, while
crashes and ambiguous ownership still block retirement until deliberately
resolved.

## Current decisions

- Normal owner release belongs to the backend command lifecycle; retirement
  remains a read/check/drop workflow and does not acquire authority from a dead
  PID alone.
- `.sut.local.lock` teardown remains outside this correction because its public
  shutdown-versus-restart contract requires a separate product decision.

## Open concerns and handoff

- **Process completion evidence:** determine how the owner observes cancellation
  across preparation, the preliminary migration, and Gradle's JVM/test workers.
  Shell stand-ins alone do not establish real Gradle descendant behavior. The
  execution plan must select representative process evidence for that boundary.
  Do not release on a supervisor's interrupted wait alone.
- **Safe release protocol:** justify the ownership comparison and removal against
  the existing admission/stale-reclamation protocol. Retain ambiguous records;
  do not quietly turn this into a general stale-lock redesign.
- **Proof and sizing:** revise the old assertions that require lock residue and
  include retirement checks explicitly; the original primary command does not
  run the retirement-evidence suite. Reassess slice sizing once the process
  design is concrete.
- **Adjacent work:** SEED-017 Story 4 concerns shared Development/SUT process
  termination. This correction does not depend on delivering that story, but
  any needed process termination should avoid adding a third competing mechanism.

No unresolved product-scope decision was found. These are implementation and
verification concerns, not evidence that the defect is absent or authorization
to execute. No conflict with Accepted ADRs 0006 or 0007 was identified.

## Learnings

None from execution yet.
