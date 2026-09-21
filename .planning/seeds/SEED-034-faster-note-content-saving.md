---
id: SEED-034
status: dormant
planted: 2026-09-20
planted_during: owner report of slow note-content saves and backlog capture request
trigger_when: prioritizing note-editing responsiveness in large notebooks
scope: medium
---

# SEED-034: Faster note-content saving in large notebooks

## Why This Matters

Note authors experience slow saves when editing content in large notebooks,
especially content containing wiki links. The owner identifies notebook 1 in
the development environment as a reproduction example and estimates that
production saves take a few seconds. Production timing remains a reported
estimate. Local investigation on 2026-09-20 reproduced substantial backend
latency on an isolated copy. The retained baseline below owns the evidence;
title-edit latency remains outside this outcome.

## Alternatives and Decision

The save path now uses durable native Git storage and shared Portable-tree
assembly that includes root attachments. First measure the original note-save
workload without attachments on either side of the comparison. Review the
combined design and unchanged attachment processing last within story 4, as a
separate assessment. Keep one complete accepted-change owner and the
existing correctness and architecture contracts; do not hide latency through
early acknowledgement or add competing representations. The owner clarified
on 2026-09-21 that practical measurements matter more than reconstructing an
exact historical benchmark, and subsequently clarified that attachments must
not be mixed into the initial performance comparison. The four-times
improvement remains the ambition; historical numeric thresholds are context,
not acceptance gates for a changed workload.

## Story Decomposition

<a id="story-3"></a>

### 3. Assess whether attachment content needs its own save-path treatment

- **Identity:** SEED-034#story-3
- **Status:** queued follow-up. Story 4 owns attachment/save-path cohesion and
  cost assessment as its final concern, after the initial performance comparison
  without attachments, per clarified owner direction on 2026-09-21. Reuse that
  evidence here rather than duplicating the investigation. No plan.
- **Goal / beneficiaries:** Note authors in notebooks that also hold sizeable
  attachments keep the save responsiveness story 2 delivers, instead of paying
  for attachment bytes on every note save.
- **Original reason for deferral:** delivered work made a notebook's root attachments
  part of its live Portable tree. Every ordinary web note save now
  loads all of that notebook's attachment bytes, byte-compares them to detect a
  no-op, and hashes them again when the tree is rebuilt
  (`AcceptedWebChangeService.commitIfChanged` → `NotebookLivePortableTree.entriesOf`
  → `NotebookAttachmentRepository.findExportRowsByNotebookId`, whose JPQL
  projection materialises each `longblob`). Before that story the tree held only
  note text, so this cost did not exist. It is correct behaviour and is what
  makes no-op detection and projection-drift checks work — it is not a defect.
  It is deferred because story 2 is concurrently replacing the accepted-repository
  storage layer and may remove, relocate or change the shape of this cost, and
  because the real magnitude depends on attachment sizes owners actually keep.
  Both implementations are now available; that deferral no longer applies.
- **Evaluation:** Consume story 4's final attachment assessment and design findings.
  If that work resolves the attachment concern or shows no meaningful cost,
  close this follow-up using its evidence. Otherwise refine only the remaining
  attachment-specific outcome. Prefer a simpler shared design over an
  attachment-only cache or bypass; do not prescribe a new digest representation
  before assessing existing Git object identity and projection guarantees.
- **Scope:** Assessment first. Any change stays inside the existing live
  Portable tree and accepted-change owners; it must not introduce a second
  content authority, weaken no-op detection or projection-drift detection, or
  change what a notebook's Portable tree contains.
- **Depends on:** Delivered story 2 (`045a5c6010815a9f106e2cc2241fe7466e24ac55`).
  The root-attachment work it pairs with is already delivered; see "Root
  attachments today" in `docs/notebook-git-synchronization.md`. Story 2 closed
  with revised scope (native storage architecture delivered; the >4× target
  itself carried forward into story 4) — story 2's actual delivered storage
  shape is now available for this assessment to measure against.
- **Effort hypothesis:** S for the measurement; unknown for any change, which
  is exactly what the measurement decides.
- **Safe stopping point:** An evidenced answer. A measured "no action needed"
  closes it.

<a id="story-4"></a>

### 4. Make note saves fast and cohesive with Git attachments, and retire legacy bundle storage

