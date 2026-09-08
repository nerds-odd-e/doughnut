# Owned SUT descendant shutdown

Source: [SEED-015 story 2b](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2b),
correcting the supporting ownership change delivered during quick/070.
Status: planned; story and slice refinement complete, not executed.

## Goal and scope

Developers can stop their isolated SUT without leaving its owned forked backend
running, so restart can reuse the allocation while a peer remains usable.
Correct the existing verified-owner shutdown used by `pnpm sut:restart` in
local Nix worktrees. The parent and its forked backend are live and their
ancestry is observable when shutdown begins. Retain that ownership through
parent exit and bounded escalation. Preserve private mock cleanup callers,
same-group shutdown, and current restart refusals.

Exclude database retirement, new registries or control protocols, listener-based
adoption, already-orphaned child recovery, supervisor hard-kill recovery,
processes newly forked during shutdown, PID-reuse hardening, continuous ancestry
tracking, CLI/MCP expansion, and Cloud VM/CI changes. No new command or UI.
Open questions: none for this boundary.

## Evidence and current decisions

- Original quick/070 contract: `6e17a7e58c^:.planning/quick/070-isolated-openai-browser-mocks/PLAN.md`.
- Execution patches: `6e17a7e58c` endpoint context; `005898f8d4` Gradle ancestry;
  `724d0d71e0` private mock; `bef7001402` foreign-listener refusal;
  `c60532bf33` background failure; `39537a92b6` cancellation;
  `47c4b24eba` paired acceptance/completion. `cfa7bac567` is backlog provenance.
  These are retrospective provenance, not instructions to review unrelated work.
- `scripts/sut-owned-process-tree.mjs:69` signals the parent/group before
  `descendantPidsByParentWalk`. A child in another group can reparent before
  that snapshot; later checks then report completion without observing it.
- Focused reproduction on 2026-09-08 used `CURSOR_DEV=true nix develop -c node
  --input-type=module`: spawn a detached Node parent that spawns a detached
  child; confirm the child in `descendantPidsByParentWalk`; call
  `stopOwnedSutProcessTree(parent, { timeoutMs: 300 })`; probe the child with
  signal 0. Output: `Owned descendant before stop: true`,
  `Descendant survives completed stop: true`. Finally killed both owned fixtures.
- Preserve verified ownership before signals can erase ancestry, and retain
  that ownership across waits/escalation. Do not discover targets by ports.
  Follow [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md):
  bounded cleanup must not report success while known owned children survive.
- `runSutServices` also invokes cleanup from its child-close callback and then
  exits the supervisor. The control callback and close callback must observe
  one completed shutdown before supervisor exit; a second ancestry snapshot
  after parent exit cannot replace the original evidence. Prefer a small
  in-memory coordination change for this owned child, not a lifecycle framework.
- Keep current termination deadlines (5 seconds graceful, 1 second after
  escalation) unless a focused test supplies a reason to revisit them. Inspect
  the final wait result; unsuccessful cleanup must fail loudly through existing
  error handling, without adding recovery or claiming allocation reuse succeeded.
- Existing owner-control HTTP acknowledgement accepts a shutdown request; it
  is not proof of descendant exit. Tests must observe the processes and port
  before restart's start callback, not only the acknowledgement or owner death.

## Outside-in proof and ordered slices

### 1. Stop forked children even when the parent exits first
Type: Behavior
Status: planned
Proof: Extend the existing real-supervisor restart case in
`scripts/sut-isolated-restart.test.mjs`, using the owned-supervisor fixtures.
Have the owned parent spawn a detached backend listener and wait for explicit
readiness before restart. Verify its separate group as a fixture precondition.
At restart's existing start callback, observe that the captured parent/backend
PIDs have exited and bind the old backend port successfully. Verify a separate
peer endpoint still responds. Keep fixture cleanup responsible for every PID
even when the assertion fails.

Behavior: Owned SUT with a separately grouped backend → ordinary owned shutdown
→ parent and backend exit without touching peer processes.

