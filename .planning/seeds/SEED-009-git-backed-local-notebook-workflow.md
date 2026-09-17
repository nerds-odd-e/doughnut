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

<a id="story-42"></a>

### 42. Manually validate the complete append-only notebook workflow

- **Goal / beneficiary:** A notebook owner and product owner have current,
  externally observed evidence that one Git-backed notebook can move between
  Donut and ordinary local tools without copying work, rewriting accepted
  or local notebook history, or separating a note from its identity and
  learning history.
- **Why now:** The individual append-only capabilities have been delivered and
  proved automatically, but the complete owner journey has not received one
  bounded manual evaluation against the current product. Relying only on the
  component and E2E evidence is the strongest smaller alternative; it does not
  assess the joined browser, installed CLI, local Git, guidance, and refusal
  experience as an owner encounters it.
- **Scope:** Spend at most one hour preparing and manually observing the second
  paragraph of the backlog's near-future direction in one isolated E2E
  environment. Use the browser, installed CLI, and ordinary Git from the
  Story Branch Mode execution worktree. Cover both linear lag directions: a
  clean local checkout receives several accepted web commits by fast-forward,
  and the remote accepts several local commits as their original contiguous
  chain. Exercise representative content editing, rename or movement with
  content change, stable note identity, and retained learning state. Observe
  the product's owner-facing guidance as part of the journey.
- **Append-only boundary:** Independently advanced local and accepted histories
  are not reconciled. With unpublished local work and a newer accepted head,
  pull must refuse clearly while leaving the local head, files, index, and
  accepted history unchanged. No rebase, merge-based reconciliation, replayed
  replacement commit, or conflict-resolution journey is accepted behavior.
  The existing source audit predicts contrary rebase/replay behavior; manual
  execution must observe the product rather than treat that audit as the test
  result.
- **Key examples:**
  1. From a clean clone, make two local commits that rename or move and edit one
     learned note, publish once, and receive the result in another clean clone.
     Donut retains the original note route and learning state, the receiver has
     the exact final bytes, and the original local commits remain the accepted
     ancestor chain.
  2. From a clean clone, make several accepted web changes to that notebook,
     including an ordinary edit and rename or move, then pull once. The checkout
     fast-forwards through every accepted commit to the final paths and bytes
     without a second copy or rewritten commit.
  3. From a clean clone, commit unpublished local work and independently append
     a web change. Pull refuses without changing either history or starting a
     Git operation; the owner keeps both bodies of work and receives guidance
     consistent with the append-only boundary.
- **Manual-testing contract:** Apply `dough-manual-testing` for the one-hour
  coverage, evidence, and report policy, and the repository `manual-testing`
  skill for browser operation. Breadth comes before depth. Reuse existing
  setup and automated evidence where it is sufficient; do not spend the hour
  replaying a deterministic matrix. If the planned observations finish with no
  actionable finding or material uncertainty, report `Good.`; otherwise report
  only discrepancies, unresolved expectations, improvements, and material
  coverage gaps with evidence. Do not diagnose or repair findings during this
  story.
- **Time budget:** 60 minutes total from manual environment preparation through
  the observation report: 10 minutes preparation, 25 minutes breadth across the
  two linear directions and divergence refusal, 15 minutes selective depth on
  identity/learning/history and any surprise, and 10 minutes confirmation and
  reporting reserve. Stop at the budget and report unobserved promises as
  coverage gaps rather than extending the session.
- **Boundaries:** This is manual evaluation, not another automated acceptance
  suite, performance validation, a rebase/merge implementation, projection-
  drift recovery, cross-notebook synchronization, native Git transport, or a
  repair story. It does not cover the independent incoming-reference defect or
  the 10,000-note performance story. Temporary setup artifacts must remain
  isolated and be removed; findings do not authorize product changes.
- **Execution:** Run the plan through `dough-execute-plan` in its default Story
  Branch Mode (no `--trunk` and no current-branch override), so the queue claim,
  isolated worktree, observation, and delivery evidence follow the normal
  planned-execution lifecycle. [Slice plan](../quick/131-manually-validate-append-only-notebook-workflow/PLAN.md).
- **Effort / status:** Refined, planned, and queued. One 60-minute manual
  observation slice; environment startup and coverage selection are bounded by
  the same testing budget. No product-scope decision remains open.
- **Depends on / safe stopping point:** The delivered append-only Git-backed
  workflow and its existing testability/CLI setup. The report remains useful
  even when it finds discrepancies or later feature work is cancelled; it must
  not claim unobserved behavior.

<a id="story-23"></a>

### Former story 23 — Receive a web note deletion locally