- **Identity:** SEED-034#story-4
- **Status:** in execution (plan 004). **Owner direction 2026-09-21, after slice 3
  measured current saves ~1.37x slower than `b5cad203d1`:** the >4x ambition is
  dropped from this story; saves must **not be slower** than `b5cad203d1`, and no
  complexity may remain that does not contribute to the result. Partially converted
  bindings get no repair code - users reset their notebooks' initial commit. Releases
  are created by the owner after execution merges to `main`.
- **Plan:** [Note-save performance and storage retirement](../quick/004-note-save-performance-and-storage-retirement/PLAN.md).
- **Goal / beneficiaries:** Note authors in large synchronized notebooks get
  responsive, durable saves with attachments included in Git. Evaluate the
  delivered native-storage and attachment work as one design, remove evidenced
  unnecessary work, and report practical progress toward the original >4×
  ambition without making an unsupported historical speedup claim.
- **Scope and order:** (a) first evaluate the original note-save optimization
  with a representative large notebook containing no attachments, using the
  same workload on the earlier and current implementations where a practical
  reconstructed comparison is available; keep missing historical evidence
  explicit rather than blocking current measurements; (b) address evidenced
  note-save costs within the existing design and complete story 2's deferred
  storage-retirement work below; (c) last, review native storage and attachments
  together for accidental special cases, duplicated ownership and repeated
  byte loading/comparison/hashing. Measure attachment overhead separately and
  simplify evidenced costs through the shared owners. Preserve the initial
  no-attachment result separately from this final assessment. Deployment waits
  do not block independent assessment work.
- **Storage retirement:** Run the
  delivered backfill migration (`V300000336__BackfillNotebookGitAcceptedObjects`,
  already proven correct and committed on `main`) against real dev/production
  data, verify every binding converted, then remove the legacy `bundle_bytes`
  column/path in a follow-up migration. Flyway orders migrations inside the
  starting application; it does not retire other running application versions.
  The release scripts perform rolling replacement, so do not assume that a
  completed backfill makes a column drop safe. Current native-storage code still
  maps `bundle_bytes`, reads it for legacy conversion, and writes it on creation
  and reset. Before dropping it, verify each current accepted head and reachable
  history in native storage, exclude legacy writers, and ensure every running
  version is independent of the column. For a rolling rollout, prepare a
  compatible transition (including the existing NOT NULL requirement), fully
  deploy column-independent code, then drop the column in a later migration/
  release. Alternatively, a deliberately coordinated stop of all incompatible
  instances can allow backfill, verification, drop and replacement in one
  maintenance release; the current rollout does not provide that guarantee.
  Select and verify the release procedure during execution; no release is
  authorized by this refinement. Record measured revision and migration state
  so conversion cost is distinguishable from steady-state saves.
- **Evaluation:** Exercise ordinary changed saves with existing and added or
  changed wiki links, with normal runtime warm-up, real debounce and serialized
  saves. Use a modest repeated sample and report typical time and spread, with
  timing boundaries stated (including whether debounce is counted). The first
  comparison has zero attachments in both versions: use reasonably comparable
  notes, links, history, edits and environment, not just equal attachment counts.
  Label reconstructed comparisons and report observed ratios only for those
  measured workloads. Keep current absolute timing distinct if historical
  comparison is unavailable. During the final attachment assessment, compare
  the same current notebook with and without realistic root attachments, recording
  their count, total bytes and size distribution. Any attachment optimization
  gets its own before/after comparison with the same files. No exact historical fixture,
  prescribed sample count, JFR run or tier-4 proof is required unless needed
  to explain a consequential ambiguity. Run earlier implementations only against
  isolated disposable data; do not roll back the live product. Original thresholds of
  198.75/196.25 ms remain historical context only. Report whether current saves
  are still slow and the observed improvement; inconclusive or inadequate
  results inform the next bounded decision rather than open-ended tuning.
- **Design acceptance:** The final cohesion review is required even if the
  initial no-attachment timing looks good. It is not a prerequisite for that
  measurement, and the measurement does not settle attachment performance.
  Inspect shared tree assembly, attachment projection, accepted-tree comparison
  and native-object reuse across web saves and Git publication. Distinguish
  necessary domain rules (attachments have bytes and paths, not note identity)
  from accidental per-story branches or repeated work. Prefer simplifying the
  common path; do not introduce an attachment-only cache, second content
  authority or skip attachments in drift/no-op checks. Further architectural
  changes remain human-owned. Retirement must preserve `docs/database-erd.md`
  accuracy and pass this project's FK-cascade/migration verification.
