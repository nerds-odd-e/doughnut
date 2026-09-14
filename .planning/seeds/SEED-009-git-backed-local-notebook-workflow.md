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

<a id="story-35"></a>

### 35. Receive web-created folders and their notes locally

- **Goal / beneficiary:** A notebook owner can organize new material in a folder
  on the web, then receive that folder and its ordinary notes locally. Creating
  the folder before writing its first note must also survive clone and pull.
- **Human direction (2026-09-14):** Keep web folder creation and creating a note
  in that new folder together in one story. Represent an otherwise empty folder
  with `.keep`. Include empty folders in initial Git snapshots of existing
  notebooks as part of the same outcome.
- **Current evidence:** `PortableTreeSnapshot` emits note files and non-blank
  container Readmes, but no placeholder for otherwise empty folders.
  `WebNoteCreationService` advances accepted history only when the starting
  projection matches and the destination folder is already represented. These
  are inspected code boundaries, not newly executed acceptance evidence.
- **Scope:** Web-created folders, including nested folders, and ordinary notes
  subsequently created in them reach a clean receiving checkout through the
  existing clone/pull flow. Empty folders use a tracked `.keep` placeholder,
  not a note or an invented Readme description. Initial snapshots preserve
  existing empty folders too. Existing note/folder identities, authored content,
  learning data, and accepted commit IDs remain intact. Subsequent web changes
  append history; updating support must not rewrite an existing initial commit.
- **Key examples:**
  1. A synchronized notebook has no `Biology` folder. Create it on the web,
     then pull: local `Biology/.keep` preserves the empty folder.
  2. Create `Cells` in that folder on the web, then pull: local
     `Biology/Cells.md` contains the new note. Receiving both web changes in one
     pull also works; no intervening owner synchronization is required.
  3. An existing notebook contains an empty nested folder `Science/Biology`.
     Its first Git snapshot and clone retain that path through `.keep`, with
     existing notes and non-blank Readmes preserved.
- **Why now / order:** Folder Trash is already available, and Restore is not a
  prerequisite. This completes basic web authoring into a new destination. It
  is a value ordering, not a claim that accumulated content publication
  technically depends on folder creation.
- **Strongest smaller alternative:** Create notes only at the root or in
  already represented folders, or write a non-blank Readme to retain an empty
  folder in a snapshot. Those workarounds constrain ordinary organization or
  require artificial content; they do not deliver the requested empty-folder
  and new-folder authoring journey.
- **Highest learning:** Does completing folder creation let an owner organize
  new material on the web and continue locally without manual folder repair or
  an inconsistent accepted history?
- **Deferred promises:** Folder moves/renames, trash synchronization, new local
  folder-publication capabilities, divergence recovery, and performance targets.
  Preserve already supported publication when received trees contain `.keep`;
  deferral does not authorize breaking the next existing local edit/publish.