Capture the owned descendant set before termination can erase parent links;
use it for graceful signals and completion checks. Coordinate the supervisor's
close callback with the active shutdown so it cannot exit early. Keep fixture,
implementation, and this one observable proof in the same green commit.

Likely files: `scripts/sut-owned-process-tree.mjs`, `scripts/sut-services.mjs`,
`scripts/sut-owned-supervisor-fixtures.mjs`, and the restart test above.
No preceding Structure slice: fixture setup serves this immediate behavior.
Sizing: about 5 minutes, medium confidence; one fixture variant and one shutdown
path. Scrutinize at 5 minutes; stop and refine at 10 unless focused test runtime
alone explains the duration. A new ownership protocol requires story review.

Focused command: `CURSOR_DEV=true nix develop -c node --test
scripts/sut-isolated-restart.test.mjs`. This also retains same-group and
busy/stale-owner refusal evidence. Port rebinding is test evidence only;
production shutdown must never find targets by port.

### 2. Complete bounded shutdown when a forked child ignores termination
Type: Behavior
Status: planned
Proof: A second case at the same restart boundary uses the same fixture with
a backend that acknowledges SIGTERM but keeps listening; its parent exits
promptly. Observe that restart's start callback occurs only after the captured
backend exits and its port is reusable. Assert the termination acknowledgement
so a fixture that exits gracefully cannot give a false green result. Keep the
peer responding throughout the check.

Behavior: Owned forked child survives graceful termination after parent exit →
shutdown timeout → bounded escalation removes that child before success.

Retain the same captured ownership through SIGKILL and the final bounded wait;
surface unsuccessful cleanup rather than discarding the wait result. Use
existing loud failure handling (ADR 0006); do not build an artificial unkillable
process harness or a new recovery policy. The supervisor must await this same
completion before exiting. Leaf 1's cooperative case remains usable if work
stops before this exception case is delivered.

Sizing: about 5 minutes, medium confidence; reuse leaf 1's fixture and completion
coordination, extending the same path to escalation. The existing graceful wait
adds seconds, not a blanket sizing exception. Apply the same 5/10-minute gate.
Run the restart test above, then the shared-caller regressions once:
`CURSOR_DEV=true nix develop -c node --test
scripts/isolated-cypress-openai-mock-cancel.test.mjs
scripts/isolated-cypress-openai-mock-failure.test.mjs
scripts/sut-services-child-exit.test.mjs
scripts/sut-isolated-start-release.test.mjs`.

These regressions preserve existing cancellation, timely background-failure
observation, and startup/child-exit cleanup. Ownership is not being transferred
to a new lifecycle owner. Change shared callers only when this correction
requires it; do not expand mock behavior.

## Promise ownership

| Contract promise | Owner and observable proof |
| --- | --- |
| Graceful forked-backend shutdown despite parent exit | Leaf 1: real supervisor restart, captured PIDs gone before start callback |
| Allocation can be reused after successful cleanup | Leaf 1: bind the released backend port at the start callback; retain existing allocation assertions |
| Peer is untouched; no port-based targeting | Leaf 1: peer response and existing no-listener-discovery guard; retained in leaf 2 |
| Child ignoring termination is removed before completion | Leaf 2: TERM acknowledgement, child exit and port reuse before restart starts |
| Known surviving child cannot count as successful cleanup | Leaf 2: escalation case proves wait ordering; review final wait result propagation under ADR 0006's loud-failure rule |
| Same-group cleanup and busy/unverified restart refusal stay intact | Leaf 1: existing isolated restart cases |
| Shared mock/startup/child-exit cleanup stays intact | Leaf 2: focused shared-caller regressions above |

## Refinement result

Both original leaves were refined in place; neither was split by technical
layer. Leaf 1 now uses the existing real restart boundary rather than the
mock-only `sut-services.test.mjs`, and includes the concurrent close callback.
Leaf 2 reuses that fixture and coordination for escalation. Each leaf has one
outside-in proof loop; regression checks are preservation evidence. Both are
Ready, with no sizing exception currently needed. These are execution hypotheses,
not elapsed-time guarantees.

Ready for direct execution via execute-plan. No product tests were run during
planning. Implementation, commit, and push have not been performed.
