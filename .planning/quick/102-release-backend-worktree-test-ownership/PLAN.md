# Release backend worktree test ownership after execution

Source: execution retrospective for
[SEED-017 Story 2b](../../seeds/SEED-017-cohesive-design-corrections.md#story-2b)
and quick plan 101, reviewed through commits `8983d9db3f`, `65d88f55cc`,
`be76f0d471`, and `1833e29ead`.

Status: planned. Retrospective correction planning authorized 2026-09-11;
implementation not requested.

## Finding and bounded outcome

Developers and AI tasks using an isolated linked worktree can run a supported
backend test or migration command and then retire that worktree without manually
removing `.worktree.local.lock` after the command has exited.

Quick plan 101 exposed the current failure after its successful backend suites:
`pnpm worktree:retire --check` refused a dead PID 23796 recorded in
`.worktree.local.lock`. `scripts/backend-test-worktree-owner.sh` writes the
invocation PID, both supported launch paths hand off with `exec`, and no normal
completion path removes the owned lock. The next backend invocation can reclaim
that stale record, but retirement deliberately and correctly refuses to do so.
This weakness predates quick plan 101; it is not a regression in note publication.

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
- Release the lock after supported opt-in and ordinary Gradle-wrapper backend
  test/migration commands finish successfully, fail, or are cancelled, without
  deleting a successor's or peer's ownership.
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

## Key examples and proof ownership

| Promise | Observable proof |
| --- | --- |
| A completed supported backend command leaves no owned checkout lock | Process-level launcher/wrapper tests observe child exit, preserved exit code, and absent `.worktree.local.lock` |
| Failure or cancellation also releases only after the owned child is finished | Process-level failure/signal cases observe the child outcome and absent lock |
| An overlapping command remains refused while the owner is live | Existing held-owner launcher/wrapper tests remain green |
| Crash evidence and retirement safety remain fail closed | Existing stale/malformed-owner and retirement-evidence tests still refuse without reclaiming |
| Both opt-in and ordinary wrapper routes share the lifecycle | One test for each public route reaches the same owner release behavior without a parallel cleanup rule |

## Ordered slices

### 1. Complete the backend worktree owner lifecycle
Type: Behavior
Status: planned
Sizing: approximately 5 minutes active implementation, proof and cleanup;
medium confidence. Before exceeding 10 active minutes, stop and refine this plan.
Test-process waits are excluded from active time.

Behavior: given a linked or configured worktree whose supported backend test or
migration command owns `.worktree.local.lock` → the owned child completes,
fails, or is cancelled → the command returns the child's outcome and removes
only its own ownership record, so a subsequent retirement check is not blocked
by normal-completion residue; live, malformed, crashed, successor and peer
ownership remain protected.

Implement one owner-aware release seam used by both
`backend-test-worktree.sh` and `backend-worktree-gradle-route.sh` through their
existing handoff. Preserve the process boundary needed for signal propagation
and the child exit code. An ownership comparison must precede removal so late
cleanup cannot delete a successor's lock. Do not weaken
`worktree-retirement-evidence.mjs` or its refusal tests.

Primary proof:

```bash
CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree
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

## Planning assessment

One Behavior slice owns one lifecycle outcome and one process-level proof loop.
The opt-in and ordinary routes are variations of the same owner contract, not
separate slices. No remaining slice-specific concern was identified; signal
propagation is the main implementation risk and is explicitly proof-owned.

## Learnings

None from execution yet.
