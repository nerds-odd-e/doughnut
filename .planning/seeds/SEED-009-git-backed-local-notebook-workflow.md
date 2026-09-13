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

The owner prioritized append-only web saves, receiving web renames, and receiving
web deletions ahead of accumulated local publication on 2026-09-13. These basic
web workflow changes take precedence while publishing after each local commit
remains a smaller workaround. Accumulated publication is retained after them;
its frequency and urgency remain unmeasured.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are comparative,
low-confidence hypotheses; refinement may split work further. Example counts
are evidence of behavior, never limits on accepted histories or note counts.
All stories preserve authorization, authored content, note identity and learning
data; invalid or ambiguous changes must not silently discard work.

<a id="story-20"></a>

### 20. Publish accumulated local commits without rewriting history

- **Refinement status:** Refined on 2026-09-13. The narrower delivery and failure
  policy below are recommendations, not approved product decisions. No execution
  plan or implementation is authorized by this refinement.
- **Goal:** A notebook owner can finish a local editing session with several
  commits, publish once, and continue using those notes in Donut without
  reconstructing work or losing note identity and learning history. Preserving
  commit IDs is an existing direction constraint; the user benefit is freedom
  from publishing after every commit.
- **Current evidence:** Focused inspection confirms both
  `cli/src/commands/notebook/notebookPublishAncestry.ts` and
  `NotebookGitProposalAncestry.java` require the same head or one direct child.
  Existing CLI and controller tests encode that restriction. This establishes
  a real capability gap, not its frequency or urgency. Tests were inspected,
  not run, during refinement.
- **Purpose and timing challenge:** The owner moved this story below append-only
  web saves, web rename, and web deletion on 2026-09-13 following refinement.
  The seed contains no measured frequency of blocked accumulated
  work. If owners can comfortably publish after each commit, defer this story;
  meeting a Git direction alone does not establish value now. If web saves
  commonly overlap local sessions, this narrower delivery may have little
  practical value: story 21 addresses web history rewriting, but neither story
  resolves divergence. Reconsider the workflow priority before expanding this
  story to cover simultaneous editing.
- **Strongest simpler alternative:** Publish after every local content commit
  using existing functionality. Try this as a baseline with the owner. It is
  insufficient when an owner already has accumulated commits or needs to commit
  without connectivity. Squashing or rebasing conflicts with the stated
  append-only direction. A new UI or generic synchronization mechanism is not
  needed to evaluate the selected outcome.
- **Proposed delivery scope:** One already bound notebook; Donut's content
  matches its accepted history; accepted head is an ancestor of local `main`;
  each intervening commit edits content of existing ordinary notes at unchanged
  paths. Publish through the existing command and show the final contents in
  Donut. Retain the original commit chain so a clean receiving checkout obtains
  the same history and final content. Several edits to the same note are the
  first example; edits across existing notes follow the same promise. Example
  commit and note counts are not limits.
- **Proposed failure simplification:** Accept the complete eligible chain or
  leave Donut's accepted head and live content unchanged. Preserve local work
  in either case. After an interrupted response, retry can discover that the
  tip was already accepted without duplicating work. If the remote advances,
  fail clearly without overwriting it; automatic recovery is deferred. This
  replaces the ambiguous partial-progress promise with a proposed atomic
  outcome, subject to confirming that existing publication supports it.
- **Deferred promises:** Multi-commit creation, deletion, renaming, moving,
  container Readme changes, simultaneous web/local editing, recovery of live
  content already inconsistent with accepted Git history, history UI,
  per-commit progress/resume, and 10,000-note performance targets. Deferral
  does not itself require rejecting naturally supported cases or regressing
  existing single-commit behavior. Branching, merging and rebasing remain
  outside the explicitly selected linear-history direction.
- **Key examples:**
  1. Donut and local start at A. Locally B edits an existing note and C refines
     it again. Publish once: Donut shows C's contents, the same learned note
     remains, and a clean receiver obtains A → B → C with identical commit IDs.
  2. The response is lost after successful acceptance of C. Retrying reports
     the accepted C and leaves history and note identity unchanged.
  3. Donut advances independently from A before publication. Publication fails
     clearly, preserving both sides; this delivery does not reconcile them.
- **Learning and stop condition:** Have an owner perform one real local editing
  session with accumulated content commits, publish once, and continue learning
  in Donut. Compare with the publish-after-each-commit baseline: did batching
  remove an actual interruption, and did the no-overlapping-web-edits condition
  fit the session? Record any unsupported operation that blocked completion.
  One successful session establishes workflow feasibility, not widespread
  demand. Use that evidence before adding structural history or more recovery.
- **What looks easiest:** Reuse the existing publication command, content-edit
  application and clean receiving flow. The publisher already has a transaction
  and same-head handling, which makes atomic publication plausible, not proven.
  Removing the two ancestry guards alone is insufficient evidence of correct
  history validation, retry behavior or preservation of learning data.
- **Effort hypothesis:** Retain L (2–4 hours), low confidence, until intermediate
  history semantics are settled. M (1–2 hours) is plausible only if existing
  publication and receipt can safely handle the chain without per-commit live
  application or new recovery machinery. No basis yet for an S estimate.
  If bounded delivery still exceeds L, revisit the promise rather than quietly
  adding a general history processor.
- **Assumptions to validate:**
  1. Owners actually accumulate commits often enough to prefer this over
     publishing each commit; demand evidence is missing.
  2. Existing-note content editing alone is useful before structural changes;
     a real session should test this.
  3. Owners can use a session without overlapping web changes; this is a
     delivery precondition, not an enforced editing lock.
  4. Donut's live content matches accepted history at session start; repairing
     an existing mismatch belongs to separately selected recovery work.
  5. Every intermediate commit in the first example is a valid existing-note
     content edit. Whether invalid intermediate content repaired at the tip
     must be accepted is unresolved; the example is not a rejection rule.
  6. Only the tip must become live Donut content; preserving intermediate Git
     commits does not require exposing each intermediate state in the app.
  7. Whole-publication atomicity and retry can reuse existing behavior. The
     transaction and same-head branch support investigating this, but do not
     prove all storage and network failure outcomes.
  8. Existing clone and clean pull can receive the longer accepted history;
     focused end-to-end evidence is still needed for this publication flow.
  9. Existing ownership checks, content fidelity, note identity and learning
     preservation remain applicable; these are obligations, not scope cuts.
- **Open decisions before execution planning:** Confirm whole-chain acceptance
  versus partial progress. Decide whether an intermediate invalid or structural
  state repaired by the tip is merely outside this delivery's promise or must
  be rejected, with a product reason for any rejection. Confirm value now with
  an actual blocked session or the owner's stated expected usage.
- **Depends on / safe stopping point:** Existing clone, publication and clean
  fast-forward pull; no new-story prerequisite is established. This content-only
  local-session workflow remains useful if all later stories are cancelled.

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
- **Status / priority:** Deferred combined idea, near the bottom of the product backlog,
  ahead only of the explicitly deprioritized publication-optimization work.
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
The owner merged title uniqueness and soft deletion into story 26. The broader
trash model is useful future work, but Git workflow stories now take priority
and publication-optimization work follows it at the bottom of the queue. Defer
the combined idea first when reducing Git workflow scope.
Among the remaining Git workflow stories, make new web history append-only,
then receive web renames and deletions, then publish accumulated local commits.
This is the owner's priority decision after challenging story 20's value now.
Prioritize these stories ahead of container descriptions and web
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
- Story 20 owns the proposed atomic publication policy and unresolved
  intermediate-history semantics; settle those decisions before its execution
  planning, without discarding or rewriting work.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
