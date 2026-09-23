---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: new owner evidence justifies revisiting abandoned save-path simplification
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
nesting depth. A note at depth 12 still pays for that chain. Detailed production
cost attribution was not measured; the owner reported roughly one-second saves
after the repair and now considers that good enough. The pre-change investigation and
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

Historical decision (superseded by the failed attempt below): replace change capture with the MySQL-hashed full assembly and
top-down diff (story 3), keeping story 1's write path. Ancestor-only
publication was to remain current behavior until then. Story 2 then still had to choose
whether fewer database round trips come from simpler use of the existing store
or from a new index. That choice was not selected. The decision record and
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

### Simplify Git publication during note saves by eliminating unnecessary database work

**Identity:** SEED-037#story-2. **Status: retry also abandoned; no-regression unestablished (2026-09-23).**

The accepted-head handoff removed the redundant post-append SQL read and passed
all 2,581 backend tests (two opt-in measurement cases skipped). Independent
review found coherent persistence ownership and preserved creation/reset,
proposal and rollback behavior. However, after mandatory formatting, affected
production code had 397 nonblank, noncomment lines both before and after;
the apparent 512 → 498 physical-line reduction came entirely from comments
and blank lines. This failed the owner's strict implementation-size gate.
The complete candidate and disposable test harness were discarded. No product
change was delivered, no latency acceptance was claimed, and no successor was
queued. Fewer SQL calls alone did not satisfy this simplify-or-abandon decision.

Original contract and plan are recoverable from `2604bfcfee`;
[execution evidence](../quick/017-simplify-note-save-publication/PLAN.md) retains
the attempt's identity, baseline measurements and final disposition.

The owner authorized a retry of the same candidate with **no increase in
implementation size**, retaining the strict no-performance-regression condition.
Four full-suite batches in baseline/candidate/candidate/baseline order measured
20 saves at each depth per batch. Root medians were **18.127 / 18.841 / 18.836 /
14.684 ms**; depth-12 medians were **41.447 / 35.367 / 41.301 / 74.032 ms**.
Candidate root medians exceeded both baselines; depth-12 varied substantially,
including roughly 70→35 ms shifts within both a baseline and candidate batch
without SQL-count changes. These observations do not prove the change caused a
slowdown, but they do not establish no regression for both shapes. Independent
review agreed to discard under the owner's condition. All code and harness
changes were restored; design, size, SQL reduction and correctness had passed.
The plan retains individual timings and proof. No successor was queued.

## Ordering and Scope Reduction

Stories 2 and 3 remain abandoned. Ancestor-only change-capture publication
remains unchanged. Neither story reserves a backlog position or authorizes an
automatic retry; further work requires a fresh owner decision and evidence.

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
