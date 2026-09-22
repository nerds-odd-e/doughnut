---
id: SEED-037
status: dormant
planted: 2026-09-22
planted_during: owner report that production note saves take 7 to 10 seconds after SEED-034#story-3 shipped
trigger_when: now; story 3 replaces change capture with a cheaper whole-notebook assembly
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

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses. Story 2 is not authorized for implementation by this seed.

<a id="story-3"></a>

### Publish a web save from a whole-notebook assembly that never moves content bytes
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Identity:** SEED-037#story-3
- **Goal / beneficiary:** Maintainers get one whole-notebook Portable-tree
  encoder for cutover, history reset, publication comparison and web saves.
  Note authors retain synchronous, atomic saves with no measured latency
  regression. Removing change capture and partial-tree derivation is the
  simplification outcome; roughly 500 fewer production lines is a hypothesis,
  not a deletion quota.
- **Scope:** Assemble paths and Git blob IDs from the current projection after
  the complete operation is flushed under the existing notebook locks. MySQL
  computes note and legacy attachment Git blob IDs without returning their
  payloads. Match the codec's exact UTF-8 bytes, null-note-as-empty rule and
  arbitrary binary bytes; byte length is not character count. Continue using
  the existing README codec and `.keep` rules. Fetch file content only for Git
  blobs the target repository actually needs, not merely because a path moved.
  Cutover/reset necessarily materialize initial blobs; "never moves content
  bytes" describes hash-only assembly, not the entire publication operation.
  Compare trees top-down and reuse existing object IDs, then append through
  the existing native transactional writer. Keep the lock ordering, one commit
  per changed notebook, stale-publication checks, identity outcomes and rollback.
- **Accepted attachment architecture:** Follow ADRs
  [0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and [0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  [the LFS contract](../../docs/notebook-git-lfs.md) and the
  [staged transition](../NORTH-STAR.md#attachment-storage-transition).
  This story delivers the current legacy representation, not LFS rollout.
  The assembly contract is the Git blob ID of the file representation: today
  a raw legacy attachment blob; after conversion, the standard pointer blob's
  Git ID, distinct from the payload's LFS SHA-256. Do not embed GCS URLs, add
  a payload-hashing dependency to the assembler, or prepare a second storage
  framework. The later LFS stories supply pointer metadata without reading or
  rehashing unchanged bucket objects. Attachment hash/pointer metadata is part
  of that accepted transition, no longer an undecided future optimization.
- **Publication semantics:** As already selected in this story's 2026-09-22
  decision, the next accepted web save adopts existing projection drift at
  untouched paths into its new commit. Local publication still detects drift;
  no accepted history is rewritten. Replace the old web-drift assertions with
  an explicit example of the new rule. Update the domain-operation section of
  `docs/notebook-git-synchronization.md` and `docs/note-content-saving.md` when
  delivered; there is no numbered "drift decision 4" in the current ADRs.
  Preserve canonical no-op behavior, including the existing rule that Git file
  mode alone is not a Portable-content change.
- **Evaluation:** Reproduce the depth-12, 4,000-folder, 11,000-note fixture
  with 1 KB notes and 20 × 500 KB attachments on local MySQL 8.4. Compare cold
  controller saves before/after under the same conditions, including transaction
  completion. Require under 1 MB of returned SQL value bytes, no more real JDBC
  executions than the measured baseline and no median latency regression.
  Report result-value bytes, not an unmeasured network-wire total. A content
  edit reads at most one accepted tree per differing directory and inserts
  only missing blobs, changed trees and one commit. Independently decoded
  downloaded history must match expected paths and exact content after the
  supported note/folder/README and multi-notebook operations. The full backend
  suite and Git-focused E2E features remain green; timing is a local experiment,
  not a flaky CI wall-clock assertion or a production latency claim.
- **Simplification:** Remove `ProjectionChangeCapture`,
  `NotebookGitChangedFolders`, `NotebookGitChangedFiles`,
  `NotebookGitAcceptedDirectoryReader`, relocation/unresolved-tree machinery
  and the encoder's derive path once their callers use full assembly. Remove
  obsolete capture/derivation-specific tests, preserving their product promises
  through controller observations rather than self-comparison with the new
  encoder. Keep one format implementation, not a parallel SQL-specific codec.
- **Deferred promises:** GCS transfer, LFS admission/hydration/metadata rollout,
  existing-attachment migration, the 10 MiB limit, per-note stored hashes,
  asynchronous acceptance and story 2's additional round-trip optimization.
  Existing attachment and cross-notebook guards remain; no new supported move
  or upload workflow is introduced here.
- **Key examples:**
  1. Edit one note at depth 12 among unrelated notes and binary files → exact
     saved bytes in one child commit, at most 13 accepted-tree reads and 13
     changed-tree inserts plus the new note blob; no unchanged payload fetched.
  2. Move a folder with 1,000 unchanged notes → correct new paths and markers,
     reusing existing subtree/blob IDs without fetching those note bodies.
  3. Save equivalent canonical content → no new objects or commit, including
     when the accepted file is executable. Empty/null and multibyte note
     content hash exactly as the existing Java codec does.
  4. An unrelated projected note differs from accepted history, then a web edit
     succeeds → both projected contents appear in that one new commit. A local
     publication attempted before that save still refuses projection drift.
  5. A supported operation changes two notebooks, or fails late during binding
     persistence → one commit per changed notebook on success; on failure,
     neither projection, accepted head nor native object rows are left changed.
- **Depends on:** story 1 (delivered).
- **Effort hypothesis:** L (2–4 hours), medium confidence. Prototype evidence
  supports the design; byte accounting, mode-only no-ops and removal of
  derivation-specific assertions are the main proof work.
- **Safe stopping point:** The full assembly is the only encoder and every
  web save publishes through it with the measured cost.

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

Story 3 comes first, ahead of the in-flight undo consistency fix
(SEED-036#story-1): it removes code the undo fix would otherwise have to work
around, and it already brings the save to 29 statements. Story 2 follows the
undo fix and starts from story 3's measured save. The
asynchronous assessment stays conditional. Preserve unrelated backlog order.
Do not prepare an index, alternate store, or asynchronous worker before story
2 selects one.

## When to Surface

Now, as the first product-backlog story; story 2 when selecting the remaining
round-trip reduction for deeply nested note saves.

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
