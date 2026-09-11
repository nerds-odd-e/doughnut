# Keep one owned-process termination mechanism

Source: [SEED-017, Story 4](../../seeds/SEED-017-cohesive-design-corrections.md#story-4), audit finding F4 in the same seed.
Provenance: SUT descendant shutdown in 072 (`392599bf16`, `d191a6b33d`), Development in 095 (`149809803a`); original audit at `6876f46de098cf6b41dfcae7b2a2a0bebb7c16ec`.

Status: complete. Both slices delivered and focused proof passed.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`; Taken claim commit `61e0c8915b6241d80e44246677347900f9bbbf09`.
- Execution checkout: `/Users/terryyin/git/doughnut-103-unify-owned-process-termination` on `codex/103-unify-owned-process-termination`.
- Integration target: `main`; push the execution branch to `origin`.
- CI observation: unavailable for this branch because `.github/workflows/ci.yml` (`donut CI`) triggers pushes to `main` only; do not claim branch CI coverage.

## Outcome and current evidence

Maintainers change the already-owned-process termination progression once;
developers retain independent, safe Development and SUT shutdown/restart.
This is the evidenced structural correction F4, not a new behavior promise or
a claim of a reproduced process leak.

Current inspection confirms `scripts/sut-owned-process-tree.mjs` and
`scripts/development-owned-process-tree.mjs` each implement descendant capture,
TERM, polling, KILL and bounded failure. Both default to 5,000 ms for TERM,
1,000 ms after KILL and a 20 ms polling interval. Both ignore ESRCH/EPERM on
signalling, but their liveness contracts differ: SUT treats EPERM as alive and
also checks ChildProcess exit/signal state; Development's `isProcessAlive`
propagates non-ESRCH errors. Preserve these adapter semantics.

`runDevRestart` authenticates recorded ownership, awaits termination and free
ports, then starts Development. SUT start cleanup awaits termination before
ownership release; `runSutServices` observes asynchronous cleanup failure via
`runSupervisedServiceGroup`. `startOwnedSutSupervisor` retains one shared stop
promise. Private OpenAI mock shutdown also calls the SUT adapter. Preserve all
of these existing compositions.

## Scope and decisions

- One mechanism owns descendant capture before signalling, ordered signalling,
  polling, escalation and bounded failure. Adapters supply the existing root
  signal/liveness operations and diagnostic context. Keep the API as small as
  these two callers require; do not dispatch by environment inside the loop.
- Ownership verification stays outside the mechanism. Preserve invalid-input
  handling, group and captured-descendant checks, default timing, diagnostics,
  and callers' awaited cleanup. Never discover kill targets by occupied port.
- Follow [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md): fail when ownership cannot be proven, preserve environment/worktree boundaries, and leave persistent Development data intact.
- Follow [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md): propagate or meaningfully report cleanup failure. Do not turn unsuccessful shutdown into success.
- No new process-management framework, environment admission, ownership record,
  database/port behavior, PID-reuse solution or timeout policy. The separate
  backend-test ownership correction in 102 is not a prerequisite and remains
  independently owned. Recheck concurrent edits before execution.

## Proof ownership

| Final promise | Owner | Observable proof |
| --- | --- | --- |
| One termination progression serves both environments | 2 (1 establishes shared implementation) | Aggregate source review: only the common mechanism owns TERM/wait/KILL/wait; adapters retain only genuine entry/liveness distinctions. |
| SUT cleanup removes owned descendants and leaves unrelated peers alive | 1 | Existing `sut-isolated-start-release.test.mjs` timeout/cancellation/early-exit process scenarios through `runSutStart`; retain foreign process/listener assertions. |
| TERM-resistant and disappeared processes preserve SUT semantics | 1 | Extend the same lifecycle fixture with TERM-resistant descendant evidence and an already-exited root; observe final owned-tree disappearance and peer survival. Add focused OS-boundary error data only where current proof is missing. |
| SUT owner observes bounded cleanup failure | 1 | Through `runSutServices` in a disposable supervisor fixture, model persistent OS liveness; after the configured TERM/KILL bound, observe cleanup diagnostic, nonzero supervisor exit, and retained ownership. An adapter rejection alone is insufficient. |
| Private mock and SUT restart still use the existing adapter contract | 1, rerun in 2 | Existing private-mock cancel/failure and isolated restart boundary tests. |
| Development refuses unproven ownership before signalling | 2 | Existing `dev-restart.test.mjs` missing/stale/foreign/partial ownership and linked-worktree refusals retain zero-signalling observations. |
| Development completes shutdown before restarting | 2 | `runDevRestart` examples in `dev-restart-owned.test.mjs`: normal exit and TERM-resistant tree, final disappearance before replacement start, tolerated missing process. |
| Development surfaces bounded failure and preserves liveness errors | 2 | OS-boundary crafted data through `runDevRestart`: persistent liveness after KILL rejects without calling start; permission errors preserve current propagation. |
| Data, ports and ownership protocols remain separate | 2 | Aggregate diff review plus existing ownership refusals and peer-safety checks; no migrations or resource-format changes. |

Use stable lifecycle entry points with real lower-level production code. Fake
only OS/external boundaries for deterministic failure cases; do not create a
test class per helper or export internals for testing. Add missing regression
proof before modifying the corresponding production path, in the same slice.
Current evidence is source/test inspection, not a test run in this planning task.

## Ordered slices

### 1. Centralize the spawned owned-tree termination lifecycle
Type: Structure
Status: done
Proof: SUT lifecycle boundary examples in the table pass before and after extraction; aggregate diff preserves SUT adapter semantics.

Delivered proof (2026-09-11):

```text
CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start-release.test.mjs scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs
14 tests passed. This covers the shared SUT progression, timeout/cancellation/already-exited-root cleanup, TERM-resistant descendant escalation and foreign-peer survival, caller-observed bounded cleanup failure with nonzero supervisor exit and retained ownership, restart, and private-mock composition.
```

Own the first part of F4 directly: move the complete existing SUT progression
to a capability-named shared module and have the SUT adapter delegate. Preserve
ChildProcess root operations, process/group liveness and diagnostic behavior.
Include missing regression observations for escalation and caller-owned failure;
reuse the existing disposable process fixtures. Keep Development's working
implementation until slice 2 replaces it. No unused framework or behavior change.

Refined proof approach: extend the disposable supervisor script already used in
`sut-services-child-exit.test.mjs`. Drive `runSutServices` with a crafted spawned
ChildProcess and a process-kill OS stand-in confined to that subprocess; let
the real adapter, termination progression and supervisor failure handler run.
Model a root that stays live, then observe the supervisor's actual nonzero
exit and cleanup log after the existing 5 s + 1 s bounds. Retained ownership
must remain observable. Do not inject a rejected `stopOwnedTree` promise as a
substitute for exercising termination. For TERM resistance, extend the existing
owned-tree stand-in with a ready signal after its TERM handler is installed,
so assertions cannot race process initialization. Keep fixture additions local
to these scenarios; no general process simulator is needed.

Safe stop: every SUT caller uses the extracted mechanism; Development remains
functional. F4 is explicitly unfinished until slice 2 removes its duplicate.

Focused command (including additions to these existing files):

```sh
CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start-release.test.mjs scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs
```

Sizing hypothesis: 5–8 minutes including extraction, focused verification and
local cleanup; medium confidence. Above-target scrutiny found one cohesive
correction proof loop, with existing fixtures for both the supervisor and owned
tree. Keep the real six-second failure bound; it does not justify a time-budget
exception. If the fixture needs a new harness or the path approaches 10 minutes,
stop and refine before expanding implementation.

### 2. Terminate recorded owned trees through the same mechanism
Type: Structure
Status: done
Proof: Development restart/refusal examples in the table pass, SUT checks remain green, and source review confirms F4's duplicate progression is gone.

Delivered proof (2026-09-11):

```text
CURSOR_DEV=true nix develop -c node --test scripts/dev-restart.test.mjs scripts/dev-restart-owned.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs
26 tests passed. This covers Development normal shutdown, TERM-resistant escalation before replacement start, disappeared processes, bounded post-KILL failure without restart, permission-error propagation, ownership refusals, free-port/restart ordering, and the retained SUT/private-mock lifecycle proof.
```

Complete F4: adapt the recorded PID/group entry to the shared mechanism and
remove Development's duplicate signal/wait/escalation functions. Keep
Development's liveness/error semantics, injectable OS boundaries, ownership
authentication, free-port wait and restart ordering. Add the missing regression
cases at `runDevRestart` before changing that path. Review implicated callers,
including private mock cleanup, for bypasses or unobserved promises.

Safe stop: both adapters and all existing callers remain functional and use one
termination lifecycle; no temporary second implementation remains.

Focused command:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/dev-restart.test.mjs scripts/dev-restart-owned.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs
```

Sizing hypothesis: about 5 minutes, medium confidence; the existing restart
fixture exposes kill, liveness, descendants and timeout controls. One adapter
conversion and its preservation proof form one correction outcome.

## Cumulative assessment and execution gates

The shared concept is termination of an already-proven owned tree. The two
adapters differ by root handle/liveness, not by a second lifecycle policy. Slice
1 must leave a concrete used mechanism; slice 2 consumes it and removes the
duplicate. No new Behavior slice is needed for this evidenced structural debt.

Plan-refinement assessment: both slices are Ready; two slices remain, none were
replaced. Slice 1 was refined in place to name the existing supervisor fixture,
OS seam, real failure bound and TERM-handler readiness handshake. Slice 2 already
has the required OS seams in the restart fixture. The common model is supported;
no unresolved requirement, ADR conflict or scope decision was found. No uncertain
new infrastructure capability is proposed:
existing process-level fixtures already exercise owned descendants and peer
safety. Their literal verification command is above; execution must record
results and critical observations, not infer a pass from this inspection.

Target approximately 5 minutes per slice including focused checks and local
cleanup; scrutinize overruns, and stop for finer decomposition above 10 minutes
unless a measured focused-test/external wait is the stated exception. No sizing
exception is asserted now. Do not split proof from its structural correction.

Ready for direct execution when separately authorized. No further refinement
or story resplit is indicated by this assessment. This is a sizing hypothesis,
not measured implementation time; apply the stop/refinement rule if disproved.

Final design account: `owned-process-tree-termination.mjs` owns the single
descendant-capture, TERM/wait, KILL/wait and bounded-failure progression. The
former parallel loops in the SUT and Development adapters are gone; each adapter
retains only its genuine root signalling, liveness and diagnostic semantics.
Ownership proof remains an invariant enforced before either adapter is called,
so unrelated processes and environment resources remain outside the mechanism.
The focused SUT lifecycle and Development restart/refusal boundaries above prove
the adapters compose without bypassing awaited cleanup or failure propagation.

On authorized execution, move only this story to Taken and follow
`dough-execute-plan`: Jidoka, fresh `dough-post-change-refactor` agent, API
generation if actually implicated, one coordinator formatting pass via
`./scripts/run.sh pnpm format:changed`, plan update, commit, push and asynchronous
CI handling. Retain plan/seed evidence through retrospective and story wrap-up.

## Learnings

- Slice 1 reused the existing SUT process fixtures. A readiness handshake was needed so the TERM-resistant descendant installs its handler before the test can trigger cleanup; no general process simulator was needed.
- The disposable supervisor can deterministically exercise the real 5,000 ms TERM plus 1,000 ms KILL failure bound by replacing only the subprocess's OS signal boundary. The production supervisor logs cleanup failure, exits nonzero, and retains ownership as required by ADR 0006 and ADR 0007.
- The execution worktree initially lacked ignored dependencies; the unchanged focused command passed after a temporary dependency link was supplied and removed. No production or proof boundary changed.
- Slice 2 needed no new harness: existing `runDevRestart` seams expressed escalation, disappearance, persistent liveness and permission failure directly. Aggregate review confirmed no owned-tree termination progression remains duplicated in the two environment adapters.
