# Restart primary SUT only with proven process ownership

Source: execution retrospective of completed SEED-017 Story 4 and `.planning/quick/103-unify-owned-process-termination/PLAN.md`, preserved at before-cleanup commit `a32e908380`.
Execution provenance: `7c664f3e713bfaa9897ef0c2be3445a63feb3ef0`, `1be32355f5e5f5188707a22da6ec19b301c79617`.
Finding provenance: the primary-checkout port-signalling path predates that execution (`scripts/sut-restart.mjs`, originally `3beee1da81`); the reviewed execution did not introduce it.

Status: planned. Retrospective correction only; no execution authority.

## Outcome and current finding

Developers can restart the primary-checkout SUT without risking unrelated
processes that happen to occupy its canonical ports. A legitimate recorded SUT
still stops completely before its replacement starts.

Current `runSutRestart` behavior is split by checkout type. An isolated checkout
requires a verified live owner and asks that owner to stop. The unconfigured
primary checkout instead uses `stopSutPorts`, discovers listeners on ports
5173/5174/9081, and sends SIGTERM without authenticating them against `sut.pid`.
The existing `terminateTcpListenersOnPort` test positively preserves that unsafe
behavior by starting an arbitrary listener and expecting restart code to kill it.

This is a current whole-product ownership weakness adjacent to, but older than,
the unified already-owned-tree termination mechanism. It conflicts with
[ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md),
which requires stopping when the environment or owner cannot be proven.

## Scope and decisions

- At the public `runSutRestart` boundary, keep the idle primary target behavior:
  when all canonical application ports are free, start SUT without signalling.
- When any canonical SUT port is occupied, require a valid live process group
  recorded in the existing `sut.pid` and prove every discovered application-port
  listener belongs to that tree before signalling anything.
- For a proven tree, terminate through the shared owned-process-tree lifecycle,
  await bounded completion and free ports, then start the replacement.
- Missing, unreadable, invalid or stale `sut.pid`, and foreign or partially
  owned listener sets, fail loudly with no signal and no replacement start.
- Preserve isolated restart behavior, canonical ports, default timing, SUT data,
  ownership records, and private-mock lifecycle. Follow
  [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  propagate termination and liveness failures rather than converting them to
  successful restart.
- Reuse the existing PID file, process-tree evidence, listener discovery and
  termination mechanism. Do not add an owner service, port-claim format,
  database behavior, generic process manager or PID-reuse solution.

## Key examples and proof ownership

| Promise | Observable proof |
| --- | --- |
| Idle primary SUT target starts without signalling | `runSutRestart` with free canonical ports calls the SUT start boundary once and records no signal. |
| A valid recorded primary SUT restarts safely | At `runSutRestart`, all occupied-port listeners are proven inside the recorded process tree; the root and captured descendants disappear, ports are free before the replacement start, and an unrelated peer stays alive. |
| Unproven ownership never authorizes a signal | Missing, invalid and stale `sut.pid`, plus foreign and mixed owned/foreign listeners, reject with zero signalling and zero replacement starts; the listeners remain alive. |
| Bounded shutdown failure stays visible | Persistent post-KILL liveness rejects and the replacement is not started. |
| Isolated restart and plan 103 behavior remain intact | Existing isolated restart, owned-tree escalation, supervisor failure and private-mock lifecycle checks remain green. |

The primary restart tests are unit tests at a stable command boundary with real
lower-level process-tree code and crafted OS data. Retain one representative
real-process peer-safety example; use injected OS boundaries for deterministic
invalid, stale and bounded-failure cases. Replace the existing test that treats
an arbitrary port listener as kill authority; its meaningful TCP survival
signal belongs in the refusal case.

## Ordered slices

### 1. Refuse primary SUT restart without proven tree ownership
Type: Behavior
Status: planned
Proof: Primary restart ownership/refusal examples pass together with isolated restart and the shared SUT termination lifecycle.

Behavior: Given an idle primary target or occupied primary ports with recorded
ownership evidence, when a developer runs `pnpm sut:restart`, then the command
either starts directly or stops only the authenticated SUT tree before starting;
missing, stale, foreign, partial or persistently live ownership evidence fails
without signalling or starting a replacement.

Implement the ownership gate at `runSutRestart`, using the existing `sut.pid`,
listener enumeration and process-tree membership evidence before calling the
shared termination lifecycle. Remove port occupancy as standalone kill
authority and delete the redundant `terminateTcpListenersOnPort` surface if no
real caller remains. Keep the isolated-owner branch unchanged.

Focused command:

```sh
CURSOR_DEV=true nix develop -c node --test scripts/sut-restart.test.mjs scripts/sut-isolated-restart.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/sut-services-child-exit.test.mjs scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs
```

Sizing hypothesis: 5–8 minutes including regression proof, implementation,
focused verification and local cleanup; medium confidence. The Development
restart boundary already demonstrates the required ownership cases, while the
primary SUT path needs one representative real-process fixture. Stop and refine
above 10 minutes unless a measured process wait is the only overrun.

## Cumulative assessment

One Behavior slice owns the complete correction: the ownership decision,
safe termination, restart ordering and refusal proof are not useful as separate
delivery states. The common rule is that a canonical port identifies where to
inspect, never who may be killed. SUT and Development may retain separate
entry adapters, but both must authenticate a recorded process tree before the
shared termination progression runs.

No slice-specific concern remains from this assessment. The principal sizing
uncertainty is adapting the existing primary restart fixture without duplicating
the Development harness.