- **Final cleanup:** Owner instruction on 2026-09-21 requires complete removal
  of obsolete storage code, conversion/fallback paths, dead helpers, old-field
  fixtures, conversion-only tests, spent migrations and temporary measurement
  machinery once their transitional job is finished. Preserve tests only for
  current observable behavior, never as navigation to the old implementation.
  Use the established deployed-checkpoint/baseline procedure to remove spent
  migrations safely. Leave no legacy archive, recovery copy, tombstone, history
  narrative or completed-plan record; current product docs describe only the
  current design. Remove this story's spent planning detail at wrap-up while
  preserving unfinished sibling work and live Git transport functionality.
- **Key examples:** A large notebook without attachments in both measured
  implementations → perform comparable note edits with existing or changed wiki
  links → report typical durable-save latency and the approximate observed
  improvement, with comparison limitations. Later, a notebook holding realistic unchanged attachments → edit
  note content, with existing or changed wiki links → a complete durable save
  preserves attachment paths/bytes and history, with measured latency. The same
  setup → repeat identical content → no extra commit. Publish an attachment
  addition/edit/removal alongside note changes → one atomic accepted result
  through the shared storage path. Preserve projection-drift refusal, concurrent
  and failed-save guarantees, binding migration without history rewriting, and
  existing cutover/trash/README semantics.
- **Architecture:** Follow Accepted ADR 0002 and `docs/notebook-git-synchronization.md`;
  preserve ADRs 0001/4/5/6/7. Retirement is cleanup of an already-delivered
  design, not a new architectural exception.
- **Deferred promises:** Title/README latency, initial page loading and bulk
  publication throughput remain out of scope, as in story 2.
- **Effort hypothesis:** M, low confidence until the bounded combined assessment
  identifies whether changes are needed; do not treat this as one slice or
  prescribe speculative optimization slices before observing the current system.
