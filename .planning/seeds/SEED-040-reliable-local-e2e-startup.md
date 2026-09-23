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
Owner agreement during refinement (2026-09-23): batch E2E compiles source
present when launched, without a continuous backend compiler. Interactive E2E
retains source reload. Reuse the existing runner/service lifecycle and the
existing bootRun-only launch capability; local batch runs retain Vite.
No implementation is authorized by this backlog entry.

## Story Decomposition

<a id="story-1"></a>

### Start local E2E reliably with an existing backend build

**Identity:** SEED-040#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/016-reliable-local-e2e-startup/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"a0e7fbde8bee3eb7a09a6067cfd1b77d4ee5fb90a108987a8f8b2586f3385959","plan":"9b6cb6cdc4fef7fd5ba04fac31d989fe23e2714fdc43f2c73365e7c8fff3e8d8"}}
```

- **Goal:** Contributors get trustworthy E2E feedback on current backend source
  from an ordinary local batch invocation, without deleting compiled classes,
  manually prebuilding, or retrying startup. This supports parallel work in
  local Git checkouts without interrupting validation of upcoming product work.
- **Scope:** `pnpm cy:run --spec <feature>` from a linked checkout with existing
  backend output compiles pending source changes and runs the selected feature.
  Batch launch does not also start the continuous backend compiler; each later
  invocation uses the then-current source. Keep the local Vite frontend.
  Preserve clean-checkout startup, built CI behavior, `pnpm cy:open` source
  reload, persistent Development behavior, isolated ownership, visible failure,
  and cleanup through the existing lifecycle owners.
- **Key examples:**
  1. Existing output and a changed backend response → ordinary batch run → the
     selected feature observes the changed response and succeeds.
  2. Another source edit after that run → the same command, without cleaning
     or prebuilding → the next response is observed and the feature succeeds.
  3. Invalid source with previously valid compiled output → batch launch →
     compilation failure is visible, Cypress never tests the stale application,
     and owned resources are cleaned up.
  4. No backend output → ordinary batch run → compilation and selected feature
     succeed; an interactive session still observes edits without restarting it.
- **Proof:** A repeatable regression test checks the actual batch launch path
  cannot also select a compiler watcher, with interactive and built-target
  preservation checks. A real Gradle/Spring/Cypress run proves recompilation
  and current-source behavior. One successful startup alone is insufficient;
  repeated probabilistic attempts to provoke a Gradle crash are not required.
- **Deferred promises:** Suite-speed optimization, CI caching/sharding, a new
  watcher or service framework, arbitrary corrupted-build recovery, continuous
  reload during batch runs, and simultaneous independent build commands in the
  same checkout. This does not promise to eliminate all startup failures or to
  repair the interactive startup race. These exclusions are delivery boundaries,
  not new product rejection rules.
- **Effort hypothesis:** S (30–60 minutes), medium confidence in the bounded
  launch-mode correction; real stack verification dominates elapsed time.
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
The remaining note-save story improves an owner-reported roughly one-second
save after the earlier latency repair, with uncertain implementation cost.
Attachment limits, LFS, and browsing more directly advance the near-future
product direction. Keep this repair ahead of them only while it stays small
and demonstrable; testing's general usefulness is not unlimited priority.
If launch-path evidence or real verification is inconclusive, or the correction
exceeds a small story, reassess its position against note-save latency and
attachment outcomes before expanding it. Keep existing unrelated queue entries
in their previous order.

## Open Decisions

None for story scope or proof. The owner accepted batch-versus-interactive
semantics and the complementary regression/current-source proof. Technical
verification remains execution work; planning does not claim a reproduced or
fixed runtime failure.

## Breadcrumbs

- [Project finding and retained occurrence](../../DonutRetrospectiveFindings.md#dd-103).
- `8b9fc7d30a`: earlier fix for the built CI target only.
- `b0b385a184`: retained DD-103 evidence before `a2660d71f6` removed it during
  unrelated story closure; no local-startup fix accompanied that removal.
- Checked against `9c4c712f70`; no matching queued or taken story exists.
