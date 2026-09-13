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

<a id="story-34"></a>

### 34. Find and recover previously trashed notes through web navigation

Plan: [Web trash navigation and recovery](../quick/117-web-trash-navigation-recovery/PLAN.md)

- **Goal:** A notebook owner using the web can find a previously trashed note
  after immediate Undo is gone and recover it through Move, without knowing
  its URL. This makes the existing trash capability useful across sessions.
- **Status:** Refined on 2026-09-13 for the owner's request for narrow scope and
  early working feedback. Slice plan linked above; implementation has not started.

#### Scope

- Deliver one journey using notes already represented beneath notebook-root
  `_trash`: discover that folder through notebook navigation, browse its nested
  folders, open a note with its content and trash warning, and use existing Move
  to put it at notebook root or in an existing active folder in the same notebook.
- Reuse ordinary notebook/folder navigation and note views. Make trash status
  clear when opening the trash root, a folder beneath it, or a trashed note.
  An accessible `_trash` entry in the existing notebook tree is sufficient;
  no separate trash dashboard or navigation redesign is required.
- Recovery moves the existing note, preserving its identity, content, learning
  history, and independent tracking preferences. Its warning disappears and
  normal location-based eligibility resumes. Recovery does not recreate
  references deliberately removed during trashing.
- Preserve ordinary Move authorization and destination-conflict behavior,
  existing Trash/Undo, authorized direct access, and legacy deleted-note
  recovery. Trash browsing must not make trashed notes active in search,
  learning, or wiki-link matching. These are continuity constraints from the
  parent contract, not new lifecycle features.
- **Deferred promises:** Legacy soft-delete migration (story 31), automatic
  original-path Restore or missing-parent creation (story 32), folder Trash
  (story 33), and Git/local compatibility (story 28). Also defer bulk recovery,
  trash search/sorting/filtering, trash counts, permanent-delete/empty-trash UI,
  and a new cross-notebook recovery journey. These omissions do not require
  rejection of cases ordinary navigation or Move already supports.

#### Key examples

1. **Find and recover after leaving:** `Biology/Cells` was trashed into
   `_trash/Biology/Cells`; the owner has left the page and no Undo is available.
   Starting at the notebook, they browse `_trash` then `Biology`, see that the
   folder is in trash, and open `Cells` with its content and trash warning.
   They Move it to existing active `Biology`; it appears there without the
   warning, with the same identity and retained learning history/preferences.
2. **Original parent absent:** `_trash/Biology/Cells` exists but active `Biology`
   does not. The owner can Move `Cells` to notebook root using the existing
   destination control. This journey needs no parent reconstruction or Restore.
3. **Destination occupied:** An independent active `Biology/Cells` already
   exists. Attempting recovery there follows ordinary Move conflict behavior
   without overwriting either note; the owner can choose a free destination.
4. **Nothing to recover:** A notebook without a trash folder remains navigable;
   this story need not create one merely to display an empty destination.
   An existing empty trash folder uses ordinary empty-folder navigation.

- **First feedback:** Have an owner start from a notebook, find a previously
  trashed nested note, and recover it through Move without a saved URL or Undo.
  Observe whether the tree entry and existing Move interaction are discoverable
  enough before committing to additional recovery UI.
- **Effort hypothesis:** S–M (30 minutes–2 hours), medium confidence, assuming
  reuse of existing listing, warning, and Move behavior. Focused inspection
  found root/child folder listings and a note trash warning already present;
  the folder page lacks a trash warning. This is reuse evidence, not an executed
  demonstration that the whole journey works. If delivery exceeds this band,
  revisit the boundary before adding navigation or recovery mechanisms.
- **Depends on / safe stopping point:** Existing location-based web trash and
  Move; no migration or new Git prerequisite. Owners retain usable recovery
  through navigation even if later convenience stories are deferred.
- **Open decisions:** None blocking this bounded journey. Exact warning wording
  and entry presentation can follow existing UI conventions; richer recovery
  UI should be driven by the first feedback.

<a id="story-31"></a>

### 31. Recover existing deleted notes through portable trash

- **For / why:** Existing web owners find previously deleted content in the same
  trash as newly trashed notes, with history intact and former paths reusable.
- **Evaluation:** Upgrade a notebook with soft-deleted learned notes; browse them
  under their trash paths, recover with Move, and reuse their former names for
  new notes. Existing note URLs, active content, and dependent data remain usable.
- **Scope:** Automatic SQL data/schema migration with release; preserve IDs,
  content, and dependent records using agreed placement/collision behavior.
  Retire note `deleted_at`, its old writes/filters/undo mechanisms, and hidden
  title-conflict restoration. Adapt existing callers as necessary to preserve
  their currently supported external behavior using the new representation.
  This is part of safe replacement, not a new Git feature or permanent-delete
  contract. Location is the remaining authority for note trash availability.
- **Value / learning:** Completes structural retirement with a directly visible
  recovery benefit for existing owners; avoids permanent compatibility machinery.
