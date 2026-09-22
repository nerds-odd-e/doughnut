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
the development environment as a reproduction example. Owner report on
2026-09-22: that notebook holds no attachments and a content save's API call
takes about 1.2 s; production saves take about 6 to 7 s, and the owner
believes production notebooks hold sizeable attachments. Plain note-content
saves became faster in story 4 (released in v1.3.15), but every save still
did work proportional to the whole notebook: it rendered and hashed every
note twice and loaded and hashed every attachment's bytes twice. Story 3
removed that whole-notebook cost from the frequent save path. Title-edit
latency beyond what that naturally gives remains outside this outcome.

## Alternatives and Decision

The save path uses durable native Git storage. Until story 3, it rebuilt the
notebook's complete Portable tree from MySQL on every web change and let Git
find the difference. The decision for story 3 was to derive the new commit's
tree from the accepted head's tree plus the rows the change actually touched,
keeping one complete accepted-change owner, one content authority and the
existing publication contracts. Rejected alternatives: stored blob-id or hash
columns (derived caches that keep per-save work proportional to the notebook),
per-operation declarations of changed paths (scattered and incomplete), early
acknowledgement or background Git work (story 5's separate question).

## Story Decomposition

<a id="story-3"></a>

### 3. Save note edits at a cost proportional to the change, not the notebook

- **Identity:** SEED-034#story-3 (unchanged; retitled on 2026-09-22 from
  "Assess whether attachment content needs its own save-path treatment"; the
  assessment is complete and its evidence is kept below).
- **Status:** refined 2026-09-22; executable plan
  [`quick/010-change-proportional-note-save`](../quick/010-change-proportional-note-save/PLAN.md).
  The drift policy (architecture decision 4) was decided by the owner on
  2026-09-22. No execution authorization.
- **Goal / beneficiaries:** Note authors in large notebooks get web saves
  whose server work depends on what they changed, not on how many notes or
  how many attachment bytes the notebook holds. This keeps editing responsive
  as notebooks grow through local AI IDE work and through the coming move of
  note images into notebook attachments (SEED-035 story 5), instead of
  degrading linearly with notebook size.
- **Evidence:**
  - Measured 2026-09-21 on an 11,000-note fixture: base save request about
    720 ms with no attachments; 28 realistic attachments totalling
    12,554,240 bytes add about +515 ms to every save, about 41 ms per MB,
    linear. Extrapolated: 100 MB of attachments adds about 4 s per save.
  - Owner report 2026-09-22: development notebook 1 (no attachments) saves in
    about 1.2 s; production saves take 6 to 7 s and production notebooks are
    believed to hold sizeable attachments. Story 4's speed-up is in the
    released code (v1.3.15 and later), so these figures describe the current
    design.
  - Cause: `AcceptedWebChangeService` assembles the complete live Portable
    tree twice per change (a before-snapshot for whole-tree drift detection
    and an after-snapshot for the commit). Each assembly renders every note
    and loads every attachment's bytes through
    `findPortableTreeRowsByNotebookId`, then hashes all of it to compute blob
    ids. The Git write side already skips unchanged blobs; the cost is the
    rebuild, not the commit.
