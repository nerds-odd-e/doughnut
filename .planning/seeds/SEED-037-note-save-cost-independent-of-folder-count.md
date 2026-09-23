---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: new owner evidence justifies revisiting abandoned whole-notebook assembly
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
publication was to remain current behavior until then. The decision record and
measurements are in commit `458496764f931f05b0d46d955e1bbbebf04beefc`.

**Outcome (2026-09-23):** story 3's implementation failed this decision's own
measured requirement — see the story-3 stub below. Ancestor-only publication
(change capture) remains current behavior; nothing from the attempt was kept.

## Story Decomposition

The retained story below records the abandoned whole-notebook assembly experiment.
It reserves no backlog position and does not authorize an automatic retry.

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
