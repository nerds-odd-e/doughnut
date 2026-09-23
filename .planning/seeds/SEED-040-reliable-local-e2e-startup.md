---
id: SEED-040
status: dormant
planted: 2026-09-23
planted_during: owner-requested retrospective finding triage and backlog prioritization
trigger_when: selecting the highest-priority unresolved project retrospective finding
scope: small
---

# SEED-040: Reliable local E2E startup

## Why This Matters

Donut contributors need ordinary local E2E runs to start reliably in a linked
checkout that already has backend build output. [DD-103](../../DonutRetrospectiveFindings.md#dd-103)
records three failed launches in one execution and about 25 minutes lost to
competing Gradle compilation, followed by a manual clean/rebuild workaround.
The existing CI-target correction does not cover ordinary local runs.

## Alternatives and Decision

Queue the remaining local-startup outcome. Repeated manual cleaning and
pre-compilation can unblock one run but retain the recurring intervention and
race. Requiring the built CI target for every local run changes the existing
development workflow and does not establish that its ordinary command works.
Reuse the existing runner/service lifecycle; the compilation ownership and
sequencing solution remains for refinement. No implementation is authorized
by this backlog entry.

## Story Decomposition

<a id="story-1"></a>

### Start local E2E reliably with an existing backend build

**Identity:** SEED-040#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Contributors can obtain local E2E feedback without deleting
  compiled classes, pre-building manually, or retrying a failed stack launch.
- **Outcome and scope:** The ordinary local E2E command starts and runs its
  selected feature from a linked checkout with existing backend build output,
  including when a source change requires recompilation. Preserve clean-build
  startup, built CI runs, interactive source reload, isolated service ownership,
  failure reporting, and cleanup. Optimize neither test bodies nor CI shards.
- **Evaluation:** A representative ordinary invocation with populated output
  and a pending source recompile reaches backend readiness and completes its
  selected E2E proof without competing writes or missing application classes.
  A subsequent invocation works without manual cleanup. Verify the retained
  built-target and interactive workflows at their existing entry points.
  Refinement must establish deterministic regression proof for the race;
  a single lucky startup does not establish the correction.
- **Value / learning:** Remove a demonstrated obstacle to product validation
  while determining how the existing compilation lifecycle can serve each
  supported launch mode coherently.
- **Effort hypothesis:** S (30–60 minutes), low confidence until reproduction
  and reload requirements are checked; resplit if they reveal independent work.
- **Depends on:** None. CI caching and test-speed work are not prerequisites.
- **Safe stopping point:** Reliable local startup remains useful without any
  subsequent suite-speed or shard changes.

## Ordering and Scope Reduction

One selected story, retained first after a second ownership and impact review.
The affected ordinary command supplies E2E proof across the queued product
work; three consecutive failed launches and about 25 minutes of recovery
justify a bounded reliability repair before the longer feature work. This
is contributor impact, not a production outage. Its evidence is one execution
with three retries, not three separate recurrences or proof of widespread
failure. No second independent unresolved project finding was established.
If reproduction is inconclusive or the correction exceeds a small story,
reassess its position against note-save latency and attachment outcomes before
expanding it. Keep existing unrelated queue entries in their previous order.

## Open Decisions

Reproduction conditions and the smallest compilation-ownership correction need
refinement. Do not choose a new watcher, service framework, or blanket removal
of interactive reload from the historical workaround alone.

## Breadcrumbs

- [Project finding and retained occurrence](../../DonutRetrospectiveFindings.md#dd-103).
- `8b9fc7d30a`: earlier fix for the built CI target only.
- `b0b385a184`: retained DD-103 evidence before `a2660d71f6` removed it during
  unrelated story closure; no local-startup fix accompanied that removal.
- Checked against `9c4c712f70`; no matching queued or taken story exists.
