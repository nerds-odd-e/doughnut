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

#### Goal and why-now challenge

A notebook owner who creates a folder and ordinary notes on the web can pull
that material into the existing local notebook and continue editing there,
without recreating folders, copying notes, or adding artificial descriptions.
An empty folder must also arrive before its first note is written.

**Existing human direction (2026-09-14):** Keep web folder creation and creating
a note in that new folder together. Use `.keep` for otherwise empty folders;
include existing empty folders in a notebook's first Git snapshot. These are
retained commitments, not new proposals from this refinement.

**Owner clarification (2026-09-15):** All production notebooks will have their
initial bundles reset, so legacy omitted-folder recovery and compatibility with
those old bundles are not requirements for this story. This is the supplied
rollout assumption, not a claim that the reset has happened or an instruction
to perform it during refinement. Only empty folders have `.keep`; folders
represented by notes, a non-blank README, or tracked descendants do not.

The useful change is continuity of an existing authoring journey. Folder
creation already exists on the web; this story does not need a new folder UI.
The strongest smaller alternative is to author at notebook root or in an
already represented folder, postponing organization. A non-blank Readme can
retain a folder in a snapshot, but an artificial description is not an adequate
substitute for the explicitly requested empty folder. That snapshot workaround
also does not establish that subsequent web changes reach accepted history.

**Why now:** The selected journey closes a concrete gap between existing web
creation and local continuation. It retains value even if later folder moves,
trash compatibility, and broader publication work are cancelled. Being first
in the backlog, having delivered Folder Trash, or “completing basic authoring”
does not establish urgency. Frequency and actual owner cost remain unmeasured;
deferral is reasonable if owners are content with root/existing-folder authoring.
No dependency on Restore or trash cleanup justifies expanding this story.

#### Scope — narrow delivery

- In one notebook whose live content matches accepted Git history, create a
  folder on the web at root or beneath an existing folder, then create ordinary
  notes there. Receive the results through existing clone and clean
  fast-forward pull. Nested folders follow the same rule; example depth and
  note counts are not product limits.
- Owners may pull while the folder is empty, or after creating its notes,
  without an intermediate synchronization step. Preserve the folder path and
  authored note content in both cases.
- The first Git snapshot of an unbound notebook retains existing empty folders,
  including nested empty paths, using `.keep`. This stays with the selected
  outcome under the explicit owner decision. The production bundle reset is
  the supplied rollout prerequisite; this refinement does not execute it.
- Use `.keep` as structural tracked content, never as a Donut note or a generated
  Readme description. Generate it only for an otherwise empty folder: no
  notes, non-blank README, or child folders. A nested empty leaf gets `.keep`;
  its ancestors are represented by that tracked descendant. Remove the generated
  marker when content or a child folder represents the directory. A blank
  Readme still emits no README and does not make the folder nonempty.
  Export, import, and lint must agree on this representation. A received tree must support the next already-supported local
  note edit and publish; this is necessary continuity, not a new local folder
  authoring feature.
- Preserve existing note/folder identities, learning data, authored content,
  authorization, and, after the planned baseline reset, accepted/local commit
  IDs. Subsequent web changes append history without amending earlier commits.
  The planned production reset is distinct from ordinary synchronization.

#### Scope challenges and deferred promises

- **Legacy omitted folders:** No backfill, legacy snapshot comparison, or old
  bundle compatibility machinery is required under the owner's production-reset
  assumption. Do not carry the previous hypothetical recovery concern into
  execution planning.
- **Marker lifecycle:** Empty-only generation and removal of a generated marker
  when the folder becomes represented are required behavior, not optional
  cleanup. Do not add a provenance journal or marker-management UI. Deliberate
  local marker editing/removal, authored nonempty `.keep` files, and arbitrary
  non-Markdown-file support are not delivery promises; this does not authorize
  silently overwriting or discarding authored content.
- **Local omission:** Publishing a tree that omits a folder's last tracked
  representation dissolves that Folder; Donut does not retain invisible
  server-only folders or rewrite the owner's commit.
