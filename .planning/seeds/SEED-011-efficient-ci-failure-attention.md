---
id: SEED-011
status: active
planted: 2026-09-05
planted_during: retrospective of SEED-009 Story 2 publication execution, on developer request
trigger_when: before another long execute-plan run or when revising CI observation and repair coordination
scope: medium
---

# SEED-011: Detect CI failures without repeated AI coordination

## Problem and evidence

The developer needs prompt CI failure attention without repeated AI polling or
lost work. The notebook-publication execution launched 14 observers for 13
revisions, spending 5,716 output tokens on launch responses alone. A failed job
reached the coordinator about 14 minutes late. This demonstrated setup overhead
and delayed delivery; it did not establish recurring lost failures or overlapping
repairs. Detection, delivery, and repair start are separate outcomes.

## Stories

<a id="story-1"></a>

### 1. Notice relevant CI failures throughout execution with one setup

**Status:** completed by quick 007 on 2026-09-05; lifecycle corrections are
tracked in quick 008.

**Goal:** Let the developer's Codex, Cursor, or Claude Code coordinator receive
relevant CI failures across successive pushes with one observer per execution.

**Scope:** Observe main-branch donut CI asynchronously, starting with the newest
completed run and all unfinished runs, then discovering later pushes. Record
failures incrementally with run/attempt/job identity and report lost coverage.
Polling requires no model turns. Stop observation when execution ends, preserving
recorded evidence and handing off unresolved failures and pending CI without
waiting for GitHub. CD and persistent monitoring are excluded.

Host delivery was demonstrated on Codex, Cursor, and Claude Code. Durable repair
ownership and resumption after context resets belong to Story 2; they are not
prerequisites for Story 1's completion or its lifecycle corrections. Current
observer behavior and recovery instructions live in
[ci-monitor.md](../../.agents/skills/execute-plan/references/ci-monitor.md) and
its linked host adapters.

<a id="story-2"></a>

### 2. Finish ongoing work and resolve accumulated CI failures one repair at a time

**Status:** valid, narrowed, and deferred by developer discussion on 2026-09-06.
Medium importance; low immediate urgency.

The narrow lost-handle observer-shutdown subset is now selected in
[SEED-012 Story 6](SEED-012-priority-execution-process-improvements.md#story-6)
and quick 015. Its bounded scope does not select the remaining repair work here.

**Goal:** Keep every reported failure accounted for until it is resolved or
explicitly handed over, preserving ongoing work across repair and context resets.

**Scope:** Make pending failures, the active repair, and work-restoration state
explicit and recoverable, reusing existing mailbox evidence and recovery notes
where sufficient. Current instructions already require safe writer handoffs,
queued failures, one repair at a time, exact stash recovery, and evidence before
dismissing duplicates. This story establishes reliable execution of those rules
across handoffs and context resets. It excludes a general repair scheduler,
forced interruption, other-session repair-policy changes, and additional hosts.

**Key examples:**

- A failure arrives during bounded implementation or refactoring → the writer
  reaches a safe handoff → pending failures receive attention before new work
  starts. Never stash while a writer or its write-capable command is active.
- Failure B arrives during repair A, then context resets → the coordinator
  recovers A's repair and restoration state and retains B for later triage,
  without another repair or nested stash cycle. Shutdown hands over unresolved
  attention and preserved work.
- A repair finishes → queued failures are checked against current code and
  focused proof → distinct causes receive attention; repetitions are dismissed
  only with evidence of the same repaired cause, not a shared job name or newer
  passing run.

**Open decisions:** Finishing the current bounded task before repair is the
recommended initial boundary; delayed attendance is accepted, but that exact
boundary is not yet an adopted cross-host rule. Select the smallest recovery
record and validate implementation/refactor handoffs, failure during repair,
and context-reset resumption when this story is taken up. These observations
validate Story 2 only; no optimal cross-model interruption policy is established.

**Effort hypothesis:** M (about 1–2 hours), medium confidence, assuming bounded
handoffs and reusable recovery state; reassess before planning.

## Priority and revisit trigger

Finish the demonstrated Story 1 lifecycle defects in quick 008, then continue
product work. Defer Story 2 until before relying on long unattended execution,
or revisit sooner if an execution loses track of a failure, repair, or preserved
work. Story 2 depends on reliable delivery, but does not reopen Story 1's scope.

## References

- Publication execution through `dd1ca6415a`; Story 1 delivery: `9a5f46f0f2`.
- [Quick 008](../quick/008-preserve-ci-observer-boundaries/PLAN.md) — active
  observer lifecycle corrections, separate from Story 2.
- [SEED-010](SEED-010-execute-plan-wrap-up-token-efficiency.md) — broader
  execution-efficiency findings; their requirements stay there.
