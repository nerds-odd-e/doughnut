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
latency on an isolated copy. Plain note-content saves are now faster than
before this seed's work; the remaining concern is attachment bytes, which story
3 owns. Title-edit latency remains outside this outcome.

## Alternatives and Decision

The save path uses durable native Git storage and shared Portable-tree assembly
that includes root attachments, and decides drift and no-op saves by comparing
Git blob identities. Keep one complete accepted-change owner and the existing
correctness and architecture contracts; do not hide latency through early
acknowledgement or add competing representations.

## Story Decomposition

<a id="story-3"></a>

### 3. Assess whether attachment content needs its own save-path treatment

- **Identity:** SEED-034#story-3
- **Status:** queued. Assessment evidence gathered on 2026-09-21 (below); only the
  unresolved outcome remains. No plan.
- **Goal / beneficiaries:** Note authors in notebooks that also hold sizeable
  attachments get note saves whose cost does not grow with attachment bytes they
  did not change.
- **Evidence (measured 2026-09-21, 11,000-note notebook, same revision and edits):**
  28 realistic root attachments totalling 12,554,240 bytes (icons, screenshots,
  photos and two PDFs, all incompressible) add about **+515 ms to every note-save
  request** - roughly 41 ms per MB - over a base request of about 720 ms. Before
  saves compared trees by Git blob identity (`562c93a893`) the same files added
  about +620 ms.
- **Already resolved:** a save no longer reads accepted blob bytes; drift and no-op
  decisions compare path-to-blob-id maps taken from tree objects, and unchanged
  blobs are reused without being re-inserted. No attachment-specific save path
  exists - the cost runs through the shared live Portable tree and accepted-change
  owners.
- **Unresolved outcome:** each changed save still assembles the live Portable tree
  twice - the before-snapshot for projection-drift detection and the
  after-snapshot - and each assembly loads every attachment's bytes through
  `NotebookAttachmentRepository.findExportRowsByNotebookId`, then hashes them to
  compute blob ids. The cost therefore grows with total attachment size
  (extrapolated, unmeasured: about 5 s per save at 100 MB).
- **Scope and constraints:** any change stays inside the live Portable tree and
  accepted-change owners. No second content authority, no attachment-only cache or
  bypass, no weakening of no-op or projection-drift detection, and no change to
  what a notebook's Portable tree contains. Reusing before-snapshot attachment rows
  in the after-snapshot on the assumption that web edits never touch attachments
  was assessed and rejected: it is a second content authority in disguise.
- **Effort hypothesis:** S to decide whether the remaining cost is acceptable for
  the attachment sizes owners keep; unknown for any change.
- **Safe stopping point:** an evidenced decision that the remaining cost is
  acceptable, or one bounded simplification proven with a same-fixture
  before/after measurement.

<a id="story-5"></a>

### 5. Assess asynchronous Git processing only if save performance remains inadequate

- **Identity:** SEED-034#story-5
- **Status:** restored on 2026-09-22 at the owner's direction; removed on
  2026-09-21 when story 4 closed, on the premise that note saves were then
  faster than before. Story 4's closed outcome (saves at about 0.6× the old
  time; the owner explicitly dropped the >4× ambition for that story rather
  than reach it) is now the evidence available for this story's activation
  gate below. Whether that outcome satisfies or falls short of the gate is
  an open owner decision, not resolved by this restoration. No executable
  plan or implementation authorization.
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
- **Depends on:** SEED-034#story-4's performance and combined-design assessment
  (closed; recover its full evidence and design history from commit
  `a62ecad9fb74ba316af133a35793a1e71f795028`'s parent,
  `2f0f1a969b`).
- **Effort hypothesis:** Unknown until activation; bound the assessment before
  planning any experiment. Do not pre-plan a speculative implementation.
- **Safe stopping point:** Either close without exploration because performance
  is sufficient, or deliver an evidenced recommendation. Rejecting or deferring
  the asynchronous design is a valid outcome.

## Ordering and Scope Reduction

Story 3 is a follow-up on the attachment cost that still reaches every note
save. Story 5 is restored second in the product backlog as a conditional
fallback pending the owner's reading of story 4's closed result against its
activation gate. Preserve the near-future direction and leave implementation
to its own bounded plan.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-22: restore story 5 (asynchronous Git processing),
  which was removed on 2026-09-21 when story 4 closed; queue it second in the
  product backlog.