- **Keep separate:** Other folder rename/move/dissolve behavior, trash synchronization (story
  28), local creation/publication of new empty folders, Readme editing (story
  24), relationship/Wikidata creation journeys, dirty/divergent checkout
  recovery, repair of unrelated live/history drift, and performance targets.
  These are deferred delivery promises, not new rejection rules for naturally
  supported cases. No exhaustive folder-operation compatibility matrix is
  required merely because the tree representation is shared.

#### Key examples

1. **Empty, then populated:** Starting with a synchronized notebook, create
   `Biology` on the web and pull into its clean checkout. `Biology/.keep` exists
   locally without becoming a note or Readme. Create ordinary note `Cells` in
   that folder on the web and pull again. `Biology/Cells.md` contains its
   authored content and `Biology/.keep` is gone; earlier commits remain unchanged.
2. **No intervening pull:** From the same starting state, create nested folders
   `Science/Biology` and note `Cells` on the web before pulling. One pull
   receives `Science/Biology/Cells.md` and the hierarchy without manual repair.
   Neither folder contains `.keep`, because the note represents both paths.
3. **First snapshot:** An unbound notebook already contains empty nested folder
   `Science/Biology`, alongside existing notes and non-blank Readmes. First
   clone includes `Science/Biology/.keep`, with no `Science/.keep`, and preserves
   the existing content. A folder with a non-blank README has no `.keep`; a
   folder with only a blank Readme and no children or notes does. This applies
   equally to the fresh snapshots after the planned production reset.
4. **Continue locally:** After receiving the new folder/note, edit `Cells.md`
   locally, commit, and publish through the existing workflow. Donut shows the
   change on the same note, retaining its learning associations. Structural
   markers neither become concepts nor prevent this ordinary continuation.

#### Evidence, constraints, and readiness

Focused source inspection on 2026-09-15 found that `PortableTreeSnapshot`
emits note files and non-blank Readmes without empty-folder markers;
`NotebookController.createFolder` uses the existing folder construction path;
`WebNoteCreationService` advances history only for a matching projection and
represented destination. `NotebookGitProjection` compares the full generated
snapshot with the accepted tree. The owner's subsequent production-reset
clarification removes the older-binding compatibility concern from this story.
Markdown validation skips non-Markdown files, while publication has additional
shape admission checks: skipping `.keep` in Markdown validation alone does not
establish round-trip support. These are source observations, not a newly run
failure reproduction or acceptance test.

[ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
is Accepted: empty folders use canonical structural `.keep` files, blank
Readmes are omitted, and export/import/lint share a lossless codec contract.
Proposed ADR 0002 records that omission of a folder's last represented path
dissolves its server projection.

- **Resolved scope and representation:** No legacy recovery; `.keep` only in
  otherwise empty folders, removed when tracked content represents the folder;
  publishing its final represented path's omission dissolves the Folder.
- **Highest learning:** Can an owner complete web folder → web note → local
  receipt → ordinary local edit/publish without a workaround or broken history?
- **Effort hypothesis:** Retain L (2–4 hours), low confidence pending assessment
  of empty-only marker round-tripping and web history updates. Legacy bundle
  compatibility no longer contributes scope. Reassess during planning.
- **Plan:** [Receive web-created folders](../quick/126-receive-web-created-folders/PLAN.md).
  Execution is authorized. The owner resolved the baseline concern: an
  unrepresented folder dissolves without rewriting the commit. Existing clone,
  pull, and ordinary note authoring supply the prerequisites; no other
  queued story is an established dependency.

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
- **Boundaries / dependencies:** Accumulated publication already composes
  supported operations across a linear range, including history analysis for an
  exact rename followed by an edit. This story owns broader inference for
  transitions whose correspondence those existing semantics cannot resolve,
  such as rename and content change together. Reuse that delivered range
  composition rather than duplicating it. Independent remote/local divergence
  and history rewriting remain outside this story. Broader folder-subtree
  operations remain unselected; story 28 retains trash-specific journeys. Any
  shared identity inference should be cohesive rather than duplicated by story.
  Its position after story 28 is the owner's priority, not an established
  technical prerequisite.
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
- The Flyway migration chain runs automatically with ordinary application
  startup/release: `V300000326` and `V300000327` are Java Flyway migrations,
  while `V300000328` is SQL. The owner explicitly does not want a separate
  rollout plan, opt-in, placeholder gate, manual migration gate, or approval
  ceremony. This overrides the default gated-DML guidance for this migration;
  it does not waive data-preservation proof.
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

<a id="story-38"></a>

### 38. Make repeated trash and Undo journeys clean and predictable

#### Goal and why-now challenge

**Recommended narrow outcome (2026-09-15 refinement):** A notebook owner can
trash a note, immediately undo that action, and later trash its containing
folder without gaining a collision suffix solely because the first operation
left an unused mirrored path. The visible result is a predictable folder name
in trash. This is not a promise to make every trash/recovery journey clean.

The 2026-09-14 audit recorded an empty `_trash/Research` causing a later
`Research` subtree to become `Research (2)`. This is evidence of confusing
organization, not lost content or unavailable recovery. Ordinary Move already
provides recovery. Doing nothing is safe under the existing collision rule;
accepting the suffix is the strongest smaller alternative. Its cost is an
unexpected name which the owner must understand or change, not a broken round
trip.

The owner previously placed these rough edges after the migration-safe release;
this item is now first in the backlog. That ordering does not establish urgency.
Fixing a reproduced surprise in a recently delivered journey is a reasonable
reason to act now, but frequency and actual owner cost are unmeasured. If this
rare naming inconvenience takes substantial lifecycle machinery to resolve,
deferral in favor of web-created folders (story 35) is preferable. No technical
dependency makes this correction a prerequisite for that story or Git work.

#### Scope — recommended cut

- Own the web note Trash → immediate Undo → containing-folder Trash journey
  above, in one notebook with available Undo history and no intervening authored
  changes to the mirrored trash path. Existing note Undo and folder Trash are
  the entry points; no additional controls are needed.
- A path introduced solely to hold the undone note must not by itself force a
  suffix on that later folder Trash. Specify this observable outcome, not a
  general empty-folder deletion policy or a prescribed cleanup mechanism.
- Preserve content, folder and note identities belonging to the owner, learning
  history, permissions, existing reference choices, and ordinary Move recovery.
  Undo does not promise to recover reference properties deliberately removed at
  trash time. Location continues to govern trash eligibility.
- Preserve occupied-destination suffixing of the incoming subtree as a whole
  under [ADR 0004 — Trash](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash).
  Do not merge into, overwrite, or discard earlier trash to obtain a nicer name.
  An intentionally retained empty folder is still an item; a folder with a
  Readme is not disposable merely because it contains no notes.
- Limit the delivery commitment to the operation's leftover path. No scan or
  cleanup of old trash, new provenance journal, folder Undo capability,
  cross-session Undo, arbitrary recovery order, Restore shortcut (story 32),
  permanent-delete UI, or new Git/local compatibility (story 28) is promised.
  These are deferrals, not rules rejecting naturally supported behavior.

#### Key examples

1. **Unused path:** Active `Research/Cells.md` exists and `_trash/Research`
   does not. Trash `Cells`, immediately Undo, then trash folder `Research`.
   The same folder and note appear at `_trash/Research/Cells.md`, without
   `Research (2)` caused by the first operation's leftover path. The recovered
   note retains its content and learning history.
2. **Earlier trash remains:** `_trash/Research/Older.md` already exists. Trash
   and undo active `Research/Cells.md`, then trash active `Research`. Earlier
   trash stays intact; the incoming folder receives the first available suffix
   under the existing rule. The correction must not reinterpret this as an
   empty destination or merge the two folders.
3. **Empty does not mean disposable:** `_trash/Research` was already an
   intentionally retained empty folder, or contains an authored Readme. The
   same journey preserves it and applies ordinary collision handling. Counting
   notes alone cannot establish that a path is disposable scaffolding.

#### Other captured work — retained for explicit deferral

- **Ignored nested Undo example:** The earlier scope promised repair of the
  child-then-parent journey. Source inspection on 2026-09-15 found the ignored
  scenario in `e2e_test/features/note_creation_and_update/note_deletion.feature`
  trashes notes `TDD` and `tech`, then undoes them in reverse order. It does not
  trash the containing folder. A nearby active scenario explicitly preserves
  structural descendants when a note is trashed. An ignored test is not proof
  of a current product failure; confirm its intended observable behavior before
  retaining a repair commitment. Recommend deferring this separate outcome;
  do not invent folder Undo or revive timestamp-based restoration to satisfy it.
- **Modal warnings:** The audit recorded four identical Vue extraneous-attribute
  warnings. Removing those warnings is independent of the naming outcome.
  Recommend deferring it unless it prevents the selected journey from working.
  Preserve working Cancel and confirmation behavior, without adding a
  browser-wide no-warnings acceptance commitment.

These earlier promises remain recorded here; the recommended cut does not
silently cancel them, queue new stories, or change sibling scope or backlog order.

#### Open decisions and readiness

- **Scope proposal:** Recommend selecting only the spurious-suffix outcome and
  deferring the other two captured outcomes. This is a refinement recommendation,
  not a claim of an already confirmed owner decision.
- **Safety assumption:** The original capture assumed empty mirrored paths can
  be cleaned safely. Inspection found path creation can reuse existing folders,
  while Undo currently carries a prior folder and title. It has not established
  how to distinguish disposable scaffolding from intentional retained folders.
  Resolve that distinction before execution planning; if the narrow journey
  cannot be supported safely without broader state, revisit its value rather
  than silently widen the story or weaken preservation.
- **Evidence:** The reproduction and warnings above come from the retained
  2026-09-14 audit. This refinement inspected current source and scenarios but
  did not rerun the application or establish a failing test. Confirm the defect
  at the current revision before implementation; if already absent, reassess
  remaining work rather than manufacture a repair.
- **Effort hypothesis:** S–M, low confidence, assuming a local correction to the
  existing journey. Safe ownership of leftover paths is the main uncertainty.
  The outcome remains useful if all later trash work is cancelled. No executable
  plan or implementation is authorized by this refinement.

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
- **Priority:** After the existing Git web-save, rename, Readme-edit, and
  web-move stories, before publication performance validation. Accumulated
  linear publication of already supported local edits is already delivered.
  Exact technical prerequisites remain unassessed until this story is selected.
- **Boundary:** This story owns new compatibility outcomes. Preserving already
  supported behavior during web restructuring remains each web story's duty;
  this backlog deferral does not authorize breaking it and repairing it later.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. The implemented
web Trash/Undo loop supplies the starting behavior; story 34 adds discovery of
older trash. The code for migrating and removing the old soft-delete structure
is release-ready and runs through normal Flyway startup. Story 39 owns removing
its temporary migration support after production and other long-lived databases
have crossed it. Neither relies on completing new Git move/rename or trash
compatibility.

Folder Trash with ordinary Move now supplies the complete folder round trip.
The owner moved the Restore shortcut to the very bottom of the backlog, after
publication performance validation. Story 39 is the first post-release cleanup;
story 38 follows because Move already provides a safe recovery path for the
remaining UI rough edges.

After the production release, retire the spent migration support (story 39),
then address the remaining repeated-trash and Undo rough edges (story 38). Next,
deliver web-created folders and their notes (story 35), then keep remaining Git
stories 20, 24, and 25 in relative order. Story 35's position reflects the
owner's 2026-09-14 addition of basic folder authoring; stories 21 and 22 are
already delivered. Then queue the single unrefined story 28. Former story 23 is
absorbed there because its deletion-sync scope overlaps the new trash lifecycle;
its outcome is retained rather than cancelled. Publication performance remains
after that combined compatibility item.

## Refinement Questions and Sizing Risks

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
- Broader identity admission beyond already supported exact correspondence,
  including same-transition rename-with-edit inference, remains open in
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  and queued story work; confirmed deletion/recreation already starts a new
  identity.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