- **Depends on:** Delivered story 2 (`045a5c6010815a9f106e2cc2241fe7466e24ac55`).
- **Evidence / open decisions:** Initial code inspection on 2026-09-21 finds
  one shared `NotebookLivePortableTree` and one native repository store, with
  final-set attachment projection inside publication. Ordinary web saves still
  read all accepted Git blobs and query attachment content for both pre-change
  and final snapshots. `PortableTreeEntry` equality/hash operations traverse
  bytes; blob reuse therefore does not itself eliminate byte processing.
  Publication's attachment projection and subsequent checks also reread the
  proposed tree. These are candidates to assess, not measured bottlenecks or
  proof that attachment-specific domain code is intrinsically wrong.
  `docs/note-content-saving.md` still describes per-save bundle replacement;
  reconcile that stale description during delivery with the native store.
  Story 2's real-save performance measurement
  (median 3,328.674 ms vs. the pre-native-storage baseline's 3,095.452 ms,
  within that baseline's own noise band) did not establish the >4× target or
  isolate attachment costs. The missing original browser-JIT fixture is
  unrecoverable; owner direction replaces exact reconstruction with the
  practical comparisons above. The backfill migration is delivered and proven
  correct in isolation; the previous story recorded no real-data application.
  Inspect actual environment migration and deployment state during execution
  rather than treating that historical report as current production evidence.
  Backfill verification and application compatibility are retirement prerequisites.
  Recover story 2's full evidence and design history from commit
  `045a5c6010815a9f106e2cc2241fe7466e24ac55`, path
  `.planning/quick/001-durable-native-git-storage/PLAN.md` ([Git copy](https://github.com/nerds-odd-e/doughnut/blob/045a5c6010815a9f106e2cc2241fe7466e24ac55/.planning/quick/001-durable-native-git-storage/PLAN.md)).

<a id="story-5"></a>

### 5. Assess asynchronous Git processing only if save performance remains inadequate

- **Identity:** SEED-034#story-5
- **Status:** conditionally queued, second in the product backlog by owner
  direction on 2026-09-21. No executable plan or implementation authorization.
- **Goal / beneficiaries:** Note authors whose frequent web saves remain too
  slow get an evidenced decision on whether deferring Git processing can make
  editing responsive at an acceptable complexity cost, allowing infrequent Git
  operations to bear some waiting instead.
- **Activation gate:** First consume story 4's practical performance results.
  If that work achieves the original four-times improvement, or gets close
  enough in the owner's judgment, do not explore this solution; close this
  conditional story without investigation or implementation. Explore only if
  the result falls materially short. No exact numerical definition of "close"
  is prescribed. Missing or inconclusive comparison evidence does not by itself
  activate the story: resolve adequacy with the owner using available timings.
- **Scope:** If activated, assess the smaller alternative of updating normal
  application state and recording durable pending Git work in the same database
  transaction, then constructing Git history in a background worker. Compare
  the remaining synchronous save cost, achievable benefit, and ongoing
  complexity against keeping the improved synchronous design. Reuse story 4's
  measurements and final attachment/cohesion findings; keep its initial
  no-attachment comparison distinct from attachment overhead. Full event
  sourcing is abandoned and outside this story: no authoritative domain event
  log, whole-application replay model, or event-sourcing migration.
- **Evaluation:** Give the owner a bounded adopt/defer/reject recommendation
  supported by the remaining measured bottleneck and a concrete account of
  save latency, total work, Git waiting, recovery, and design complexity.
  Background execution alone does not establish reduced total work or zero
  impact on editing. Assessment does not commit to delivering a worker.
- **Key examples and design questions:** A web save acknowledged before Git
  catches up must survive a crash with its pending work. A local publication
  arriving during that delay must not overwrite a saved web edit; assess
  ordered catch-up and stale-head revalidation. Clone/pull must have a defined
  freshness boundary. Retrying a worker must not duplicate commits or lose
  ordering. Preserve immutable content needed by pending changes, including
  attachment bytes, without copying all unchanged attachments on every save.
  Distinguish processing batches from collapsing several saves into one commit;
  history consolidation is not implicitly authorized. Review whether the same
  shared owners can handle notes, attachments and publication coherently.
- **Architecture:** This is exploration of a possible change to Accepted
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  and its [synchronization contract](../../docs/notebook-git-synchronization.md):
  acknowledging saved application content before accepted Git advances changes
  current content authority and atomic-publication guarantees. Describe that
  tradeoff explicitly; any implementation requires a human-owned architecture
  decision and separate execution authorization.
- **Depends on:** SEED-034#story-4's performance and combined-design assessment.
- **Effort hypothesis:** Unknown until activation; bound the assessment before
  planning any experiment. Do not pre-plan a speculative implementation.
- **Safe stopping point:** Either close without exploration because performance
  is sufficient, or deliver an evidenced recommendation. Rejecting or deferring
  the asynchronous design is a valid outcome.

## Ordering and Scope Reduction

Queue ahead of the remaining note-presentation cleanup, which was explicitly
deferred to last. Story 4 is queued first: measure without attachments first,
then finish with the combined design and unchanged-attachment assessment.
Story 5 is second in the product backlog solely as a conditional fallback:
do not explore it if story 4 reaches or gets sufficiently close to the four-times
improvement. Full event sourcing is abandoned; only the smaller asynchronous
Git-processing option remains a candidate if the activation gate is met.
Story 3 retains its queue position as a follow-up for any unresolved
attachment-specific outcome, reusing story 4's evidence. Preserve the near-future
direction and leave implementation to its own bounded plan.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner direction, 2026-09-21, clarified: assess the delivered save optimization
  and root attachments within the same story, including cohesion and accidental
  specialized code. First measure the original workload without attachments on
  either side; address unchanged attachment processing last in a separate
  assessment. This supersedes the earlier interpretation that attachments must
  be included in the primary comparison. Practical evidence is enough; exact
  historical reconstruction remains unnecessary.
- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-20: queue the remaining 4× target and native Git
  storage redesign first; allow conservative production-code growth while
  retaining correctness and architecture requirements.
- Owner decision, 2026-09-20: story 2 closed with revised scope after
  delivering the native storage architecture (schema, JDBC-backed object
  store, every caller cut over, atomicity, publication ancestry, lifecycle,
  and a proven-but-not-yet-run backfill migration) with no measured real-save
  regression, since gate 1's browser-JIT baseline proved genuinely
  unrecoverable this session. The unmet >4× target and deferred storage
  retirement carry forward into story 4, queued alongside the already-queued
  story 3 (attachment save-path assessment), which depends on both.
