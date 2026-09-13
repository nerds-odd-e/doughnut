---
id: SEED-009
status: active
planted: 2026-09-04
planted_during: ADR 0002 v1 discussion
trigger_when: when selecting the next Git-backed notebook workflow story from the product backlog
scope: large
---

# SEED-009: Refine a Donut notebook locally with Obsidian and AI-enabled IDEs

## Why This Matters

Notebook owners should move between local refinement and Donut without manual
copying, losing work, or separating notes from their learning history.
The [near-future direction](../PRODUCT-BACKLOG.md#near-future-direction) governs
selection: one append-only history, no branching or rebasing, with either
repository potentially several commits behind.

These candidates are planning hypotheses grounded in the retained workflow
boundaries and the owner's clarified direction, not a fresh implementation
audit. Confirm each gap during refinement before planning. Performance work
remains separately owned and is not decomposed or selected here.

## Alternatives and Decision

Publishing after every local commit is the strongest smaller workaround, but
does not meet the explicit requirement to catch up across accumulated commits.
Manual copying sacrifices the continuous workflow and can lose identity.
Deferring all further work would leave that requirement unanswered even after
publication becomes faster.

Recommend accumulated local publication first: it tests the central assumption
that ordinary commit-by-commit work can be exchanged without rewriting history.
Then address append-only web saves and common web authoring gaps. The ordering
is proposed on value and risk, not measured user frequency.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are comparative,
low-confidence hypotheses; refinement may split work further. Example counts
are evidence of behavior, never limits on accepted histories or note counts.
All stories preserve authorization, authored content, note identity and learning
data; invalid or ambiguous changes must not silently discard work.

<a id="story-20"></a>

### 20. Publish accumulated local commits without rewriting history

- **For / why:** An owner can commit naturally while working locally, then publish the accumulated work when Donut's accepted head is an ancestor of local main.
- **Evaluation:** Given several successive local commits editing existing notes and no independent remote changes, publication brings Donut to the local tip with the same commit IDs and order; a clean receiving checkout obtains that history and content.
- **Value / learning:** Directly closes the explicitly identified remote-behind gap. Receiving already-accepted linear history is recorded as delivered; do not invent a duplicate catch-up story.
- **Scope:** Existing-note content commits are the first delivery promise; structural-history expansion is deferred. Divergence is outside this direction. Failure must preserve the local chain and leave any accepted progress explicit and retryable.
- **Effort hypothesis:** L, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-21"></a>

### 21. Keep web saves in an append-only Git history

- **For / why:** An owner can rely on previously created Git commits remaining unchanged while editing notes in Donut.
- **Evaluation:** Given consecutive changed web saves of a note, each durable Git change appends to the previous tip without replacing a commit, whether or not a client has downloaded it; later pull retains the chain.
- **Value / learning:** The prior batching contract permits amending unexposed tips, which conflicts with the newly stated strictly append-only direction. This is a proposed behavior change, not unfinished old batching work.
- **Scope:** No new history UI or migration rewriting old commits. Refinement must explicitly reconcile the existing batching behavior with this delivery; do not treat the old amendment policy as authority to rewrite new history.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-22"></a>

### 22. Receive a web note rename locally without losing its identity

- **For / why:** An owner can rename a learned note in Donut and continue refining it locally.
- **Evaluation:** Given a synchronized notebook and a local checkout with no unpublished work, a web rename appends accepted history; pull receives the renamed path, and a later local content publication retains the same learned note.
- **Value / learning:** Closes an ordinary web action that otherwise interrupts the shared editing loop. Web identity is known, avoiding inference from a local rename-with-edit.
- **Scope:** One notebook; local history is behind or equal. Preserve authored-reference semantics. No divergent reconciliation or cross-notebook transfer.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-23"></a>

### 23. Receive a web note deletion locally

- **For / why:** An owner can remove an obsolete note in Donut and have the local notebook reflect that decision.
- **Evaluation:** Given synchronized state and no unpublished local work, deleting a note on the web appends accepted history; pull removes its Portable file while retaining existing deletion and private-data semantics.
- **Value / learning:** Prevents deleted content remaining in the owner's active local knowledge set and blocking subsequent refinement.
- **Scope:** Ordinary-note deletion, without restoring deleted notes, reusing reserved deleted paths, or reconciling independent local edits.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-24"></a>

### 24. Publish edits to existing notebook and folder Readmes

- **For / why:** An owner can refine existing container descriptions locally and use the updated descriptions in Donut.
- **Evaluation:** Given represented notebook and folder Readmes, a direct-child commit edits their valid content; publication preserves the authored bytes and existing container identities, visible in Donut and a receiving checkout.
- **Value / learning:** Makes ongoing container authorship useful after initial publication; initial creation alone does not deliver this outcome.
- **Scope:** Existing represented containers. Container creation, removal and movement are separate concerns; no divergence or broad import promise.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-25"></a>

### 25. Receive a web note move locally without losing learning history

- **For / why:** An owner can organize a note between existing folders in Donut and continue local refinement at its new path.
- **Evaluation:** Given synchronized state and a clean local checkout, a web move within the notebook appends accepted history; pull receives the new location and subsequent local editing keeps the original note's learning data.
- **Value / learning:** Completes another common organizational step in the same editing loop, independently useful after rename synchronization.
- **Scope:** Existing represented destinations within one notebook; no new-folder or folder-subtree move promise, cross-notebook transfer, or divergent reconciliation.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-26"></a>
<a id="story-27"></a>

### 26. Align note deletion, trash, and title reuse with Git

- **Goal / beneficiary:** Notebook owners can delete, trash, and recreate notes
  through Donut or ordinary Git/file operations with predictable effects on
  content, identity, and learning history, without invisible title reservations.
- **Status / priority:** Deferred combined idea, last in the product backlog.
  Former story 27 is merged here; its anchor remains an alias. This is broad
  non-executable planning input requiring decomposition before implementation,
  not a prerequisite for publication performance work.
- **Current evidence:** Source and existing controller tests inspected on
  2026-09-13 show that the database unique index includes deleted notes and the
  application rejects title reuse. UI creation offers to restore the old note
  instead of applying new creation content. Git publication rejects addition,
  rename, or relocation into a deleted note's reserved path; it does not restore
  automatically and rejection preserves accepted state. Tests were read, not run.
- **Value / learning:** Separate content recovery from identity recovery. The
  same title, even with the same content, does not reliably distinguish restoring
  an old note from creating a new one. Owners recovering the same note may expect
  their non-Git learning data back, while new notes must not inherit old progress
  merely because their names match.

#### Agreed direction — 2026-09-13

- **Permanent deletion:** Delete means removal from the database, including
  dependent data whether or not Git represents it. Publishing a file deletion
  has this meaning. Recreating that file, purposefully, accidentally, or from Git
  history, creates a new note; permanently deleted dependent data is not recovered.
- **Trash:** Replace the current soft-delete feature with a notebook trash folder
  (working name `_trash/`). Its notes remain ordinary notes represented in Git,
  retaining their identities and dependent learning data. Moving into trash
  removes a note from normal use, recall, and assimilation; moving out restores
  eligibility with retained learning data. Moving to trash frees the former path.
- **Authority:** Location under the notebook trash folder, including descendants,
  determines trash status. Metadata cannot independently make an outside note
  trashed or an inside note active. Local moves into trash without timestamp
  frontmatter are valid and must still exclude the notes from learning.
- **Timestamp and cache:** The owner wants deletion/trash-date metadata in
  frontmatter as the authoritative date when supplied, with the current
  `deleted_at` responsibility moved out of the note table into a derived index
  cache for efficient filtering. This is not a second authoritative soft-delete
  state. The cache must support trash membership even when the date is absent.
  Exact metadata naming, absent-date representation, and whether the server adds
  missing date metadata (and how that enters append-only history) remain open.
- **Existing production data:** Preserve existing soft-deleted notes and their
  dependent data by migrating them into trash, retaining deletion dates. This
  replaces the earlier suggestion to permanently discard those production rows.
- **Architecture context:** [ADR 0004 — OKF-compatible notebook Markdown
  profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs Portable paths, preserved author YAML, and derived authored indexes.
  The proposed trash-date cache and trash lifecycle are future product direction,
  not an implemented format contract or an amendment to the ADR. Proposed ADR
  0002 is non-binding.

#### Key examples and boundaries

- Active learned note → delete its file and publish → note and dependent data
  are permanently removed; later file recreation does not restore trackers.
- Active learned note → move under `_trash/` in Donut or locally and publish →
  keep its identity and trackers, represent the moved file in Git, and exclude
  it from recall and assimilation even without date frontmatter.
- Trashed note → move out to an available path → resume normal learning with
  retained tracker data. Stale timestamp metadata cannot override location.
- Existing production soft-deleted note → migration → retained note, deletion
  date, and dependent data in trash, with its former active path available.
- Trash placement and restoration still face visible filename collisions:
  different source folders, repeated trashing of the same name, or an occupied
  restoration destination. Naming/layout and conflict handling remain unresolved;
  preserving source subfolders alone does not solve repeated trashing.

#### Open decisions and delivery boundaries

- Final trash folder name/layout and collision resolution; UI distinction between
  trash and permanent deletion, including whether trash must ship before the
  existing Delete action becomes permanent.
- Timestamp key/format, missing or invalid dates, server metadata enrichment,
  and handling stale date metadata after moving out. Location authority is settled;
  timestamp completion is deliberately deferred.
- Migration details for dependent records and Git representation, and identity
  preservation across supported local moves. No migration or implementation is
  authorized by this capture. Story 23 keeps its current scope pending explicit
  refinement against this future direction.
- **Alternatives:** Title-reservation changes alone would be smaller but would
  leave ambiguous implicit recovery. Automatic restoration on filename reuse
  could incorrectly reconnect old trackers. The owner selected explicit trash
  preservation and permanent deletion, while deferring delivery in favor of
  near-future publication work.
- **Effort hypothesis:** Likely larger than L (2–4 hours), low confidence due to
  migration, Git moves, filtering, and UI consequences. Split into evaluable
  stories when resurfaced; keep one combined backlog idea now as requested.
- **Depends on / safe stopping point:** No prerequisite relationship with
  publication performance is established. Existing behavior remains until
  authorized delivery; retaining these decisions is useful if implementation
  is deferred indefinitely.
- **Source:** Owner's 2026-09-12 capture and 2026-09-13 split, discussion, and
  explicit remerge/deprioritization. The latest production migration decision
  preserves data rather than purging it.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns selection and order.
The owner merged title uniqueness and soft deletion into story 26 and moved it
last in the queue. Publication performance takes priority; the broader trash
model is useful future work, not required to pursue that immediate direction.
Defer the combined idea first when reducing current scope.
Among the remaining Git workflow stories, publish accumulated commits first,
then make new web history append-only.
Prioritize web rename and deletion ahead of container descriptions and web
relocation. Drop the latter two from the queue first if learning changes the
priority; retain their candidates here. None of this authorizes execution.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- Broader reconciliation of independently advanced local and remote histories,
  including multiple local commits, structural changes, and conflict recovery.
  Earlier proposals used ordinary Git rebase; the current direction excludes it.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Wider folder operations and rename-with-content-edit identity decisions.
  Deleted-path reuse and trash restoration are retained in deferred story 26.
- Native standard Git transport, notebook binding within a project subdirectory,
  attachments, and history browsing or revision restoration.

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
The old proposal's rebase model and web-tip amendments are not constraints on
the newly selected append-only stories.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Multi-commit publication refinement must settle useful failure/retry behavior
  if an intermediate commit is invalid, without discarding or rewriting work.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