- **Architecture context:** [ADR 0004 — OKF-compatible notebook Markdown
  profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  requires tracked content to retain empty folders and omits blank Readmes.
  `.keep` supplies structural tracked content without creating a Markdown
  concept. Settle shared export/import/lint handling during refinement.
- **Open refinement questions:** How should already-bound notebooks acquire
  representation for omitted empty folders through an append-only update?
  When real content arrives, should the generated `.keep` be removed or retained?
  Define marker ownership and handling of an existing authored `.keep` without
  overwriting user content. These decisions belong here, not in a separate
  baseline-rewrite story.
- **Effort hypothesis:** L (2–4 hours), low confidence pending marker round-trip
  and existing-binding analysis. Reassess if this exceeds L; the owner selected
  the combined outcome, not an implementation plan.
- **Depends on / safe stopping point:** Existing clone, clean fast-forward pull,
  ordinary web note creation, and append-only web saves. No new-story technical
  prerequisite is established. Owners retain useful folder/new-note authoring
  even if accumulated publication and later organization stories are deferred.
- **Status:** Queued; needs refinement before execution planning. No
  implementation is authorized by this backlog addition.

<a id="story-20"></a>

### 20. Publish accumulated local commits without rewriting history

- **Slice plan:** [Publish accumulated local commits](../quick/122-publish-accumulated-local-commits/PLAN.md).
  Planning only; the deletion/recreation leaf awaits the identity-policy answer
  requested during planning. Remaining leaves cover the resolved composition
  outcome without requiring the full ADR implementation.
- **Refinement status:** Updated from the owner's 2026-09-14 scope decision.
  The earlier content-only proposal is replaced by composition of the existing
  local → Donut publication capabilities. Architecture below is a draft under
  discussion in [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md),
  not an approved ADR or authorization to plan/implement.
- **Goal:** A notebook owner can combine already supported local publication
  operations across accumulated commits and publish once. Donut shows the final
  notebook, preserves the original Git history, and retains the correct note and
  folder identities and their learning data. Any combination of those supported
  operations should work when the final state is valid and identity is resolved;
  neither an operation count nor an existing handler's inability to compose is
  a product reason to reject it.
- **Delivery boundary clarified by the owner:** Support multiple commits, each
  of which may itself mix multiple already supported edits. This is composition
  within commits and across commits, not delivery of the full ADR architecture
  or all possible histories. The earlier count of three described web → local
  actions; it is not a limit or an enumeration of local publication operations.
- **Current evidence:** CLI and backend ancestry guards allow only the same head
  or one direct child. Single-commit publication already supports content edits,
  note additions, note deletions, unchanged-content note renames/moves,
  represented unchanged folder relocations, and creation of folders/Readmes
  through added content. Clean local pull already receives several accepted
  commits. Code and test assertions were inspected; this refinement did not run
  the application test suites.
- **Why now:** The owner regards timing as reasonable. Preserve the backlog's
  web-trash detour and new web-folder story, then remove the need to publish
  after each local commit. Publishing after every operation is a workaround,
  but interrupts local sessions and does not solve already accumulated work.
  Keep performance validation and broader identity inference at their selected
  positions; no newly measured demand or performance claim is made.
- **Scope:** One bound notebook whose live projection matches accepted history;
  accepted main is an ancestor of local main. Compose the already supported
  operations, whether they affect different notes or successive states of the
  same note. Preserve genuine operation semantics, including permanent removal
  for a resolved deletion, but do not preserve incidental restrictions on mixing
  otherwise supported operations as new product rules. Existing Readme editing,
  newly inferred rename-with-content-change within one ambiguous transition,
  broader folder operations, and trash-specific compatibility are not silently
  added here. These are owned by their existing stories.
- **Intermediate revisions — owner's architecture proposal:** Preserve all
  original commits reachable through the accepted chain. A full clone receives
  that history. Only the proposed tip must be representable in Donut;
  intermediate drafts need not be valid Donut notebooks. Compute and apply the
  final result once, without replaying each revision through live database
  mutations. Inspecting intermediate paths/blobs for identity evidence is
  compatible with this boundary; invalid Markdown is not invalid Git.
  This is the architectural direction, not a promise to deliver every
  intermediate-draft case in this story. Only the parts needed to compose
  already supported edits are this story's delivery responsibility.
- **Composition and identity:** An unchanged-content rename in B followed by a
  content edit in C is in this story, even though the base-to-tip bytes differ.
  Use relevant intermediate evidence to compose supported identity transitions.
  The [research](../../docs/notebook-git-identity-research.md) demonstrates why
  endpoint matching alone misses such cases. Story 36 owns broader inference
  and ambiguity-resolution capabilities; it does not own all history inspection
  and is not an excuse to exclude combinations promised here. Ambiguous intent
  remains unresolved product input, not automatic permission to delete/create.
- **Proposed atomic acceptance:** Accept the full range with the final content,
  resolved identity outcomes, and required derived state, or leave Donut's
  accepted head and live state unchanged. Retain local work either way. A retry
  after a lost successful response recognizes the accepted tip without applying
  operations twice. Concurrent remote advancement fails clearly without
  overwriting either side. Exact crash/retry guarantees need outside-in proof.
- **Key examples:**
  1. B adds a folder represented by content and a note; C edits that note.
     Publish once: the final folder and note appear in Donut; a receiver gets
     the original A → B → C chain and final authored files.
  2. B renames or moves a learned note without changing its bytes; C edits its
     content. Publish once: the original note and learning history survive at
     the final path with C's content. Supported folder relocation followed by
     descendant editing follows the same composition promise.
  3. A session edits an existing note, adds another, and deletes an unrelated
     note across commits. Publication applies the final result together;
     resolved deletion uses existing permanent-removal semantics. Unrelated
     additions and deletions are not automatically ambiguous merely because
     endpoint classification currently groups them together.
  4. B edits two notes and adds a third; C edits that third note and renames
     another without changing its bytes. One publication accepts the mixed
     commits together, retaining the original chain and correct identities.
  5. Edits are fully undone by the tip. The new commits remain publishable even
     when final file bytes equal the base; identity-sensitive deletion/recreation
     requires the policy below rather than an automatic same-tree shortcut.
  6. A failed final validation accepts none of the range. A lost response after
     success permits a safe retry. A competing accepted web commit preserves
     both histories and prevents a stale publication.
- **Open decisions before execution planning:** Resolve only the application,
  acceptance, and identity decisions needed for this composition outcome;
  completion of the full ADR design is not a prerequisite. Define identity for a
  note deleted and recreated within an unpublished range, including at the same
  path and with identical content; distinguish temporary removal/undo from
  intended replacement. Define a safe response to unresolved correspondence
  without requiring history rewriting. Arbitrary ID-free histories cannot
  guarantee inferred intent, so “any combination” needs those semantics, not
  silent guessing. Preserve existing supported identity cases as evidence.
- **Deferred promises:** Independently advanced web/local history reconciliation,
  history browsing/restoration UI, per-commit application/progress/resume,
  unrelated live/history drift repair, new operation capabilities owned by
  sibling stories, and 10,000-note performance targets. No branching, merging,
  rebasing, or squashing is introduced by this story.
  General support for nonrepresentable intermediate drafts, new similarity-based
  identity inference, and owner-assisted ambiguity resolution remain broader
  architecture or later-story work. Deferral adds no requirement to reject
  naturally handled cases or validate every intermediate tree as a notebook.
- **Learning / sizing:** Evaluate a real combined editing session, including
  rename-then-edit, rather than a content-only demonstration. The former L
  estimate no longer establishes sizing for this broader outcome. Reassess
  after identity policies and architecture are settled; split delivery only
  without silently dropping the owner's combination goal.
- **Depends on / safe stopping point:** Reuse existing publication, clone, clean
  pull, and supported operation semantics. No later-story prerequisite is
  established for basic composition. This workflow remains independently useful
  if broader rename inference and later stories are cancelled.

<a id="story-36"></a>

### 36. Publish local renames, moves, and edits across commits while preserving note identity

- **Goal / beneficiary:** A notebook owner can rename or move notes while
  refining their contents locally, then publish the accumulated work into the
  original Donut notes, retaining their identity and learning history.
- **Human direction (2026-09-14):** Queue this story second from the bottom,
  immediately before publication performance validation. The current
  exact-byte matching behavior may remain useful, but is too specific to serve
  as the final solution for ordinary local rename-and-edit workflows. It may
  need to be replaced rather than surrounded with more special cases.
- **Current evidence / concern:** Publication currently infers a note move from
  uniquely paired removed/added files with identical blob bytes. Updating a
  heading, frontmatter, or body along with the filename breaks that match.
  Existing identity-preservation tests prove only the narrower behavior; they
  do not establish general rename-and-edit support. Actual file deletion now
  permanently removes the note and dependent learning data, so misclassifying
  a move as deletion plus addition would have a material preservation cost.
- **Scope direction:** Local → Donut within one notebook and the selected
  append-only history model. Cover content changes combined with note renaming
  or moving, including changes spread over multiple local commits. Preserve
  accepted and local commit IDs, final authored content, and the original note's
  learning associations. The owner should not need an artificial sequence of
  unchanged-content moves and intervening publications to preserve identity.
- **Key examples for refinement:**
  1. Rename `Old.md` to `New.md` and update its heading/body in the same commit.
     Publishing updates the original Donut note under its new title.
  2. Starting at accepted A, B edits `Old.md`, C moves/renames it to
     `Folder/New.md`, and D edits it again. One publication retains the same
     note and learning history, final D content, and the original commit chain.
     Intermediate revisions are potential identity evidence even when the
     accepted and final file contents differ.
  3. Several similar or identical notes change paths and contents. The product
     must not silently attach one note's learning history to another; the
     ambiguity outcome needs refinement. Example counts are not limits.
- **Approach concern:** Consider history across commits as evidence rather than
  relying only on the accepted-to-tip diff. This is an investigation direction,
  not a selected algorithm or a promise that every intent can be inferred.
  Exact bytes and similarity alone must not be mistaken for proof of user intent.
- **Strongest smaller alternative:** Rename/move without changing contents,
  publish, then edit and publish again. This can use the existing narrow path,
  but interrupts ordinary local editing and does not handle already accumulated
  rename-and-edit work without reconstructing it.
- **Highest learning / open decisions:** Establish which realistic histories
  provide sufficient identity evidence and what happens when they do not.
  Distinguish a move from intentional copying or deletion/recreation; determine
  whether unresolved cases need explicit owner confirmation or a clear refusal.
  Define destination-folder scope and interaction with repaired intermediate
  drafts. No similarity threshold, metadata scheme, identity-mapping UI, or
  per-commit replay design is selected by this story.
- **Boundaries / dependencies:** Story 20 owns combinations of already supported
  publication operations, including history analysis needed for an exact rename
  followed by an edit. This story owns broader inference for transitions whose
  correspondence those existing semantics cannot resolve, such as rename and
  content change together. Reuse story 20 rather than duplicating range analysis.
  Independent remote/local divergence and history rewriting remain outside
  this story. Broader folder-subtree operations remain unselected; story 28
  retains trash-specific journeys. Any shared identity inference should be
  cohesive rather than duplicated by story. Its position after story 28 is the
  owner's priority, not an established technical prerequisite.
- **Effort / status:** Queued, unrefined; sizing is unresolved until identity
  evidence and ambiguity policy are understood. Do not claim execution-ready
  scope or authorize implementation from this entry.
- **Safe stopping point:** Supported rename-and-edit journeys retain their
  original notes and learning history even if later organization work is
  deferred; unsupported ambiguity preserves work without silently guessing.

<a id="story-23"></a>

### Former story 23 — Receive a web note deletion locally

Merged into [Use portable trash across Donut and local Git](#story-28) by the
owner's web-first reprioritization. Its web-deletion synchronization outcome is
retained there; this anchor remains for existing references.

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

### Portable trash — parent problem (former stories 26 and 27)

For notebook owners, hidden deletion state and reserved old titles should become
visible, portable trash that preserves learning history and supports ordinary
file moves, with a simpler domain model and no loss of existing data.

#### North Star and completion boundary

Location owns trash membership; ordinary moves own entering and leaving trash.
Memory trackers follow their note's availability while retaining identity,
learning history, and independent tracking preferences. Restore is a shortcut
for a move, and permanent deletion removes the entity and dependent data.

The owner expects a cohesive solution, cleaner domain mapping, fewer concepts,
and a net reduction in product code after completion. Lines of code are a
supporting check, not a reason to compress code or remove meaningful proof.
Do not add an independent trash state, timestamp protocol, restoration journal,
or parallel reference resolver to implement these behaviors. Any proposed cache
needs measured justification and must be derived, not a second authority.

The selected sequence must reach story 31: all existing soft-deleted notes are
migrated, note `deleted_at` and its old state-management paths are removed, and
existing owners can use their retained data through trash. Temporary coexistence
before that boundary is unfinished work, not the final architecture. Story 31
owns its removal. The independent memory tracker deletion work is complete;
tracker activity is now derived from the owning note's deletion state.

The active query boundary uses a read-only SQL view of trash-folder ancestry,
shared by existing JPA/native query owners. It stores no membership state;
object checks use current ancestry after moves. Story 31 retains this
location-derived boundary while removing the legacy deletion state.

#### Continuous behavior preservation

Use gradual replacement in the spirit of the Strangler Application pattern.
Every completed story must keep all agreed preserved external behavior working
for both legacy and newly represented data, together with behavior delivered by
earlier stories. Prefer the same guarantee at every slice boundary. Never remove
a working behavior with a promise to reconstruct it in a later story.

Keep the existing path working until its replacement supports the required
behavior. In particular, legacy note recovery remains usable until migration;
web Trash preserves reference-handling choices; story 31 migrates data and
removes old machinery only with recovery, visibility, learning, and direct
access working through the replacement.
Minimal temporary compatibility is allowed for these boundaries and is owned
for removal by story 31. It must not become a second authority for new trash.

During refinement and slice planning, identify the preserved behaviors touched
by each replacement and carry their regression proof through that boundary.
If a proposed cut breaks continuity, change the cut rather than defer repair.
An explicitly changed behavior switches to its agreed replacement within the
same completed story. This seed specifies continuity obligations, not evidence
that implementation already satisfies them.

#### Human decisions and alternatives — 2026-09-13

- Beneficiaries are notebook owners using Donut and ordinary local Markdown/Git
  tools. Value now is reliable recovery and title reuse without duplicated state.
- Deferring retains invisible title reservations and the complicated deletion
  model. Merely allowing title reuse is smaller but leaves recovery ambiguous.
- The owner selected web-only feedback to drive the trash sequence. Local move
  and rename support is incomplete; building trash around those prerequisites
  would delay the useful web outcome. All trash/Git and local-file compatibility
  work stays together in lower-priority story 28, unsplit and unrefined.
- A timestamp or original-path record was considered and rejected. Root path
  membership and a prefix-removing Restore shortcut provide the chosen behavior.
- Highest-learning hypothesis: an owner can trash and recover a learned note
  through ordinary web navigation and Move, with one location rule governing
  visibility and participation. The web Trash/Undo capability established this
  without requiring a local checkout or new Git move/rename support.
- Direct SQL migration runs with the application release. The owner explicitly
  does not want a separate rollout plan, opt-in, placeholder gate, or migration
  approval ceremony. This overrides the default gated-DML guidance for this
  migration; it does not waive data-preservation proof.
- Subsequent owner clarification for story 31: inconsistent server Git history
  may be abandoned entirely. Build one fresh base from current migrated notebook
  data, with no old parent/history. Include permanent removal on accepted Git
  file deletion; moving to trash is the recoverable operation. This narrowly
  supersedes the earlier all-Git-work deferral and continuity obligation for
  those outcomes. Validate on isolated data here; tag and release later.

#### Agreed external behavior contract

**Preserve:** authorized direct access; note content and learning history;
independent removed-from-tracking preferences; ordinary editing and renaming;
ordinary move permissions and destination conflicts; deletion warnings and
reference choices (leave dead links, remove from referring properties with its
non-recovery warning, and eligible relationship reduction to a source property).
Keep authored outgoing references and the existing behavior that inactive
referrers are omitted from incoming-reference displays.

**Change:** trashing preserves the original path beneath notebook-root `_trash`;
root matching is case-insensitive and Donut creates lowercase `_trash`. Location
controls search, recall, assimilation, and wiki-link matching/ambiguity
eligibility. Trashed content stays in the Portable tree. Former active paths
become reusable by independent new notes. Leaving references during trashing
keeps their authored spelling, using deletion reference handling rather than
ordinary move rewriting. Actual file deletion permanently removes dependent
data; later recreation has a new identity.

**Add:** browse trash and directly open its notes/folders with a warning; Move
remains available with active destinations; Restore strips the leading trash
component and recursively creates missing parent folders. Trashing and moving
folders apply to descendants. A web trash collision suffixes the incoming
basename with the first available ` (2)`, ` (3)`, etc., before `.md` for notes.
A colliding incoming folder is suffixed as a whole rather than merged. Restore
retains that visible suffix and follows ordinary destination-conflict rules.
Editing or renaming in trash changes the path Restore subsequently uses.

**Remove at completion:** hidden soft-delete title reservations and implicit
restoration on title reuse; note soft-delete and timestamp-matching undo as
separate lifecycle mechanisms. No `trashed_at` is needed. The earlier suggestion
to retain deletion-date metadata is superseded by this decision.

#### Architecture and evidence

[ADR 0004 — Trash](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
owns the accepted format and eligibility rules. [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md)
keeps direct note URLs identity-based. [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
governs retained learning state. [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
is Proposed; consult its identity/acceptance discussion without treating its
older rebase or deletion-retention wording as binding.

Source inspection during this discussion found reference choices in
`useNoteDeleteFlow`, deletion/undo in `NoteService`, and inactive-referrer
filtering in `AuthoredNoteReferenceInboundFacade`. These are navigation evidence,
not a complete audit or executed proof. Existing web moves and folder navigation
are reuse hypotheses to validate in refinement. The owner identified incomplete
local move/rename support; it is not a foundation for the web trash stories.

### Selected web trash stories

Each story below is non-executable planning input. S = 30–60 minutes,
M = 1–2 hours, L = 2–4 hours. Estimates assume reuse of existing web moves,
reference handling, and navigation. Refine and split a web story found larger
than L without introducing a second mechanism. Story 28 is explicitly exempt
from further splitting/refinement for now, by the owner's instruction.

<a id="story-37"></a>

### 37. Release portable trash without risking notebook data

- **Goal / beneficiary:** Notebook owners keep every retained note, folder,
  reference, learning preference, and accepted Portable tree when the release
  converts legacy soft-deleted notes to location-based trash. The release
  operator can interrupt or retry the upgrade without leaving an unusable
  schema or allowing an old application instance to write against a partially
  upgraded database.
- **Why now:** The 2026-09-14 release-readiness audit found the trash behavior
  ready but blocked production release on migration and rollout safety. This
  story must complete before the owner releases the current trash work.
- **Scope capture:** Make the `V300000326`–`V300000328` upgrade interruption-safe,
  including the three-step `note.deleted_at` index/column change that currently
  fails on retry after the first MySQL DDL statement commits. Exclude concurrent
  old-application traffic and writes while legacy trash is converted and Git
  baselines are rebuilt, or introduce an equivalent expand/contract release
  sequence. Preserve Flyway history deliberately; changing an already committed
  but not production-applied migration requires an explicit human-owned exception
  to the normal immutability rule.
- **Evaluation:** Rehearse the complete release path against a populated copy of
  the pre-upgrade production schema. Interrupt at each non-transactional boundary,
  retry through the supported operator path, and compare retained row identities,
  content, folders, authored references, learning data, notebook bindings, and
  generated Git trees before and after. The normal production deployment must
  not overlap an old schema-dependent instance with the destructive contract step.
- **Boundary:** Include the backup, pre/post row-count checks, fresh-checkout
  communication, and partial-upgrade recovery instructions needed to make this
  release safe. Do not add later trash UI polish from story 38.
- **Provenance:** The release audit reproduced MySQL error 1091 when retrying
  `V300000328` after `idx_note_structural_peer` had already been dropped. The
  current rolling replacement keeps an old instance available while a new
  instance migrates the shared database, despite the upgrade test's stated need
  to exclude concurrent old-app writes.

#### Key examples

1. **Populated upgrade:** A pre-upgrade notebook contains active and legacy
   soft-deleted notes, nested folders, authored references, learning history and
   independent tracking preferences. The isolated upgrade puts retained deleted
   notes in location-based trash, preserves their identities and authored data,
   and produces a new accepted Git baseline containing the resulting Portable
   tree. Existing Git commit IDs/history are replaced by this release's baseline
   rebuild; preservation does not mean an unchanged old Git head.
2. **Interrupted upgrade:** The operator loses the upgrade process after a
   committed migration operation, including after the index drop or column drop
   in `V300000328`, or between notebook baseline rebuilds in `V300000327`.
   The supported recovery path completes without losing retained data or
   duplicating trash placement. Traffic stays excluded until verification passes.
3. **Release isolation:** An old application instance is serving before release.
   The release excludes its traffic and all schema-dependent writers before
   conversion begins; replacement and automatic restart cannot reintroduce old
   writers during the upgrade. Traffic resumes only with the compatible app and
   verified final schema/data. A failed upgrade leaves a recoverable maintenance
   state, not an automatic restart of the old binary against the changed schema.

#### Migration implications and evidence

- `V300000328__drop_note_deleted_at.sql`, committed in `2849e01a9c`, contains
  three separate `ALTER TABLE` statements: drop the structural-peer index,
  drop `deleted_at`, and recreate the index. A subsequent higher-numbered
  migration cannot repair a retry that fails before reaching it.
- Startup calls `flyway.repair()` before `flyway.migrate()`. Repairing migration
  history does not restore the already-dropped index or column and is not proof
  of data/schema recovery.
- `V300000326` delegates to a transaction covering legacy trash conversion;
  `V300000327` rebuilds one notebook per transaction and can replace completed
  baselines again on retry. These boundaries require rehearsal, but inspection
  has not established a need to edit those two committed migration classes.
- `NotebookUpgradeDataPreservationTest` creates its representative data after
  startup migrations and manually reruns the baseline rebuild. It supports
  rebuild preservation, not the complete populated pre-upgrade migration or
  interruption/recovery promise.
- Apply ADR 0004, **OKF-compatible notebook Markdown profile**
  (`docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md`): location-based
  trash retains ordinary Portable files and note-linked learning data. Apply
  ADR 0007, **Environments and isolation**
  (`docs/adrs/0007-environments-and-isolation-accepted.md`): rehearse on an
  explicitly isolated copy, not persistent Production or Development data.

#### Decisions and bounded follow-up

- Prefer a bounded maintenance release with backup, writer exclusion, verified
  upgrade/recovery, and fresh-checkout communication. An expand/contract rollout
  remains an alternative if a maintenance window is unacceptable; zero downtime
  is not an established promise of this story.
- On 2026-09-14 Terry Yin explicitly authorized revising the committed migration
  and confirmed that it has not been deployed to production. This is the
  human-owned exception to `.cursor/rules/db-migration.mdc` for `V300000328`;
  it is not general permission to rewrite migration history. Carry the exception
  into the implementation commit. Production state is owner-confirmed, not
  independently queried here; release preflight must still inspect actual history
  and schema, including other long-lived databases and any partially applied state.
- Rehearse 10,000 legacy soft-deleted notes, including nested paths, existing
  trash and occupied destinations, plus retained learning/reference data.
  Compare identities/content and complete final Portable trees; record elapsed
  time and resource observations. This is a representative one-time safety
  proof, not a permanent performance SLA or a general scalability project.
- Use the bounded maintenance-release approach above as the planning assumption
  following the owner's acceptance. Actual scheduling and release identity are
  resolved at release time. No zero-downtime subsystem is included.
- Temporary migration rehearsal code, fixtures, fault injection and release
  controls have a removal owner: [story 39](#story-39), triggered by verified
  production migration and successful release. Keep an exact inventory in the
  execution plan; do not retain a permanent migration framework or benchmark.
- **Executable plan:** [Safe portable-trash upgrade](../quick/123-safe-portable-trash-upgrade/PLAN.md).
- Story 38's UI/Undo cleanup and later Restore/Git journeys remain deferred.
  This refinement does not authorize executing migrations or deploying a release.

<a id="story-39"></a>

### 39. Retire the spent portable-trash upgrade after production success

- **Goal / beneficiary:** Maintainers can install and upgrade Donut without
  carrying conversion code, tests or release controls for a legacy state that
  production has already left behind.
- **Trigger / prerequisite:** Story 37's tested release has succeeded; production
  Flyway history, schema, retained-data checks and application smoke evidence
  confirm conversion. Verify other long-lived environments have crossed the
  upgrade before removing their path. A tag or green CI alone is insufficient.
- **Scope:** Completely remove the spent portable-trash migration files and
  exclusively migration-owned helpers, tests, fixtures, temporary fault injection,
  rehearsal commands and release controls inventoried by story 37. Retain shared
  runtime Portable-tree/Git functionality and ordinary trash behavior tests.
  Recheck callers before deleting shared helpers; do not retain dead code merely
  because it lives outside the versioned migration directory.
- **Safe removal:** Follow `.cursor/rules/db-migration.mdc`'s baseline/tip squash
  procedure: establish a version above every version ever applied, deploy and
  confirm that no-op tip on long-lived databases before removing older files,
  then fold final DDL into the existing baseline version. Freeze schema migrations
  during that sequence. This may need a follow-up deployment; do not delete
  files immediately on a success notification and break fresh installation.
- **Key examples:** An empty disposable database installs the final schema
  without the legacy conversion; an isolated copy at the confirmed tip starts
  after removal with all retained rows/bindings unchanged and sound Flyway history.
  Ordinary trash and Git product tests still pass; no migration-only callers,
  test entry points or release gates remain.
- **Boundary / safe stopping point:** No broad migration-framework rewrite or
  unrelated test cleanup. After closure, fresh installation and supported
  long-lived upgrades remain usable even if all later trash stories are cancelled.
- **Effort hypothesis:** M (1–2 hours active work), medium confidence; deployment
  and confirmation waiting is separate. Refine and plan when the trigger is met.

<a id="story-38"></a>

### 38. Make repeated trash and Undo journeys clean and predictable

- **Goal / beneficiary:** A notebook owner can repeatedly trash, undo, and
  re-trash related notes and folders without a failed nested recovery or a
  surprising collision suffix caused only by empty recovery scaffolding.
- **Why later:** Move already provides recovery and the main trash journeys are
  green. The owner explicitly placed this cleanup after the migration-safe
  production release.
- **Scope capture:** Repair the ignored child-then-parent trash/Undo journey;
  decide and implement coherent cleanup for empty mirrored trash folders left by
  note Undo so a later folder Trash is not unnecessarily renamed; and remove the
  Vue extraneous-attribute warnings emitted whenever trash confirmation modals
  open. Preserve real occupied-trash collision suffixing and ordinary Move
  recovery from ADR 0004.
- **Evaluation:** Trash a child note and its parent, then undo both and observe
  the complete active structure restored. Separately trash and undo a note in an
  otherwise empty mirrored path, then trash its original folder and observe the
  unsuffixed name when no retained trash content occupies it. Note and folder
  confirmations still support Cancel and confirmation and emit no browser
  warnings.
- **Boundary:** Do not add the Restore shortcut, bulk empty-trash behavior,
  permanent-delete UI, or change collision handling when retained trash content
  genuinely occupies the destination.
- **Provenance:** The 2026-09-14 audit confirmed the main menu journeys manually,
  found the nested Undo scenario still tagged `@ignore`, observed four identical
  modal warnings, and reproduced an empty `_trash/Research` folder causing a
  later active `Research` subtree to become `Research (2)`.

<a id="story-32"></a>

### 32. Restore a trashed item to its visible original path

#### Goal and value challenge

A notebook owner revisiting older trash can return one selected note or folder
to the location expressed by its current trash path, without selecting a
destination or manually rebuilding missing parent folders. This preserves the
owner's organization as well as the item's content, identity, and learning data.

Recovery already exists through Move. The incremental value is a predictable
shortcut, especially when parents are missing; it is not newly making recovery
possible. The strongest smaller alternative is to keep Move and manually create
parents, or recover to notebook root. Deferral is reasonable if that friction is
rare. Being next in the queue or completing the trash feature is not evidence of
urgency. The earlier owner decision favors web-first feedback, but frequency and
cost of this particular recovery problem remain unconfirmed.

The useful learning is whether the visible trash path gives owners the destination
they expect. The existing decision already excludes a restoration journal; proving
an internal simplification alone is not the user value of this story.

#### Scope — existing commitments

- A Restore action for one selected trashed note or folder in the web UI. For a
  folder, the selected subtree moves together; descendants retain their identities.
- Derive the destination in the same notebook by removing the leading trash
  component from the **current** path. Retain visible renames and collision
  suffixes. "Original path" does not mean a remembered historical location.
- Reuse existing destination parents and recursively create missing parent
  folders. Recreating a path does not recover an old parent folder's identity,
  description, or other contents.
- Apply ordinary destination-conflict rules and preserve authorization, content,
  note identity, learning history, and independent removed-from-tracking choices.
  Leaving trash restores location-based eligibility; it does not reset scheduling
  or promise that every recovered note is immediately due for recall.
- Preserve existing Move and immediate Undo behavior. Restore does not reconstruct
  reference properties removed when trashing; ordinary reference behavior remains
  the baseline, without a new link-repair promise.
- No new Git/local compatibility journey, folder Trash action, bulk selection,
  permanent deletion UI, empty-trash action, or historical-version recovery is
  committed here. Those deferrals do not justify restricting ordinary moves.

#### Key examples

1. **Missing organization:** After revisiting trash in a later session,
   `_trash/Biology/Cell science/Cells.md` exists and `Biology` does not. Restore
   recreates `Biology/Cell science` and places the same note at
   `Biology/Cell science/Cells.md`, retaining content and learning history.
   The owner explicitly requires recovery even when the entire active parent
   path is gone: recreate every missing ancestor, not just the immediate parent.
   For folder `_trash/Research/Biology/Topic`, with `Research` entirely absent,
   recreate `Research/Biology` and move the retained `Topic` subtree there.
   This recreates missing path folders; it does not resurrect their old metadata
   or unrelated contents.
2. **Existing organization:** The same destination parents already exist.
   Restore reuses them and leaves their other contents intact. A note directly
   beneath root `_trash` returns to notebook root.
3. **Current visible name:** A note is now at `_trash/Biology/Cells (2).md`,
   whether because of a trash collision or a rename. Restore targets
   `Biology/Cells (2).md`; it does not infer `Cells.md` from past state.
4. **One selected subtree:** Restore folder `_trash/Biology/Topic` to
   `Biology/Topic` with its descendants. Siblings left in trash remain there.
   This does not depend on adding a folder Trash button.
5. **Occupied destination — proposed interaction:** The derived destination
   conflicts with an existing item under ordinary placement rules. Show the
   conflict and leave the selected item in trash, without overwriting content
   or partially restoring a subtree. The owner can use existing Rename or Move.
   Whether Restore also offers the existing explicit folder merge choice remains
   open; automatic merge or automatic suffixing is not an agreed Restore behavior.

#### UI and assumption challenges

- Proposed wording is **Restore to …**, exposing the derived destination so a
  renamed item or retained suffix is unsurprising. Avoid promising recovery to a
  historical path. Exact presentation is not yet selected.
- Immediate Undo currently receives a prior folder and title; Restore derives
  its destination from today's trash path. They can legitimately differ. This
  story does not silently redefine Undo to make their results identical.
- Restoring a folder is useful even before folder Trash: trash already contains
  path folders and ordinary folder Move exists. Keep the current note-and-folder
  commitment unless the owner chooses to defer folder Restore; do not add folder
  creation/trashing workflows to justify this shortcut.
- The reserved root `_trash` itself has no item destination after prefix removal;
  treating it as Restore All would introduce a separate bulk outcome. Also resolve
  how to present a nested trash path whose derived destination is still beneath
  root `_trash`, rather than claiming it has left trash. These boundaries must
  respect the existing location rule, without inventing another trash state.

#### Open decisions and readiness

- **Priority decision (2026-09-14):** The owner explicitly moved Restore to the
  very bottom of the product backlog, after publication performance validation.
  Recursively reconstructing a completely missing active parent path remains
  required, but its incremental value over Move does not justify earlier priority.
  Restore is retained, not cancelled, and is not a prerequisite for folder Trash
  with Move recovery or the intervening Git work.
- **Narrow cut:** Retain notes and folders under one rule (recommended), or
  explicitly defer the existing folder Restore promise for a note-only delivery?
- **Conflict interaction:** Recommend reporting the conflict and relying on
  existing Rename/Move. Decide whether an explicit folder merge choice belongs
  inside Restore before making a delivery commitment to it.
- **Effort hypothesis:** M, low confidence. Missing parents and reference behavior
  remain sizing risks; this refinement is not an executable plan.
- **Prerequisites and stopping point:** Existing trash browsing and ordinary moves
  provide the starting journey. No new Git compatibility or folder Trash capability
  is required. The shortcut remains useful if those later stories are cancelled.

Focused source inspection on 2026-09-14 found existing older-trash Move examples
in `e2e_test/features/note_creation_and_update/note_deletion.feature`, prior-location
Undo in `NoteController`, and a default folder collision with optional explicit
merge in `NotebookController`. These are inspected examples and source behavior,
not newly executed test evidence. Proposals above await the owner's refinement
answers and do not supersede the shared agreed contract.

### Deferred combined compatibility story

<a id="story-28"></a>
<a id="story-30"></a>

### 28. Use portable trash across Donut and local Git

- **Goal / beneficiary:** Notebook owners can use the agreed trash lifecycle
  across the web and local files without losing content, identity, or learning
  history, and distinguish recoverable trash moves from permanent file deletion.
- **Scope capture:** All trash-related Git/local compatibility, including local
  trash/recovery moves, receiving web trash/restoration locally, migrated trash
  representation. Story 31 now owns the fresh Git baseline and permanent file
  removal/recreation semantics under the owner's subsequent scope decision;
  reuse that behavior here rather than reimplementing it. Former
  stories 28 and 30 and the web-deletion synchronization outcome of story 23
  are consolidated here; their anchors remain for traceability.
- **Evaluation direction:** An owner can continue the same trash/recovery journey
  between Donut and a local notebook with the agreed identity and data semantics.
- **Status / sizing:** Deliberately broad, unsplit, and unrefined at the owner's
  request. Likely larger than L; no execution-size or readiness claim is made.
- **Priority:** After the existing Git web-save, rename, accumulated-publication,
  Readme-edit, and web-move stories, before publication performance validation.
  Exact technical prerequisites remain unassessed until this story is selected.
- **Boundary:** This story owns new compatibility outcomes. Preserving already
  supported behavior during web restructuring remains each web story's duty;
  this backlog deferral does not authorize breaking it and repairing it later.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. The implemented
web Trash/Undo loop supplies the starting behavior; story 34 adds discovery of
older trash. The code for migrating and removing the old soft-delete structure
is complete, but story 37 owns making its first production application safe.
Neither relies on completing new Git move/rename or trash compatibility.

Folder Trash with ordinary Move now supplies the complete folder round trip.
The owner moved the Restore shortcut to the very bottom of the backlog, after
publication performance validation. Story 37 is the release-blocking correction;
story 38 deliberately follows the release because Move already provides a safe
recovery path for the remaining UI rough edges.

First, make the portable-trash migration and deployment safe for production
(story 37), then address the remaining repeated-trash and Undo rough edges
(story 38) after that release. Next, deliver web-created folders and their notes (story 35),
then keep remaining Git stories 20, 24, and 25 in relative order. Story 35's
position reflects the owner's 2026-09-14 addition of basic folder authoring;
stories 21 and 22 are already delivered. Then queue the single unrefined story 28. Former story 23 is
absorbed there because its deletion-sync scope overlaps the new trash lifecycle;
its outcome is retained rather than cancelled. Publication performance remains
after that combined compatibility item.

## Refinement Questions and Sizing Risks

- During story 37 refinement, inspect the live production migration boundary and
  release orchestration so the already implemented migration and retirement
  preserve supported behavior under interruption. Existing Git consistency must
  not regress, but no new Git compatibility acceptance journey is to be refined
  or split out of story 28. If an actual preservation conflict is discovered,
  surface it rather than silently expanding the web story or retaining old state.
- Existing user-authored root `_trash` content needs migration inspection so the
  new reserved meaning does not silently lose or overwrite existing content.
- Restore uses ordinary conflicts when a required parent path is a non-folder.
- Check simplicity cumulatively through story 31: fewer product-code lines and
  concepts, one state owner, and removal of duplicated rules. Preserve meaningful
  external proof even if test coverage increases total repository lines.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- Broader reconciliation of independently advanced local and remote histories,
  including multiple local commits, structural changes, and conflict recovery.
  Earlier proposals used ordinary Git rebase; the current direction excludes it.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Wider folder operations. Rename-with-content-edit and multi-commit note
  identity preservation are now queued together in [story 36](#story-36).
  All trash-related Git/local work is retained together in deferred story 28.
- Native standard Git transport, notebook binding within a project subdirectory,
  attachments, and history browsing or revision restoration.

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
The old proposal's rebase model and web-tip amendments are not constraints on
the newly selected append-only stories.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Story 20 owns composition of supported publication operations. Its final-only
  projection proposal and unresolved identity/deletion-gap semantics are drafted
  in ADR 0002; settle them before execution planning without discarding or
  rewriting work.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