- **Effort hypothesis:** L, low confidence, particularly migration and existing
  callers. Refine further if necessary; do not turn Git enhancement into a
  prerequisite or promise unsupported behavior to make retirement appear complete.
- **Depends on:** Existing web trash and preserved Move recovery. The deferred
  Git compatibility story is not a prerequisite.
- **Safe stopping point:** All legacy deleted data is migrated and the old note
  soft-delete structure is gone, with existing behaviors preserved. This remains
  the required completion boundary even if later convenience work is deferred.

<a id="story-32"></a>

### 32. Restore a trashed item to its visible original path

- **For / why:** A web owner can put a note or folder back without choosing its
  destination and reconstructing the parent folders manually.
- **Evaluation:** Restore `_trash/Biology/Cells (2)` while `Biology` is absent;
  create the parent and move to `Biology/Cells (2)` with retained identity/history.
  The same prefix-removing action restores a folder and its descendants.
- **Scope:** Restore toolbar action on trashed notes/folders; reuse existing
  parents, recursively create missing ones, and apply ordinary destination
  conflicts. Renames in trash change the visible path used for restoration.
- **Value / learning:** A small web convenience proves recovery needs no original
  path journal, timestamp grouping, or separate folder-undo concept.
- **Effort hypothesis:** M, low confidence around missing parents and references.
- **Depends on:** Existing web note Trash. Story 31 is higher priority for
  structural completion, not a technical prerequisite. No new Git compatibility
  promise is included.
- **Safe stopping point:** Restore is useful independently of a folder Trash
  action, permanent deletion UI, or empty-trash capability.

<a id="story-33"></a>

### 33. Trash a folder on the web as one recoverable subtree

- **For / why:** A web owner can set aside an organized body of notes in one
  action without mixing it into an earlier trash operation.
- **Evaluation:** With `_trash/Biology` present, trash active `Biology`; place the
  whole subtree in `_trash/Biology (2)`. Browse and move it out with every note's
  identity/history retained and participation following location.
- **Scope:** Folder Trash action, recursive eligibility, full-path placement and
  whole-folder collision suffixing. Preserve the existing trash subtree and
  ordinary editing/Move. Use Restore when available from story 32.
- **Value / learning:** Extends the demonstrated web move model to a subtree
  without inventing timestamp-based group recovery.
- **Effort hypothesis:** M, low confidence, assuming reuse of existing subtree
  moves and the common eligibility rules.
- **Depends on:** Existing web note Trash. Story 32 supplies convenience, not
  required recovery. No new Git compatibility promise is included.
- **Safe stopping point:** The folder workflow is useful without web permanent
  deletion, automatic expiry, or empty-trash actions.

### Deferred combined compatibility story

<a id="story-28"></a>
<a id="story-30"></a>

### 28. Use portable trash across Donut and local Git

- **Goal / beneficiary:** Notebook owners can use the agreed trash lifecycle
  across the web and local files without losing content, identity, or learning
  history, and distinguish recoverable trash moves from permanent file deletion.
- **Scope capture:** All trash-related Git/local compatibility, including local
  trash/recovery moves, receiving web trash/restoration locally, migrated trash
  representation, and permanent file removal/recreation semantics. Former
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
older trash; story 31 completes migration and removal
of the old structure before convenience
expansion. Neither relies on completing new Git move/rename or trash compatibility.

Story 31 is required completion work, not optional cleanup. If work pauses before
it, keep the old behavior usable and its retirement visibly unfinished. Defer
folder Trash first, then the Restore shortcut; Move already supplies recovery.
No database-, API-, or UI-only preparation story is queued.

After those web stories, keep existing Git stories 21, 22, 20, 24, and 25 in
relative order. Then queue the single unrefined story 28. Former story 23 is
absorbed there because its deletion-sync scope overlaps the new trash lifecycle;
its outcome is retained rather than cancelled. Publication performance remains
after that combined compatibility item.

## Refinement Questions and Sizing Risks

- During story 31 refinement, inspect current persistence and callers to ensure
  migration and retirement preserve supported behavior. Existing Git consistency
  must not regress, but no new Git compatibility acceptance journey is to be
  refined or split out of story 28 now. If an actual preservation conflict is
  discovered, surface the concrete conflict rather than silently expanding
  the web story or keeping old state indefinitely. No deployment plan is requested.
- Existing user-authored root `_trash` content needs migration inspection so the
  new reserved meaning does not silently lose or overwrite existing content.
- Restore uses ordinary conflicts when a required parent path is a non-folder.
- Folder deletion-reference warnings have no existing folder-delete flow to copy;
  refine the application of existing choices before story 33, without inventing
  automatic reference removal.
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
- Wider folder operations and rename-with-content-edit identity decisions.
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
- Story 20 owns the proposed atomic publication policy and unresolved
  intermediate-history semantics; settle those decisions before its execution
  planning, without discarding or rewriting work.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