- **Scope, promised behavior:**
  1. Every web accepted change that goes through the accepted-change owner
     today (note content and title edits including referrer rewrites, note
     creation, note move, trash, recovery and permanent removal, folder
     creation, rename, move, trash and permanent deletion, folder README
     edits, relationship reduction across its notebook set) appends its commit
     by deriving the new tree from the accepted head's tree and the rows that
     change inserted, updated or deleted. Unchanged notes are neither rendered
     nor hashed. Unchanged attachments' bytes are neither read from the
     database, nor hashed, nor written to Git, regardless of where in the
     notebook they sit.
  2. A change that only moves paths (folder rename or move, trash, recovery,
     note move) re-lists the affected entries under their new paths using the
     blob ids already in the accepted tree. Attachments follow their folder
     without their bytes being read.
  3. The `.keep` marker for folders without other content, and folder and
     notebook `README.md` files, keep their current Portable rules; the rule
     is applied to the directories the change touched.
  4. A canonical no-op save (same resulting tree) still leaves accepted
     history unchanged.
  5. The complete assembly of a notebook's tree remains for repository cutover
     and history reset, and for the local publication drift check, and is the
     same derivation applied to an empty base, so one place decides how
     projection rows become Portable entries.
  6. The synchronization contract
     ([domain operation ownership](../../docs/notebook-git-synchronization.md#domain-operation-ownership))
     is updated to describe derivation from the projection change.
- **Rejection constraints (each with its independent reason):**
  - No second content authority, attachment-only cache or bypass
    ([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
    content authority).
  - No stored blob-id or content-hash columns, per-note caches, or background
    precomputation (owner direction 2026-09-22: manipulate less data through a
    simpler design; caches keep per-save work proportional to the notebook).
  - No change to what a notebook's Portable tree contains or how files are
    encoded ([ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)).
  - Exactly one commit per accepted web change on linear `main`, appended in
    the same transaction as the projection (ADR 0002 publication boundary).
  - No asynchronous or deferred Git work (story 5 owns that question).
- **Deferred (not built or verified here):** making local publication
  acceptance or its drift check incremental (infrequent; still assembles the
  full tree with attachment bytes); title-edit latency beyond the natural
  effect; web attachment upload or deletion (SEED-035); removing the attachment
  bytes column from MySQL (decide together with web download, SEED-035
  story 1); a retained performance harness.
- **Key examples:**
  1. *Content edit in a large notebook.* Notebook with 11,000 notes and 28
     attachments (12.5 MB). Trigger: save new content for one root note.
     Result: one commit whose tree differs from the parent's only at that
     note's path; the request issues the same database statements as the same
     edit in a notebook holding one note and no attachments; the commit's tree
     equals the full assembly of the projection.
  2. *Folder rename carrying attachments.* Folder `Photos/` holds three notes
     and 10 MB of images. Trigger: rename to `Pictures/`. Result: the new tree
     lists every entry under `Pictures/` with the blob ids the parent tree had
     under `Photos/`; no attachment content is read; oracle equality holds.
  3. *Folder emptiness marker.* Folder `Ideas/` is empty (tree holds
     `Ideas/.keep`). Trigger: create note "Plan" in it. Result: `Ideas/.keep`
     is gone and `Ideas/Plan.md` is present. Trigger: trash that note. Result:
     `_trash/Ideas/Plan.md` appears and `Ideas/.keep` returns.
  4. *Title rename with referrer rewrites.* Note "A" is linked from three
     notes. Trigger: rename "A" to "B" with references rewritten. Result: the
     tree drops `A.md`, adds `B.md`, and replaces the three referrers' blobs;
     nothing else changes; oracle equality holds.
  5. *Canonical no-op save.* Trigger: save content that encodes to the same
     bytes. Result: accepted head unchanged, note timestamp updated (existing
     behavior).
  6. *Pre-existing drift.* A note was created outside the owner and exists in
     MySQL but not in Git. Trigger: edit a different note. Result: the edit is
     committed on the accepted head and
     the unsynchronized note stays absent from Git until a history reset or a
     local publication surfaces the drift.
- **Architecture (decisions made up front, 2026-09-22):**
  1. **Derive, do not rebuild.** The accepted head's tree is the base; a web
     change becomes removals, renames and additions of tree entries. Git
     commits stay full snapshots, shared by object id. No ADR changes: ADR
     0002's publication boundary, content authority and linear history are
     untouched; only how the owner computes the tree changes.
  2. **The owner captures the projection change at flush.** The accepted
     change owner binds a per-transaction collector to Hibernate's flush
     events (an `Interceptor` registered through Spring Boot's Hibernate
     customizer) for Note, Folder, NotebookAttachment and Notebook rows:
     inserts, updates with the pre-update path fields (folder or parent,
     title or name or filename) and deletes. Domain operations declare
     nothing, so referrer rewrites and other indirect changes are captured
     without each operation knowing about Git. Rows changed in a notebook the
     operation did not lock are ignored, as today. Attachments cascade-deleted
     with their folder at the database level are covered by the folder's
     prefix removal. No repository on these tables uses bulk update queries
     (checked 2026-09-22), so flush events see every change.
  3. **One encoder over path-to-blob maps.** A Portable tree revision is a
     map from path to blob id. Deriving a revision applies the captured
     change: remove old paths, rename entries under changed folder prefixes
     (deepest changed ancestor wins), add new entries whose content is in
     hand, then apply the `.keep` rule to the directories touched. New blobs
     are inserted only for added or replaced content. The full assembly for
     cutover, history reset and publication drift checks is this derivation
     over an empty base with every row as an insertion, so
     `PortableTreeSnapshot`, `NotebookLivePortableTree` and
     `NotebookGitLivePortablePath` merge into that one encoder.
  4. **Drift policy on the web path (owner decision, 2026-09-22): drop the
     before-snapshot.** Today a whole-tree before-snapshot detects any
     pre-existing drift and silently skips the commit, after which that
     notebook never commits again until a history reset. A derived commit
     never adopts drift at untouched paths by construction, records the
     author's edit as ADR 0002 requires, and drift stays detectable where the
     whole-tree comparison already lives, the local publication check (409
     "refresh the checkout"). Rejected: a path-scoped check that skips the
     commit when the changed rows' pre-change content does not match the
     accepted tree at their old paths; it needs pre-change content and keeps
     silently dropping edits. The existing test
     `preExistingPortableDriftKeepsTheWebSaveAndAcceptedHistoryUnchanged` is
     rewritten to the new policy.
  5. **No-op detection is tree identity.** A derived tree id equal to the
     parent's tree id means no commit.
  6. **Publication acceptance stays as is.** Git-to-web publication, its
     projection and its drift check are outside this story.
- **Effort hypothesis:** M to L (one day of slices), medium confidence. The
  mechanism is known; risk sits in change-capture completeness and the local
  `.keep` rule, both guarded by an oracle test comparing every derived tree
  with the full assembly.
- **Safe stopping point:** each slice keeps every operation correct because
  change kinds the derivation does not yet cover fall back to the full
  assembly; the fallback is removed in the last slice. Stopping early retains
  the cheap path for the covered change kinds.

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

Story 3 (closed 2026-09-22) removed the whole-notebook cost from every web
save: each accepted web change derives its commit from the accepted head's
tree and the rows the change touched, so unchanged notes are not rendered and
attachment bytes are never read. Measured on an 11,000-note notebook with 28
attachments (12.5 MB): the content-save request median fell from about
1.3 s to about 180 ms, and the request part of a save that adds a wiki link
from about 1.3 s to about 180 ms as well. Story 5 stays a conditional
fallback: judge its activation gate against that result before exploring
deferred Git work. Web README edits do not yet enter the accepted-change
boundary; correction plan `quick/011-readme-edits-through-accepted-change-owner`
routes them.

## When to Surface

When selecting performance work on editing notes in large notebooks.

## Breadcrumbs

- Owner report, 2026-09-20: content saves in large notebooks, especially with
  wiki links, are slow; development notebook 1 is an example, production is
  estimated at a few seconds, and the requested improvement is more than 4×.
- Owner direction, 2026-09-22: restore story 5 (asynchronous Git processing),
  which was removed on 2026-09-21 when story 4 closed; queue it second in the
  product backlog.
- Owner direction, 2026-09-22: production saves take 6 to 7 s and development
  notebook 1 (no attachments) about 1.2 s; the full-snapshot design is
  challenged because saving is frequent and should manipulate far less data.
  Story 3 is reframed from an assessment into the change-proportional save
  promise, kept as one story, with architecture decided up front.
