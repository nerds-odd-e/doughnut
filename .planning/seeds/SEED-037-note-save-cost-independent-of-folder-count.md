---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: story 2 is selected; ancestor reads still follow nesting depth
scope: medium
---

# SEED-037: Save a note at a cost independent of the notebook's folder count

## Why This Matters

A web save publishes by editing the accepted Git tree along the paths the
change affects. It reads those ancestor directories, reuses untouched subtree
object IDs, and does not append a commit when the root tree is unchanged.
The contract is in
[notebook Git synchronization](../../docs/notebook-git-synchronization.md#domain-operation-ownership).

Those ancestor reads, existence checks, and object inserts still follow
nesting depth. A note at depth 12 still pays for that chain. Production timing
on the reported notebook was not observed. The pre-change investigation and
the local measurement are in commit `458496764f931f05b0d46d955e1bbbebf04beefc`,
at `.planning/seeds/SEED-037-note-save-cost-independent-of-folder-count.md` and
`.planning/quick/012-path-scoped-note-save/PLAN.md`.

## Alternatives and Decision

Ancestor-only publication is current behavior. Story 2 still has to choose
whether fewer database round trips come from simpler use of the existing store
or from a new index. That choice is not selected. The decision record and
measurements are in commit `458496764f931f05b0d46d955e1bbbebf04beefc`.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. Story 2 is not authorized for implementation by this seed.

<a id="story-2"></a>

### Save a note with only a few database round trips for Git history

- **Identity:** SEED-037#story-2
- **Goal / beneficiary:** A note author gets lower and more predictable save
  latency even for deeply nested notes, by reducing remaining Git-related
  database round trips beyond the current ancestor-only traversal.
- **Scope:** Start from the measured ancestor-only save in that commit: object and ref reads,
  existence checks, object inserts, binding/projection writes and transaction
  completion. Seek one/few Git persistence round trips independent of nesting
  depth without loading unrelated content. Distinguish reducing round trips
  from reducing object rows, bytes and server work. Compare simpler use of the
  existing store first, then tree-edge/path indexing or storage redesign if
  needed. Deliver the selected improvement after refinement; this is not just
  a report recommending future implementation.
- **Evaluation:** A cold content-only save at the root and at depth 12 or more
  uses the refined small round-trip budget and improves measured end-to-end
  latency against that measured save under comparable database conditions. Unrelated
  notebook growth adds no traversal; accepted content/history and rollback
  remain correct. Agree the numeric budget after consuming those measurements
  rather than inventing a promise now.
- **Design constraints:** Prefer fewer concepts and domain-cohesive owners,
  with less code when practical. Any added index, cache, store or maintenance
  mechanism must justify its total cost, including initialization, publication,
  recovery and migration. Retain native Git history and atomic synchronous
  acceptance under ADR 0002. Architectural exceptions remain human-owned.
- **Key examples:** The same one-note edit at shallow and deep paths should
  not incur one database round trip per ancestor; a cold first edit must gain
  the improvement too; a late failure must preserve prior content and history.
- **Deferred promises:** Background acknowledgement/worker delivery (the
  separate SEED-034#story-5), universal optimization of Git downloads/imports,
  and an exact chosen storage architecture. No index or migration is approved
  by this backlog entry.
- **Depends on:** the measured ancestor-only save in commit `458496764f931f05b0d46d955e1bbbebf04beefc` (`.planning/quick/012-path-scoped-note-save/PLAN.md`).
- **Effort hypothesis:** Unknown until the remaining cost and architectural
  tradeoff are established. Refine before slice planning; resplit if needed.
- **Safe stopping point:** The selected deeper optimization delivers its
  agreed latency/round-trip improvement with history and publication guarantees
  intact. The current ancestor-only save remains useful if this work is deferred.
- **Open decisions:** Numeric budget, selected design, and whether its
  complexity/migration cost is justified.

## Ordering and Scope Reduction

Story 2 follows the in-flight undo consistency fix (SEED-036#story-1). The
asynchronous assessment stays conditional. Preserve unrelated backlog order.
Do not prepare an index, alternate store, or asynchronous worker before story
2 selects one.

## When to Surface

When selecting the remaining round-trip reduction for deeply nested note saves.

## Breadcrumbs

- Refinement inspection at `0f8e0bf6ba` confirmed both recursive tree reads and
  the Hibernate-only cost test. No new runtime measurements were made during
  refinement; the counts and timings below are prior investigation evidence.
- Owner report, 2026-09-22: production content saves take 7 to 10 seconds,
  slower than expected after v1.3.18; saves were made slowly, so no lock
  queuing was involved. The same notebook saves in under a second in dev.
- Production health check reported commit 54acba9fbb (v1.3.19), which
  includes SEED-034#story-3 and its correction plan.
- Measurement (throwaway test, 2026-09-22): content save queries = 34 + 2 per
  folder; 0 folders 34, 100 folders 234, 400 folders 834. Dev copy of the
  reported notebook: 4,046 folders, 11,184 notes, nesting depth 12.
- Both tree walks already existed at v1.3.17; the release removed the
  whole-notebook load and added no per-folder work.
