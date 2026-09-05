# Preserve CI observer coverage across lifecycle races

Source: retrospective of [SEED-011 Story 1](../../seeds/SEED-011-efficient-ci-failure-attention.md#story-1) and reviewed commit `9a5f46f0f2`.
Status: planned.

## Outcome and boundaries

For the developer supervising execute-plan in Codex, Cursor, or Claude Code,
the one execution observer does not silently miss a newly pushed run, suppress
a persisted native-host event before it reaches the hook output boundary, or
hang while stopping an observer whose terminal receipt is unavailable.

Representative behavior: observation starts late within one clock second, a
fast failing push is created in that same second, and a native hook process is
interrupted once before returning its event → the observer still reports the
new run, retries the unacknowledged durable event at the next owning boundary,
and later stops its exact worker within a finite local deadline while preserving
unread evidence and reporting any lost coverage.

This is a bounded correction of completed Story 1. Keep one observer, the
selected startup baseline, the existing event identities, and the Codex versus
native-host delivery adapters. Do not add Story 2 repair scheduling, compaction
ownership, cause-equivalence policy, more agent hosts, a general message broker,
or persistent monitoring after execution.

## Retrospective evidence

- `watch-ci-execution.mjs` compares second-precision GitHub `createdAt` values
  with the observer's millisecond `startedAt`. A focused replay starting at
  `12:00:00.900Z` and returning a new completed failure stamped
  `12:00:00.000Z` on the next poll emitted only `CI_MONITOR_UNAVAILABLE` at
  budget expiry; it never inspected or emitted run 42.
- `ci-host-hook.mjs` writes `delivery.json` before it returns host output. A
  focused replay that discarded the first returned result found the persisted
  failure in that result, but the next owning hook boundary returned `{}`.
- `waitForTerminalResult` has neither a fallback observation path nor a finite
  terminal deadline. The execution also observed the analogous file-watch
  race in process tests, and the final aggregate wrapper had two transient
  Codex lifecycle timeouts before a 50/50 pass.
- SEED-011 marks Story 1 completed but still says failure-during-repair and
  compaction demonstrations are prerequisites to adopting the cross-host
  contract. Those observations were explicitly deferred with Story 2 and must
  not make this correction expand into repair policy.

## Outside-in proof and promise ownership

| ID | Final promise | Owner | Observable proof |
| --- | --- | --- | --- |
| R1 | A main push created after startup is observed even when its provider timestamp shares the startup second | 1 | Observer replay begins with an empty startup snapshot, then exposes a completed failing run in the same timestamp second and receives its job failure once. |
| R2 | A native-host event remains deliverable until the hook process successfully writes its output | 2–4 | Shared selection/acknowledgement remains once-only, then Cursor and Claude process fixtures each interrupt one delivery before output, receive the event at the next owning boundary, and prove successful output advances progress. |
| R3 | Shutdown targets only the execution's worker and always reaches a finite honest result | 5–7 | Focused mailbox and real launcher/worker/stop fixtures cover a suppressed file notification, bounded terminal fallback, retained exact-worker identity, missing terminal receipt, unread evidence, and explicit lost coverage without a broad process kill. |
| R4 | Permanent guidance describes the completed one-observer contract without importing Story 2 prerequisites | 8 | SEED-011 and observer references distinguish delivered Story 1 behavior from deferred repair/compaction observations; the repository wrapper passes. |

Use the existing capability entry points and process fixtures. Final aggregate
proof is:

```sh
./scripts/run.sh bash scripts/test/ci_observer.test
```

Do not substitute arbitrary sleeps for lifecycle handshakes. Reuse the recorded
installed-host evidence when output shapes and ownership stay unchanged; rerun
an installed-host probe only if a host-facing JSON shape or binding changes.

## Current decisions

1. Classify a run as startup history or post-start work from identities captured
   in the startup snapshot, not by comparing provider timestamps at finer
   precision than GitHub supplies. Preserve the newest-completed-plus-unfinished
   startup baseline and bounded pagination.
2. Persist event evidence before delivery, but do not advance durable delivery
   progress merely because an in-process function assembled a result. A hook
   process interrupted before its output is successfully written leaves the
   event eligible for the next owning boundary. Normal successful output remains
   once-only for Cursor and Claude Code.
3. Graceful shutdown remains first. Persist the minimum exact worker identity
   needed to distinguish this observer, use a bounded local terminal wait, and
   target only that worker if graceful completion cannot be confirmed. Preserve
   unread records and report pending CI as unobserved; never claim green CI or
   use a broad process-name kill.
## Ordered slices

### 1. Observe a post-start run created in the startup second
Type: Behavior
Status: done
Proof: `watch-ci-execution-startup.test.mjs` replays an empty startup snapshot followed
by a completed failing main push whose `createdAt` shares the observer's startup
second; its failed job is emitted once and older completed startup history stays
excluded.

Behavior: the startup baseline has been captured → a new push run appears with
a timestamp that is not greater than millisecond `startedAt` → run identity,
not timestamp precision, admits it to normal observation and its failure is
reported.

Keep bounded pagination and retained unfinished-run behavior green. Do not
broaden startup replay or change failure classification.

Learning: the startup snapshot's run identities are the reliable boundary;
provider timestamp precision is unnecessary for later admission.

### 2. Separate native selection from durable acknowledgement
Type: Structure
Status: done
Proof: `ci-host-hook.test.mjs` keeps existing Cursor and Claude owner,
generation, sub-agent isolation, output shape, and once-only delivery behavior
green while selection no longer writes delivery progress.

Internal change: represent selected native-host records separately from the
operation that advances their durable progress, without adding a lease or
general broker. External hook output remains unchanged. This immediately
enables slice 3 to acknowledge Cursor delivery at the process output boundary.

Learning: the existing delivery entry point can preserve synchronous callers
by acknowledging an explicit selection immediately; host processes can own the
later output-boundary acknowledgement without changing their JSON payloads.

### 3. Redeliver an interrupted Cursor hook event
Type: Behavior
Status: done
Proof: a real Cursor hook child process is interrupted after selecting a
persisted failure but before writing host output; the next owning boundary
receives it, then successful output advances progress and later boundaries stay
silent.

Behavior: a durable event is ready for its owning Cursor coordinator → the
Cursor hook process attempts delivery but does not write output successfully →
the event remains eligible for the next owning boundary; a completed output is
acknowledged once.

Keep the existing Cursor JSON shape and owner/generation/sub-agent isolation.

Learning: a blocked stdout pipe provides a deterministic process-boundary
handshake for interruption proof; acknowledgement can follow the write callback
without changing the synchronous compatibility entry point.

### 4. Redeliver an interrupted Claude Code hook event
Type: Behavior
Status: planned
Proof: a real Claude Code hook child process is interrupted after selecting a
persisted failure but before writing host output; the next owning boundary
receives it, then successful output advances progress and later boundaries stay
silent.

Behavior: a durable event is ready for its owning Claude Code coordinator → the
Claude hook process attempts delivery but does not write output successfully →
the event remains eligible for the next owning boundary; a completed output is
acknowledged once.

Keep the existing Claude Code JSON shape and owner/sub-agent isolation.

### 5. Observe a terminal result despite a missed file notification
Type: Behavior
Status: planned
Proof: a focused mailbox fixture suppresses the file-watch callback, writes the
terminal record, and still receives that record through a bounded local fallback;
existing normal launcher, Codex stream, native lifecycle, and stop output remain
green.

Behavior: graceful observer shutdown begins and the terminal record is written
without a usable file-watch notification → the bounded terminal wait checks the
authoritative record → shutdown receives the normal terminal result without
hanging.

Use a lifecycle deadline, not an arbitrary proof sleep.

### 6. Retain the detached worker's exact identity
Type: Structure
Status: planned
Proof: focused launcher and mailbox tests show the receipt remains unchanged
while the mailbox retains only the identity required to address its own worker;
normal stop remains green.

Internal change: store and read one cohesive exact-worker identity for the
detached mailbox worker. External launch and normal-stop output remain unchanged.
This immediately enables slice 7 to resolve only that worker after a missing
terminal receipt.

### 7. Finish shutdown honestly when terminal publication fails
Type: Behavior
Status: planned
Proof: a real launcher/worker/stop fixture withholds terminal publication;
shutdown reaches its lifecycle deadline, targets only the retained worker,
terminates within a finite bound, preserves unread events, and returns explicit
lost-coverage and pending-CI evidence without a broad process kill.

Behavior: execution ends and the exact observer does not publish a terminal
receipt after graceful stop → bounded shutdown resolves that worker only → the
coordinator receives an honest finite result with unread evidence and
unobserved CI, and no observer process remains.

### 8. Publish the completed observer lifecycle contract
Type: Behavior
Status: planned
Proof: `ci-monitor.md`, its host adapters, and SEED-011 consistently distinguish
the completed one-observer lifecycle from deferred Story 2 repair/compaction
observations; `./scripts/run.sh bash scripts/test/ci_observer.test` passes.

Behavior: the corrected observer lifecycle is proved → a developer or agent
reads the permanent observer guidance and Story 1 record → they see the finite,
exact-worker, durable-delivery contract without a stale Story 2 prerequisite.

Update only statements whose truth changed through slices 1–7.

## Readiness

The original native-delivery and shutdown slices were refined before execution
into single-proof leaves for shared acknowledgement, each native host, terminal
notification fallback, exact-worker identity, forced shutdown, and permanent
guidance. Each remaining leaf is a target-sized hypothesis; no sizing exception
is currently required.
