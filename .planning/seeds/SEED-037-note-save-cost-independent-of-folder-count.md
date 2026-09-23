---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: story 3's hash-only assembly attempt failed (2026-09-23); a retry needs a design that avoids a whole-notebook row read per save
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

Story 3 (2026-09-22): the change-capture design that SEED-034#story-3 added
(a Hibernate interceptor recording changed rows, derivation of previous and
current paths, folder-subtree relocation, and the ancestor-directory reader)
was justified by the belief that assembling the whole notebook tree per save
is expensive. Measured on the reported notebook's shape (depth 12, 4,000
folders, 11,000 notes of 1 KB, 20 attachments of 500 KB; one content save,
median of three, local MySQL):

| Design | Time | JDBC statements | Bytes from MySQL |
|---|---|---|---|
| Current: change capture + ancestor-only trees | 85 ms | 62 | 173 KB |
| Full assembly, write everything, store deduplicates | 400 ms | 38 | 22 MB |
| Full assembly hashed in Java + top-down diff write | 130 ms | 29 | 21.5 MB |
| Full assembly hashed by MySQL + top-down diff write | 56 ms | 29 | 885 KB |

Assembling 15,000 entries in memory costs milliseconds. The cost was moving
note and attachment bytes out of MySQL, and in production the per-directory
tree reads story 1 removed. When MySQL computes each note's and attachment's
Git blob hash inside the row query
(`SHA1(CONCAT('blob ', LENGTH(content), 0x00, content))`), a save transfers
about 80 bytes per note and never the content. A top-down comparison of the
assembled tree against the accepted root reads only the directories that
differ and writes only those trees and blobs; changed note contents are then
fetched by id. The prototype's root tree hash equalled the Java assembly and
the accepted head before and after the edit.

Decision: replace change capture with the MySQL-hashed full assembly and
top-down diff (story 3), keeping story 1's write path. Ancestor-only
publication is current behavior until then. Story 2 still has to choose
whether fewer database round trips come from simpler use of the existing store
or from a new index. That choice is not selected. The decision record and
measurements are in commit `458496764f931f05b0d46d955e1bbbebf04beefc`.

**Outcome (2026-09-23):** story 3's implementation failed this decision's own
measured requirement — see the story-3 stub below. Ancestor-only publication
(change capture) remains current behavior; nothing from the attempt was kept.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. Story 2 is not authorized for implementation by this seed.

<a id="story-3"></a>

### Publish a web save from a whole-notebook assembly that never moves content bytes

**Identity:** SEED-037#story-3. **Status: experiment failed, abandoned (2026-09-23).**

Tried a top-down, hash-only tree comparison sourced from per-save SQL reads of
every note/attachment's storage-computed Git blob ID, replacing the prior
Hibernate-interceptor change capture. Slices 1–3 (hash inputs, ID-based
cutover/reset assembly, missing-object selection) worked and were proven
individually. Slice 4, wiring web saves to the new assembly and deleting the
old change-capture path, measurably regressed real web-save cost on the
story's own representative fixture (~2x median latency, more JDBC executions)
because computing the whole candidate tree's structure still requires reading
every note's and attachment's row on every save, regardless of edit size —
the opposite of this story's goal. No code from the attempt was kept; the
prior change-capture-based save path is unchanged in the current codebase.
A retry would need a design that avoids the whole-notebook row read on an
ordinary single-note save, not just a cheaper hash.

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
- **Deferred promises:** Background acknowledgement/worker delivery, universal
  optimization of Git downloads/imports, and an exact chosen storage
  architecture. No index or migration is approved by this backlog entry.
- **Depends on:** the measured ancestor-only save in commit `458496764f931f05b0d46d955e1bbbebf04beefc` (`.planning/quick/012-path-scoped-note-save/PLAN.md`).
- **Effort hypothesis:** Unknown until the remaining cost and architectural
  tradeoff are established. Refine before slice planning; resplit if needed.
- **Safe stopping point:** The selected deeper optimization delivers its
  agreed latency/round-trip improvement with history and publication guarantees
  intact. The current ancestor-only save remains useful if this work is deferred.
- **Open decisions:** Numeric budget, selected design, and whether its
  complexity/migration cost is justified.

## Ordering and Scope Reduction

Story 3 failed (2026-09-23); the codebase is unchanged from before it was
attempted — the ancestor-only change-capture save (story 1, delivered) remains
current behavior. Story 2, if pursued, starts from that unchanged ancestor-only
save, not from a story-3 measurement that no longer applies. The asynchronous
assessment stays conditional. Preserve unrelated backlog order. Do not prepare
an index, alternate store, or asynchronous worker before story 2 selects one.

## When to Surface

Story 2 when selecting the remaining round-trip reduction for deeply nested
note saves; a story-3 retry only with a design that avoids a whole-notebook
row read per save.

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
- Owner decision, 2026-09-22: after story 1 shipped, production saves on the
  reported notebook returned in about one second. The owner asked whether the
  423 production lines of SEED-034#story-3 still earned their place; the
  measurements in Alternatives and Decision answered no, and the owner accepted
  story 3 as the correction.
