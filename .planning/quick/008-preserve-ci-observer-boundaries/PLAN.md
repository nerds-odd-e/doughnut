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
| R2 | A native-host event remains deliverable until the hook process successfully writes its output | 2 | Cursor and Claude process fixtures interrupt one delivery before output, receive the event at the next owning boundary, then prove normal output advances progress and does not redeliver. |
| R3 | Shutdown targets only the execution's worker and always reaches a finite honest result | 3–4 | Real launcher/worker/stop fixture covers graceful stop, suppressed file notification, missing terminal receipt, exact-worker fallback, unread evidence, and explicit lost coverage without a broad process kill. |
| R4 | Permanent guidance describes the completed one-observer contract without importing Story 2 prerequisites | 4 | SEED-011 and observer references distinguish delivered Story 1 behavior from deferred repair/compaction observations; the repository wrapper passes. |

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
Status: planned
Proof: `watch-ci-execution.test.mjs` replays an empty startup snapshot followed
by a completed failing main push whose `createdAt` shares the observer's startup
second; its failed job is emitted once and older completed startup history stays
excluded.

Behavior: the startup baseline has been captured → a new push run appears with
a timestamp that is not greater than millisecond `startedAt` → run identity,
not timestamp precision, admits it to normal observation and its failure is
reported.

Keep bounded pagination and retained unfinished-run behavior green. Do not
broaden startup replay or change failure classification.

### 2. Acknowledge native delivery only after hook output succeeds
Type: Behavior
Status: planned
Proof: real Cursor and Claude hook child processes select the same persisted
failure; one is interrupted before writing output and the next owning boundary
redelivers it, while a successful output write advances progress and subsequent
boundaries stay silent.

Behavior: a durable event is ready for its owning Cursor or Claude coordinator
→ the hook process attempts delivery → progress advances only after successful
host output, so an interrupted attempt cannot suppress the event and a completed
attempt remains once-only.

Keep owner/generation/sub-agent isolation and the existing host JSON shapes.
Separate event selection from progress acknowledgement only as far as this
process boundary requires; do not introduce a general lease or broker.

### 3. Retain exact worker identity and a bounded terminal wait
Type: Structure
Status: planned
Proof: existing normal launcher, Codex stream, native-host lifecycle, and stop
tests remain green; a focused fixture can suppress a file-watch callback and
still observe a subsequently written terminal record within the declared local
deadline.

Internal change: give the mailbox lifecycle one cohesive representation of the
detached worker identity and bounded terminal-result wait, replacing duplicated
watch-only waits where they govern observer shutdown. External launch, event,
and normal stop output stay unchanged. This immediately enables slice 4 to stop
or report the exact worker when graceful terminal publication fails.

### 4. Finish shutdown honestly when graceful termination fails
Type: Behavior
Status: planned
Proof: the real process fixture stops normally when possible; when terminal
publication or its notification is withheld, shutdown targets only the recorded
worker, terminates within the local deadline, preserves unread events, and
returns explicit lost-coverage/pending-CI evidence instead of hanging or
claiming success. The 50-test aggregate wrapper plus new cases passes.

Behavior: execution ends and the exact observer does not publish a terminal
receipt after graceful stop → bounded shutdown resolves that worker only → the
coordinator receives an honest finite result with unread evidence and
unobserved CI, and no observer process remains.

Update `ci-monitor.md`, the host adapters, and SEED-011 only where the proved
behavior changes their current truth. Remove the stale implication that Story 2
repair/compaction demonstrations are prerequisites for completed Story 1.

## Readiness

Refinement recommended for slices 2 and 4: each has one observable outcome, but
process-interruption injection and exact-worker termination may reveal a
multi-beat path or exceed the ten-minute leaf limit. Slices 1 and 3 are ready
for direct execution. Do not execute this plan as part of the retrospective.
