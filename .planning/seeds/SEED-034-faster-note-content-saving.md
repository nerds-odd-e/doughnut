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

## Ordering and Scope Reduction

Story 3 is the only remaining story: a follow-up on the attachment cost that
still reaches every note save. Preserve the near-future direction and leave
implementation to its own bounded plan.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