Merged into [Use portable trash across Donut and local Git](#story-28) by the
owner's web-first reprioritization. Its web-deletion synchronization outcome is
retained there; this anchor remains for existing references.

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
than L without introducing a second mechanism. Git/local trash-boundary
moves are existing product behavior under
[former stories 28 and 30](#story-28);
the earlier web-story selection does not authorize additional trash features.

<a id="story-38"></a>

### 38. Make repeated trash journeys clean and predictable

#### Goal and human direction — 2026-09-16

A notebook owner should be able to repeat web trash operations without confusing
organization or losing retained work. “Clean and predictable” is a provisional
label: refinement must identify a concrete unwanted result and its desired
replacement before this becomes an executable story.

The owner moved publication-scale validation to the bottom of the backlog,
initially leaving this story first, and explicitly postponed all Undo work. Undo is removed
from this story's goal, scope, and acceptance examples. This defers improvements
and repairs; it does not request removal of already-working product behavior.

#### Backlog decision — 2026-09-16

Removed from the active backlog at the owner's request; retained here for future
reconsideration. The clarified journey is trash a note, create a new note with
the same title at the original location, and trash the replacement without Undo.
Current implementation retains distinct notes and gives the incoming note the
first available numbered suffix. The existing controller test
`repeatedTrashAfterNameReuseKeepsDistinctNotesAndUsesTheFirstFreeTrashTitle` in
`backend/src/test/java/com/odde/donut/controllers/NoteControllerTrashTests.java`
asserts distinct identities, the same trash folder, and titles `Reusable` and
`Reusable (2)`. Source and test were inspected, not rerun.

This journey provides no demonstrated missing behavior or information-loss
problem requiring the story now. With all new Undo work postponed, resurface
only when a concrete non-Undo problem and worthwhile correction are identified.
The probes below remain unselected refinement possibilities, not queued work.

#### Purpose and why-now challenge

The previous narrow example was note Trash → immediate Undo → containing-folder
Trash, where an empty mirrored `_trash/Research` led to `Research (2)`. The
2026-09-14 audit recorded confusing naming, not lost content or unavailable
recovery. With Undo postponed, that example no longer justifies this delivery.
There is no demonstrated non-Undo defect in the retained evidence.

The near-future direction prioritizes portable trash, so this story aligns with
that direction. Alignment and queue position alone do not establish urgency.
Act now only if an owner encounters a concrete repeated-trash problem whose
cost warrants a correction. Frequency, practical cost, and a current failing
non-Undo journey remain unknown. There is no established prerequisite requiring
this cleanup before portable Git trash or broader web organization work.

The strongest smaller alternative is to retain ordinary collision suffixes and
use existing Move recovery. Suffixes distinguish retained items and can be the
correct, predictable result. If the complaint is merely that a legitimate
collision has a suffix, the current behavior may already meet the useful goal.
The owner has now dropped this item from the active backlog because the
clarified journey already works. Do not invent replacement work to fill the story.

#### Scope — proposed narrow boundary

- Select one demonstrated web repeated-trash journey in one notebook and one
  observable correction. The exact correction is still unresolved; no generic
  cleanup or naming redesign is promised.
- Preserve retained content, note/folder identity, learning history, permissions,
  reference-handling choices, and ordinary Move recovery. Keep location-derived
  trash eligibility and existing occupied-destination collision rules.
- Do not merge, overwrite, or discard earlier trash to obtain a nicer name.
  An empty folder can be intentionally retained; a folder Readme is authored
  content. Emptiness alone does not justify deletion.
- Exclude all Undo work, including reverse-order/nested recovery repairs, folder
  Undo, and cross-session Undo. Also defer historical trash cleanup, automatic
  empty-folder pruning, provenance tracking, bulk operations, permanent-delete
  UI, Restore (story 32), and new Git/local compatibility work.
- Modal warning cleanup is independent unless a warning exposes a failure of
  the chosen user journey. No browser-wide no-warnings requirement is selected.
  These exclusions are deferred promises, not new product rejection rules.

#### Key examples — refinement probes, not agreed new acceptance

1. **Repeated items and a real collision:** An earlier subtree already occupies
   `_trash/Research`. Trash another active `Research` subtree. Preserve the
   earlier subtree and suffix the incoming one as a whole with the first
   available number. This is existing intended behavior; identify what actually
   fails before treating it as new work.
2. **Possible non-Undo leftover-path journey:** Start with active
   `Research/Cells.md` and no `_trash/Research`. Trash `Cells`, recover it through
   ordinary Move, then trash active `Research`. Investigate only if the owner
   selects this journey. Whether retaining the empty mirrored path and producing
   `Research (2)` is unacceptable remains a product question. Do not substitute
   this journey for Undo silently or promise automatic cleanup.
3. **Preservation boundary:** An empty `_trash/Research` was intentionally
   retained, or contains a Readme. Later trash operations must preserve it and
   follow ordinary conflicts. A nicer name cannot take precedence over work.

#### Evidence and critical risks

Source inspection on 2026-09-16 found existing scenarios in
`e2e_test/features/folder_organization/folder_trash.feature` covering retained
subtree recovery and whole-folder collision suffixing. The Move-recovery scenario
in `e2e_test/features/cli/cli_notebook_web_trash.feature` explicitly expects
`_trash/Biology/.keep` after the note leaves trash. Thus an empty mirrored folder
is currently represented as retained portable content in that example. Removing
it would change an observed contract, not merely tidy invisible UI scaffolding.
These scenarios were read, not run; they establish intended coverage, not fresh
proof that the current application passes or that repeated operations fail.

Before choosing cleanup, establish whether a disposable path can be distinguished
from intentionally retained content without introducing disproportionate state.
Do not solve a naming inconvenience with a new lifecycle/provenance mechanism.
If the safe correction is larger than the owner benefit, defer the correction.

#### Open decisions and readiness

- Which non-Undo journey causes a concrete problem: repeated distinct items,
  ordinary Move recovery followed by more trash, or another owner example?
- What visible result is wrong, what result should replace it, and why is the
  existing suffix/Move workaround insufficient now?
- Once a journey is chosen, confirm the gap at the current revision before
  implementation. No new acceptance outcome, effort estimate, executable plan,
  or implementation is established by this refinement.

<a id="story-32"></a>

### 32. Restore a trashed item to its visible original path

**Owner direction — 2026-09-16:** No additional trash features are to be
developed. Remove this optional shortcut from the active backlog; ordinary Move
remains the recovery journey. The earlier refinement below is retained as
unselected history, not an implementation commitment.

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
  with Move recovery or the intervening Git work. On 2026-09-15, the owner moved
  the non-reproducible story 41 below Restore; that later direction supersedes
  only Restore's last-place position.
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

<a id="story-28"></a>
<a id="story-30"></a>

### Former stories 28 and 30 — Use portable trash across Donut and local Git

Local Git checkouts move notes and unchanged folder subtrees into or out of
root `_trash` as ordinary files. Publication that resolves those as moves keeps
the same note identities and retained learning; availability follows the final
location. Destination parents required by the committed tree are constructed
through ordinary folder ownership. Same-notebook folder Trash is one accepted
web change. Recovery uses existing location and availability rules, not a
separate restoration path. See
[ADR 0004 — Trash](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
and
[One complete accepted web change](../NORTH-STAR.md#one-complete-accepted-web-change).

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. The implemented
web Trash/Undo loop supplies the starting behavior; story 34 adds discovery of
older trash. The code for migrating and removing the old soft-delete structure
is release-ready and runs through normal Flyway startup. Story 39 owns removing
its temporary migration support after production and other long-lived databases
have crossed it. Neither relies on completing new Git move/rename or trash
compatibility.

The owner initially placed the incoming-reference correction in story 41 first,
then moved it to the bottom on 2026-09-15 after manual testing could not
reproduce the production report. The deeper-folder reproduction on 2026-09-17
made it actionable again, and it is now Taken with a slice plan.

Folder Trash with ordinary Move now supplies the complete folder round trip.
The 2026-09-16 direction ends additional trash feature development: Restore is
no longer selected. Story 42 is now the first queued item and owns the one-hour
manual validation of the completed append-only local/web workflow; publication-
scale validation remains after it. Story 39 was identified as the first post-
release cleanup; story
38 is retained in this seed but was removed from the backlog on 2026-09-16
because the clarified repeated-trash journey already works and Undo is deferred.

Completed stories 20 and 25 supply accumulated local publication and web-note
movement evidence to story 42; they are not remaining queue items. Story 38 is
no longer selected for delivery. Former story 23 is
absorbed into portable-trash Git publication because its deletion-sync scope
overlaps the trash lifecycle; its outcome is retained rather than cancelled.
Publication performance remains the next selected Git-scale item.

## Refinement Questions and Sizing Risks

- Restore uses ordinary conflicts when a required parent path is a non-folder.
- Check simplicity cumulatively through story 31: fewer product-code lines and
  concepts, one state owner, and removal of duplicated rules. Preserve meaningful
  external proof even if test coverage increases total repository lines.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- All new Undo work is postponed by the owner on 2026-09-16. This includes the
  former story 38 Trash → Undo → parent-folder Trash leftover-path outcome and
  the ignored reverse-order Undo scenario in
  `e2e_test/features/note_creation_and_update/note_deletion.feature` (notes `TDD`
  and `tech`, not folder Trash). The ignored test does not establish a current
  failure. Reassess purpose and reproduce any gap before selecting future work;
  no Undo repair or extension remains a commitment of story 38.

- Broader reconciliation of independently advanced local and remote histories,
  including multiple local commits, structural changes, and conflict recovery.
  Earlier proposals used ordinary Git rebase; the current direction excludes it.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Wider folder operations. Same-notebook web folder moves, rename-with-content-edit,
  and multi-commit note identity preservation are supported. Cross-notebook
  folder-move Git histories and further subtree composition remain deferred.
- Native standard Git transport, notebook binding within a project subdirectory,
  attachments, and history browsing or revision restoration.

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
The old proposal's rebase model and web-tip amendments are not constraints on
the newly selected append-only stories.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Ordinary-note rename-with-edit direction is settled in the North Star.
  Wider identity outcomes remain deferred;
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  remains Proposed. Confirmed deletion/recreation starts a new identity.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
