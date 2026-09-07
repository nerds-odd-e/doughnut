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

For an Obsidian user who also uses an AI-enabled IDE such as Codex, Cursor, or
Claude Code, a Donut notebook that currently lives only as remote Donut state
should become an ordinary local Git repository that the user can open, refine,
and synchronize with the accepted remote copy, within the constraints below.

The current catalog ZIP export is decoration, not a synchronization workflow.
Initializing a personal Git repository from a ZIP gives the user neither a
shared remote history nor a safe way to return changes to the same Donut
entities. Re-exporting and copying files cannot reliably detect divergence,
merge accumulated changes, or preserve the private identity and learning data
of a renamed note.

The desired effect is observable without a Donut-specific editor: the user
works on the Portable notebook tree with Obsidian or an AI IDE, commits with
Git, and synchronizes in either direction without silent overwrite or transfer
of learning history to the wrong note.

The product constraints established in the discussion are:

- The working tree follows Accepted ADR 0004 and contains no Donut IDs,
  manifests, sidecar files, or local database.
- V1 uses one dedicated repository per notebook, rooted at `/`, with one
  linear `main`. Published history is never rewritten; unpublished local work
  is rebased.
- Direct use of standard `git clone`, `git fetch`, and `git push` against Donut
  is a later capability. V1 may require the Donut CLI to obtain and synchronize
  the repository, but Git commits, trees, refs, and rebase remain the
  synchronization model. Minimal binding/authentication data may live in
  ordinary Git configuration or the normal credential store, never in the
  Portable tree.
- Every existing notebook is automatically Git-backed during one fleet
  migration; there is no owner opt-in or clone-triggered persistence mode.
  Each existing notebook receives one root commit containing its canonical
  tree at cutover. Earlier history is not fabricated. New notebooks are
  Git-backed from creation.
- The accepted Git `main` is authoritative for Portable content after cutover.
  MySQL remains the current application projection and the authority for
  Donut-only identity-bound data.
- Every accepted Donut web change is represented by a commit. V1 has no Donut
  historical-checkout or revert UI.
- Unsafe identity conclusions and unresolved content conflicts stop before
  advancing remote `main`.
- A later notebook-to-subdirectory project-repository binding must remain
  architecturally possible, but it is outside this v1 decomposition.

The highest-learning first increment is whether an existing remote notebook can
cross the automatic Git cutover and arrive as an ordinary local repository
that the owner can open with their chosen tools. This tests the baseline,
portability, and user entry workflow before accepting local mutations.

A seed is a story's home, not a feature boundary. Keep each story's
requirements here and link from related seeds instead of duplicating them.
Global selection order lives in the [product backlog](../PRODUCT-BACKLOG.md).

## Alternatives and Decision

1. **Do nothing or defer:** users remain limited to the Donut web application
   and one-way export. Rejected because it does not enable Obsidian or
   AI-assisted local refinement.
2. **Offer a smaller snapshot/download behavior:** a ZIP or read-only folder
   gives a local copy but no common history or return path. The user explicitly
   rejected ZIP as merely decorative.
3. **Use a manual existing-tool workflow:** export, run `git init`, edit, and
   manually copy files back. Rejected because it cannot establish the accepted
   remote base, safely detect concurrent changes, or preserve Donut identity
   through structural changes.
4. **Pursue Git-backed synchronization:** automatically bootstrap every
   notebook with one root commit, let V1 use a CLI-assisted Git workflow, and
   add user-visible capabilities in the order below. Recommended because it
   provides immediate local-tool value while keeping one revision and conflict
   model that can later support a standard Git remote.

The estimates below are story hypotheses made without implementation
inspection. Any discovery that changes a story's outcome, boundary, or order
returns to this seed; implementation complexity within one selected story is
handled by `slice-planning` and, when necessary, `slice-plan-refinement`.

## Story Decomposition

<a id="story-1"></a>

### 1. Open an existing Donut notebook in Obsidian and an AI IDE

**Status:** delivered. Completed plan removed by `c0b51e1e4f`; recover evidence
from Git.

- **For / why:** An existing notebook owner wants a normal local directory and
  Git history that their preferred tools can open without first activating or
  converting that notebook.
- **Evaluation:** After the v1 cutover, the owner uses the supported
  CLI-assisted flow for any compatible existing notebook, receives its
  canonical Portable tree in an ordinary Git repository with one root commit
  for the pre-cutover state, and opens the same files in Obsidian and an AI
  IDE. No fabricated earlier commits or Donut metadata appear in the tree.
- **Value / learning:** Delivers useful local ownership and AI/Obsidian access
  even if publishing is cancelled. Tests the most consequential first
  assumption: automatic bootstrap plus local delivery can preserve the
  notebook exactly.
- **Effort hypothesis:** L — low confidence; assumes the accepted ADR-0004
  representation and CLI authentication can be reused, but no implementation
  inspection has tested that assumption.
- **Depends on:** none.
- **Safe stopping point:** The local copy is useful as a snapshot, but the
  product must clearly say that local publishing is unavailable until Story 2;
  creating the copy must not mutate remote content.

<a id="story-2"></a>

### 2. Publish a local content edit to the same Donut note

**Status:** delivered. The completed publication plan was removed; implementation
and proof remain recoverable from Git history.

The owner can publish one direct-child Git commit that edits one existing
Portable Markdown note. Donut advances the accepted head and updates that same
Note while preserving its identity-bound learning data. Invalid, stale,
structural, or competing proposals do not overwrite the accepted projection.

<a id="story-3"></a>

### 3. Receive a Donut web edit in a clean local repository

**Status:** delivered. The completed quick plan was removed; implementation and
proof remain recoverable from Git history.

**Goal**

A notebook owner who alternates between Donut and Obsidian or an AI IDE can
bring accepted web edits into the same local Git repository without copying
files or cloning again. Together with Story 2, this provides a sequential loop:
receive accepted changes, edit locally, commit and publish, then edit on the web
and receive again. Existing notes retain their identity and learning data.

**Scope**

- Supported web change: edit an existing note's Markdown body or valid authored
  frontmatter at an unchanged Portable path, including notes inside folders.
  Before the save, the notebook's current Portable tree must match accepted
  `main`.
- Each durable save that changes supported Portable content appends an
  immutable commit to accepted `main`, retaining its ancestry. A failed save
  must not leave an accepted Git revision and displayed note content that
  disagree. Saving unchanged Portable content need not create a commit.
- An authenticated owner uses `donut notebook pull <directory>` on an existing
  bound checkout to download accepted history and fast-forward local `main`
  and its working tree. Several sequential accepted saves can be received
  together; identical heads are an unchanged success.
- A clean eligible checkout is on `main`, has no staged, unstaged, or untracked
  work under the existing CLI readiness policy, and its head equals or is an
  ancestor of accepted `main`. No unpublished local commits may be discarded.
- Dirty work, unpublished commits, divergent history, detached HEAD, or another
  branch stop with actionable guidance and preserve local branches, commits,
  index, and files. Receiving does not publish, auto-commit, stash, rebase,
  merge, or resolve conflicts. Stories 8 and 9 own divergence handling.
- Git remains the revision model. Existing checkout binding and authentication
  are reused; no Donut metadata is added to the Portable tree. Direct standard
  Git remote access and a historical-checkout UI remain excluded.
- Commit batching is deferred to Story 10; one commit per changed durable save
  is sufficient here.
- Exclusions: web note creation/deletion/rename/move, folder changes,
  and notebook/folder README edits. Local structural publication remains in
  Stories 4–7. Newly populated notebooks whose accepted tree is still empty,
  and notebooks with earlier unsynchronized web changes, are outside the
  supported starting state. No drift repair, new cutover, or history reset is
  included.
- Conservative handling outside that starting state: retain existing web-save
  behavior, but do not advance accepted Git history or absorb unrelated drift
  into the new content edit. Existing publication drift rejection remains.
  Git failures during eligible saves propagate and roll back rather than
  becoming an out-of-scope drift save. The testability snapshot hook remains
  only for fixture setup involving unsupported structural changes.

<a id="story-4"></a>

### 4. Create a new note locally

**Status:** delivered. The completed quick plan was removed; implementation and
proof remain recoverable from Git history.

**Goal**

A notebook owner who discovers a new concept while working in Obsidian or an
AI IDE can publish it as a new Donut note from the same local Git repository.
The owner sees the note at its authored Portable path in Donut and can continue
editing it through the existing local/web loop. This makes local tools useful
for growing a notebook even if deletion, moves, and divergence are deferred.

**Scope**

- Extend `donut notebook publish <directory>` to accept one direct-child
  commit of the current accepted `main` that adds exactly one regular `.md`
  note file and changes no existing files. Reuse the existing bound-checkout,
  owner authentication, clean working-tree, and `main` readiness rules.
- Approved location boundary: the notebook root or an existing
  folder represented in the accepted tree. Creating parent folders is excluded
  from this story; an existing empty notebook may receive its first
  root note when its current Portable tree matches accepted `main`.
- The filename without its final `.md` supplies the display name; the parent
  path supplies the location. Apply existing title, path, reserved-name, and
  destination-uniqueness rules. Do not silently rename the file, overwrite an
  existing note, or create a missing parent folder.
- Follow [Accepted ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  require valid typed Markdown and preserve the authored body, headings,
  frontmatter, and links. An authored YAML `title` does not override the
  filename. Valid unknown types remain allowed; creation introduces no new
  type-specific behavior. Invalid content is rejected rather than repaired.
  Advisory names such as `index.md` and `log.md` warn without blocking a valid
  addition; notebook/folder README authoring is outside this story.
- Create a fresh Donut note identity, including when the new file copies the
  contents of an existing note that remains in place. Existing notes and their
  private data retain their identities. No learning history, questions, or
  conversations are copied or reassigned based on matching text.
- Accept the authored commit and new note together, or neither. The notebook's
  current Portable tree must match the accepted parent before publication.
  Invalid input, collisions, stale proposals, projection drift, or failed
  publication leave accepted `main` and remote notes unchanged, preserving
  the local commit for correction. Retrying an already accepted head succeeds
  without creating a duplicate note.
- Once accepted, the new file is part of ordinary clone/pull history and the
  note supports the existing content-edit flow. No Donut IDs or other metadata
  are added to the Portable tree.
- Exclusions: multiple additions or mixed add/edit/delete commits, publishing
  several unpublished commits together, renames/moves, folder or README
  authoring, attachments, web structural synchronization, drift repair, and
  rebase/conflict handling. [Story 11](#story-11) owns multiple additions and
  mixed additions/edits in one commit. Stories 5–9 retain their outcomes.
  Existing single-note content publication remains supported.

<a id="story-5"></a>

### 5. Delete a note locally without transferring its private data

**Status:** delivered in PR #1623, merged as `e60f9ba369` on 2026-09-07.
The completed quick plan was removed in `cad4b44737`; recover it from that
commit's parent. Backlog implications are recorded under Ordering and Scope Reduction.

**Goal**

A notebook owner working in Obsidian or an AI IDE can deliberately remove one
note locally and publish that deletion to Donut. The note stops being active
and due for recall, while its learning history and other private associations
stay with its deleted identity. Learn whether this single-note workflow is
useful before adding broader deletion or identity-inference behavior.

**Scope**

- Extend the existing `donut notebook publish <directory>` flow to accept one
  direct-child commit of current accepted `main` that deletes exactly one
  existing regular Markdown note file and changes nothing else. Reuse owner
  authentication, bound-checkout, clean working-tree, and `main` readiness
  rules. The current Donut Portable tree must match the accepted parent.
- The note may be at the notebook root or inside an existing folder. Apply
  Donut's existing soft-deletion outcome: retain the deleted note and its
  private associations, deactivate its memory trackers, and leave other notes'
  identities and learning data alone. This is not permanent data erasure;
  earlier accepted Git history still contains the file.
- Conservative reference policy for this first delivery: leave authored links
  in other notes unchanged, using the existing leave-dead-links behavior.
  Deletion does not rewrite referring Markdown, remove properties, or reduce
  a relationship note to a source property. No reference-policy chooser is
  needed for this CLI flow.
- Accept the authored commit and deletion together, or neither. Unsupported
  shapes, stale proposals, projection drift, or publication failure leave the
  accepted head and remote note state unchanged, with the local commit
  available for correction. Retrying an already accepted head is an unchanged
  success under the existing matching-projection rule.
- Existing clone/pull exposes the accepted tree without the file. Deleting
  the last note does not delete its notebook or folder or generate a README.
  Empty folders can disappear from the Portable files while remaining in
  Donut, following [Accepted ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- After publishing the deletion, a separately published addition at a
  different, available path uses the existing creation flow and gets a fresh
  identity, even if its content matches the deleted note. No old learning
  history, questions, or conversations are transferred or restored.
- Keep the existing deleted-title collision rule: an addition at the same
  deleted path is rejected. Supporting same-path recreation is deferred;
  this story does not change title reuse or restoration policy. This is a
  conservative scope choice for early learning, not a claim that the full
  delete-and-create ambition in Proposed ADR 0002 is delivered.
- Exclude multiple deletions, any deletion mixed with additions or edits,
  rename/move inference, folder or README deletion, attachment cleanup,
  permanent purge, Git-driven undo/restore, web deletion/restore
  synchronization, multiple unpublished commits, rebase/conflict handling,
  drift repair, and new preview or confirmation UI. Existing supported
  addition/edit publication remains available. Stories 6–10 retain their
  separate outcomes.

<a id="story-6"></a>

### 6. Rename a note without losing its learning history

**Status:** delivered; merged directly to main through `980114d23d` on
2026-09-07; `eaa59f5d69` removed the completed quick plan. Recover the final
plan at `980114d23d`. Two guidance/diagnostic corrections are tracked in
[Plan 52](../quick/052-clarify-note-rename-publication/PLAN.md); that plan owns
their current execution status. Address them before the next feature;
they do not reopen the delivered identity-preservation outcome or create a
duplicate backlog story. Backlog implications are recorded under Ordering
and Scope Reduction.

**Goal**

An owner using Obsidian or an AI IDE can improve a note's filename in its
current location and keep the same learned concept and private history in
Donut. This is useful even if moving between folders is deferred indefinitely.

**Scope**

- Use the existing authenticated owner publish flow from a clean bound
  checkout on main. Accept one single-parent commit directly after accepted
  main, with current Donut Portable content matching that parent.
- Rename exactly one ordinary Markdown note at the root or within its current
  folder, including nested folders. The complete commit removes one file and
  adds one with identical authored bytes in the same parent directory; no
  other file changes. Compare the removed/added pair in that commit, not
  similarity or unchanged copies elsewhere.
- Update the same note's filename-derived title. Preserve tracker activation
  and scheduling, learning history, questions, conversations, and other
  identity-bound data. An inactive tracker remains inactive; an unchanged
  identical copy keeps its own data. Keep YAML title, headings, and all other
  authored bytes unchanged.
- Preserve referring body and property links as authored, using ordinary
  current-state resolution. Links to the old exact path can become unresolved.
  No automatic link rewrite, alias, or reference-policy chooser is included;
  a referrer can be edited in a separate supported content-edit commit.
- Validate the final filename under current title/path/sibling rules, including
  reserved names and soft-deleted destinations. Accept the exact authored
  commit and identity-preserving change together, or neither. Invalid,
  ambiguous, rewritten, mixed, stale, or drifted proposals retain the owner's
  local work and leave accepted remote state unchanged.
- An accepted retry with a matching projection is unchanged. Ordinary clone/pull
  exposes the renamed file with earlier history intact. After publishing the
  rename, a separately authored and published content edit at the new path
  changes that same identity.
- Reject changes of parent directory, even when Git labels them renames.
  [Story 12](#story-12) owns relocation. Also exclude folder/README changes,
  new folders, cross-notebook moves, attachments, multiple unpublished commits,
  rebase/conflict handling, drift repair, restore/deleted-path reuse, and web
  structural synchronization.
- Retain delivered add/edit/delete behavior. In particular, an accepted
  deletion followed by a separate addition at another available path creates
  fresh identity; this must never become a delayed rename.

<a id="story-7"></a>

### 7. Move a folder while preserving descendant identities

- **For / why:** The owner wants to reorganize a group of notes without losing
  the individual Donut identities and learning data under that folder.
- **Evaluation:** The owner commits one unambiguous folder relocation and
  synchronizes it; Donut shows the folder and descendants at their new paths,
  and each corresponding Note or Folder entity retains its private data.
- **Boundary learned from deletion:** Start with a folder represented by
  tracked content and require exact correspondence for the whole relocation,
  including any tracked README. An empty, unrepresented Donut folder has no
  Git move to infer. Disappearance of its last tracked note alone must continue
  to leave the container intact, as Story 5 promises. Reject destination
  collisions, partial moves, and ambiguous correspondence without deleting
  containers or inventing README files. Clarify unrepresented empty descendants
  during story refinement before claiming their identities can be inferred.
- **Boundary learned from renaming:** A successful equal-blob file pair proves
  one note's correspondence, not the identity of its containing folder. Reuse
  the unchanged-authored-tree and private-data contract; do not infer a folder
  move from one note's accepted rename or add similarity-based identity guesses.
- **Value / learning:** Extends safe reorganization from one file to the
  common notebook-level operation. Note-level moves remain valuable if this
  story is cancelled.
- **Effort hypothesis:** L — low confidence; assumes exact descendant
  correspondence can define a bounded folder-move case and excludes ambiguous
  partial rewrites.
- **Depends on:** Story 12.
- **Safe stopping point:** Incomplete or multiply plausible descendant
  correspondence rejects the commit atomically.

<a id="story-8"></a>

### 8. Keep non-overlapping accumulated local and web changes

**Refinement status:** Refined on 2026-09-07. The developer selected explicit
`pull`, then `publish`; the conservative content boundary below is the current
planning understanding. [Plan 53](../quick/053-reconcile-local-web-content/PLAN.md)
owns execution leaves and proof. No implementation has started for this story.

**Goal**

A notebook owner editing in Obsidian or an AI IDE can keep a committed local
refinement when Donut has meanwhile accepted edits to other notes. Both sides'
content survives in the same checkout and, after publication, on the same Donut
note identities with their learning data. This removes the need to discard or
manually copy work merely because the owner did not synchronize before editing.

**Scope**

- Start with an authenticated, bound, clean checkout on `main`, with no active
  Git operation. Local work is exactly one unpublished, single-parent commit
  editing the body or valid authored frontmatter of one existing ordinary note,
  at an unchanged Portable path. Root and folder-contained notes are included.
- Accepted `main` has advanced from that local commit's parent through one or
  more linear content-only commits. Every intervening change modifies existing
  ordinary notes at unchanged paths, and none touches the locally edited path.
  Check the history interval, not only the net endpoint difference: changing
  then restoring a path does not erase the fact that it was touched.
- `donut notebook pull <directory>` rebases that one unpublished commit onto
  the downloaded accepted head. The clean local result contains both changes;
  accepted commits keep their hashes and order, and the local edit remains one
  commit with its author/message and patch. Its hash changes through rebase.
  Pull does not publish. It identifies the local result as unpublished and
  points to `donut notebook publish <directory>`.
- Explicit publication uses the existing one-direct-child contract. Donut
  accepts the rebased content on the original note, keeping private data and
  the other notes' accepted content. A repeat pull before publication leaves
  this already-based local commit unchanged and still tells the owner to publish.
- Rejections retain recoverable local work. Dirty/changed checkout state,
  unsupported histories, or unsupported path changes stop before rebase.
  A remote advance before or during publication retains the existing safe
  rejection; no forced update or automatic retry. A further pull may reconcile
  another eligible other-note advance before the owner publishes again.
- Receiving downloads accepted history only. Pre-existing web projection drift
  remains outside synchronization and publication still rejects it; pull must
  not advertise that it repaired or incorporated unsynchronized web content.
- Existing fast-forward receipt, including already accepted structural changes
  when there is no divergent local work, remains supported. Binding, owner
  authorization, Portable bytes and private identity contracts are reused.

Excluded: same-note concurrent edits even on disjoint lines; conflict markers,
manual conflict continuation/abort workflows (Story 9); multiple unpublished
commits or multiple local edited notes; additions, deletions, renames, folder,
README, attachment or mode changes on either divergent side; structural changes
hidden by a later reversal; drift repair; squash/merge commits, automatic stash,
force/reset recovery, new sync commands, remote transport, background sync,
commit batching, or new metadata in the Portable tree. Git author identity is
not a new server-provenance test: eligibility depends on accepted ancestry and
content shape, with actual web saves used to demonstrate the selected journey.

**Key examples**

1. **Keep both edits:** From accepted A, commit L editing `Biology/Cell.md`
   locally; Donut accepts B editing `Biology/DNA.md` → pull → local L′ is one
   child of B with both edits and a clean checkout; Donut still has B → publish
   → accepted L′ displays both edits on their original note identities.
2. **Receive accumulated web work:** A→B→C edits other notes, including valid
   authored YAML → pull the one local edit → A, B and C remain unchanged and
   the local patch is reapplied once above C. Repeat pull before publish → no
   duplicate or rewritten local commit; the result remains ready to publish.
3. **Same path is outside this increment:** Both sides edit `Cell.md`, even
   different paragraphs, or remote edits then restores it → pull → identify
   the overlapping path without rebasing, publishing, or leaving conflict markers.
4. **Do not infer structural intent:** Either side renames a note, edits a
   README, or remote deletes then recreates a path → pull with divergent local
   work → explain the unsupported change and preserve local state. An ordinary
   clean checkout with no local commit can still receive an accepted rename.
5. **Preserve work across races:** An editor changes the checkout during
   download → pull rejects without overwriting that change. After a successful
   rebase, remote advances again → publish rejects safely and the rebased local
   commit remains available for another pull or ordinary Git inspection.
6. **Keep drift visible:** An unsupported web creation is absent from accepted
   Git history → pull only incorporates accepted commits, and publication
   continues to reject the projection mismatch without losing local content.

**Decisions and dependencies**

- The explicit pull-then-publish workflow was confirmed by the developer in
  this refinement. No unresolved decision blocks this bounded plan.
- Depends on delivered Stories 2 and 3; finish the existing Story 6 corrections
  in Plan 52 before feature execution, as already recorded in the seed.
- Story 9 owns conflicting same-path content recovery. Story 12 owns note
  relocation; neither is a prerequisite for this different-note rebase.
- **Effort hypothesis:** M (about 1–2 hours), low confidence, retaining the
  original comparative estimate for the different-note outcome. Assumes the
  delivered receive/publish workflows remain reusable; history eligibility and
  the complete owner journey are the main sizing risks. Plan 53 records the
  separate execution-leaf sizing hypotheses.
- **Safe stopping point:** After pull, both edits are inspectable locally and
  publication is still explicit. After rejection, remote accepted history has
  not advanced through this command and local work remains recoverable.

<a id="story-9"></a>

### 9. Resolve an overlapping edit with ordinary Git

- **For / why:** The owner needs conflicting local and web refinements to be
  detected and resolved without a Donut-specific merge format.
- **Evaluation:** Local and remote commits overlap in one Markdown file.
  Synchronization presents an ordinary Git rebase conflict, leaves the accepted
  remote unchanged, and, after the owner resolves and continues the rebase,
  accepts the resulting linear history and displays the resolution in Donut.
- **Scope / examples:** Begin with one unpublished content edit conflicting
  with an accepted web content edit at the same unchanged path. Resolve and
  continue → publish the resolved content on the same note identity. Abort →
  recover the original unpublished work without altering accepted history.
  Delete/edit and rename/edit conflicts are deferred: a clean text result or
  conflict resolution must not silently authorize restoration, same-path
  recreation, or a change of private identity.
- **Rename delivery boundary:** Splitting a rename and edit into local commits
  is not yet sufficient for publication: the rename must first be published
  and accepted. This story does not add publication of a multi-commit range
  or resolve identity ambiguity through text-conflict resolution.
- **Value / learning:** Completes the safety promise for overlapping
  accumulated changes while validating that standard Git conflict handling is
  understandable in the CLI-assisted v1 workflow.
- **Effort hypothesis:** M — low confidence; assumes standard text conflict
  behavior is adequate for the first overlapping Markdown case.
- **Depends on:** Story 8.
- **Safe stopping point:** Cancelling or abandoning conflict resolution loses
  neither the accepted remote commit nor the user's original local commit.

<a id="story-10"></a>

### 10. See one stable commit for one continuous web edit

- **For / why:** An owner reading Git history should see a meaningful editing
  unit rather than one commit per autosave or rewritten published commits.
- **Evaluation:** Several continuous saves to the same note, with no intervening
  accepted commit, appear as one stable commit. A different accepted commit
  cuts the batch. Once advertised, the earlier commit ID never changes.
- **Value / learning:** Improves the history users and AI tools inspect without
  changing the synchronization model. All earlier synchronization value
  remains if this refinement is cancelled.
- **Effort hypothesis:** M — low confidence; assumes Story 3 may safely start
  with one commit per durable save and that batching can be added as an
  observable policy refinement.
- **Depends on:** Story 3.
- **Safe stopping point:** Prefer extra immutable commits over amending any
  history already visible to a client.

<a id="story-11"></a>

### 11. Publish several note changes in one commit

**Status:** completed.

**Goal**

A notebook owner can publish related additions and accompanying edits as one
authored Git commit and see the complete change in Donut.

**Scope**

- Use the existing publish flow from a clean bound checkout on `main`, exactly
  one single-parent commit ahead of accepted history. Retain ownership, readiness,
  stale-head and drift checks; current Portable content must match the parent.
- Support several additions, optionally with existing-note edits at unchanged
  paths, plus existing single-note operations. Use root or represented folders
  under ADR 0004's Markdown/path rules; preserve authored bytes without repair.
- Accept the exact commit and all changes atomically. Invalid or unsupported
  members identify their path/reason and reject everything, leaving remote
  notes/history unchanged and local work available for correction.
- Give additions fresh identities and retain edited notes' private learning
  data; copying text transfers no private associations. Accepted retries leave
  notes and history unchanged. Existing note views and clone/pull expose results.
- Exclude edits-only batches, deletions, renames/moves, new folders, README changes,
  attachments, multiple unpublished commits, divergence/rebase/conflicts, web
  structural synchronization/batching, drift repair, direct Git remotes, bulk-import
  optimization, preview, partial publication and automatic commit splitting.

<a id="story-12"></a>

### 12. Move a note between existing folders without losing its learning history

**Status:** refined and [slice-planned](../quick/050-relocate-local-note/PLAN.md);
not implemented. Story 6's identity-preservation prerequisite is delivered.
Stories 8 and 9 remain ahead by product priority, not technical dependency.

**Goal**

An owner organizing notes in Obsidian or an AI IDE can put a learned concept
in a better existing folder, or at the notebook root, without starting its
learning history again. Renaming in place cannot achieve this outcome.

**Scope**

- Extend the identity-preserving publication contract delivered by
  [Story 6](#story-6) to exactly one change of parent directory within the same
  notebook, optionally changing the filename in the same commit. Retain its
  exact-byte, one-pair/no-other-changes rule, ownership/readiness, atomicity,
  private-data preservation, unchanged references, retry, and rejection rules.
- Reuse the delivered raw removed/added blob correspondence and same-note
  mutation; receiving a rename and a later edit already works through ordinary
  pull. The new learning here is final destination eligibility and container
  preservation, not a second transport or identity mechanism.
- Support root→existing represented folder, folder→root, and folder→folder,
  including nested destinations and folders represented only by a README.
  The destination must exist in Donut and be represented in accepted parent
  history. Do not infer a new folder from a local directory or an invisible
  empty Donut folder.
- Validate the final folder/title together. Neither a hypothetical intermediate
  title at the source nor the old title at the destination should block an
  otherwise valid combined relocation/rename. An occupied or soft-deleted
  final destination remains unavailable.
- Preserve source and destination folder identities. Moving the last tracked
  note out of a folder can make it unrepresented in Git; leave its Donut
  container intact without generating a README. Story 7 owns folder moves.
- Publish through the installed CLI and expose the accepted location through
  ordinary clone/pull with retained history. Publish and accept the relocation
  before authoring and publishing a later content edit at that location.
  Extend the corrected Story 6 guidance to name newly supported destinations,
  unchanged authored links and path-specific rejection reasons. Keep rejected
  work available; do not duplicate or regress Plan 52's corrections.
- Keep Story 6's exclusions: no changed content or accompanying reference
  rewrite, multiple moves, new/unrepresented destination folders, folder or
  README relocation, cross-notebook moves, restore, deleted-path reuse,
  multiple unpublished commits, divergence, drift repair, or new UI/metadata.

**Key examples**

1. Move Inbox/Cell.md to Biology/Cell.md → publish → the same note appears in
   the existing represented Biology folder; another checkout receives it.
2. Move Inbox/Cell.md to Biology/Cell basics.md → publish → validate only that
   final available destination, even if Cell basics is reserved in Inbox.
3. Move Inbox's only tracked note to the root → publish → Inbox remains a
   Donut folder, with no generated README. Other identities remain untouched.
4. Target a missing or unrepresented folder, or a reserved final path → reject
   without changing the source note, containers, or accepted history.
5. Publish relocation, then separately publish a content edit there → receive
   both commits in another clean checkout → same learned note, new location
   and content, original ancestry retained.

- **Evaluation:** Installed CLI relocation changes the note's visible folder
  while preserving its identity and private data; another checkout receives it.
- **Value / learning:** Delivers filing/reorganization independently of moving
  a whole folder; tests destination eligibility and container preservation.
- **Effort hypothesis:** M (about 1–2 hours), low confidence. Assumes exact
  single-note relocation to already represented folders stays bounded and
  Story 6's identity contract remains usable. This is a behavior-level
  hypothesis, not an estimate inferred from implementation size.
- **Depends on:** Story 6's delivered identity-preserving publication contract.
  Stories 8 and 9 are higher priorities, not technical prerequisites.
- **Safe stopping point:** Owners can rename and file one note while whole
  folder moves remain unsupported.
- **Open decisions:** none within existing represented destinations.

## Ordering and Scope Reduction

Stories 1–6 and 11 are delivered. The 2026-09-07 review after Plan 49 keeps
Stories 8 and 9 next, ahead of Story 12, followed by Story 7; Story 10 remains
last. Complete the bounded Story 6 corrections in Plan 52 before expanding
the workflow. The [product backlog](../PRODUCT-BACKLOG.md) owns the global
story order; corrective execution leaves remain attached to their home story.

### Learning from the delivered rename story

- **Identity preservation now has a demonstrated first case.** An isolated,
  same-parent equal-blob pair updates the same Note; an unchanged identical
  copy keeps its own data. Private associations and late-failure rollback are
  covered by `6e983e55a9`, `1a166ebfea`, and `83e9e6daa3`. This satisfies the
  technical prerequisite for relocation without raising its product priority.
- **Receipt is reusable; divergence remains the workflow gap.** Separately
  accepted rename/edit commits update the same identity and arrive in another
  clean checkout (`295bedeb06`, `980114d23d`). This does not demonstrate
  publishing several unpublished commits, rebasing structural changes, or
  repairing web projection drift. Keep Stories 8/9 narrowly content-focused.
- **Authored links and publication boundaries need explicit guidance.**
  Referrers remain unchanged (`bde458d888`), so old-path links can stop resolving.
  The owner must publish/accept the rename before making the later edit.
  The retrospective found incomplete CLI instructions and missing destination
  context for a reserved-title rejection; Plan 52 owns those corrections.
- **No evidence supports broader identity inference yet.** Folder moves,
  rename-with-edit commits, and structural conflicts still need their own
  bounded outcomes. Passing automated examples establish feasibility and
  safety in the delivered scope, not user demand or calibrated effort estimates.

### Learning from the merged deletion story

- **Decision — commit boundaries express identity intent.** Isolated deletion
  followed by a separate addition at an available path leaves private data
  with the old identity. A move needs a distinct same-identity acceptance
  rule; delete then create is not a workaround for Story 6. Evidence:
  `2f9ca9c366` and
  [fresh-identity publication proof](../../backend/src/test/java/com/odde/donut/controllers/NotebookGitCopyIdentityControllerTest.java).
- **Lesson — successful receipt does not solve divergence.** Accepted deletion
  can already arrive through ordinary pull, but a newer accepted web edit
  blocks stale deletion, and unsynchronized web creation still blocks
  publication as projection drift. Evidence: `a0cf7fefef`, `cad4b44737`, and
  [projection-drift proof](../../backend/src/test/java/com/odde/donut/controllers/NotebookGitProjectionDriftControllerTest.java).
  This supports prioritizing recovery from ordinary content divergence before
  broader folder reorganization. Drift repair is a separate unresolved need.
- **Pattern — accept the authored tree without hidden collateral edits.**
  Deletion leaves referring body and property links authored and unresolved;
  rollback and retry protect the complete accepted outcome. Reuse that visible
  contract when refining moves instead of adding silent link cleanup.
  Evidence: `d703f095c3`, `a2c337142f`, and
  [deletion publication proof](../../backend/src/test/java/com/odde/donut/controllers/NotebookGitDeletionPublicationControllerTest.java).
- **Boundary — absent files do not imply absent identities or containers.**
  Same-path recreation remains rejected; deleting a folder's last note leaves
  its Donut folder intact even when it vanishes from the Portable files.
  Folder relocation therefore needs stronger evidence than a disappearing
  directory. Evidence: `cad4b44737`, `2f9ca9c366`,
  [reserved-path proof](../../backend/src/test/java/com/odde/donut/controllers/NotebookGitDeletedDestinationControllerTest.java),
  and [container proof](../../backend/src/test/java/com/odde/donut/controllers/NotebookGitDeletionContainerPublicationControllerTest.java).

These are implementation and executable-example findings, not evidence of
real-user frequency or satisfaction. The removed plan has no reliable completed
duration summary; do not infer actual effort from commit timestamps or claim
estimate calibration. Retain low confidence for the remaining story estimates.

### Rename/relocation split and alternatives

For the same Obsidian/AI-IDE owner, rejected file reorganization should become
safe identity-preserving publication under the existing Portable-tree and
single-commit constraints. The owner's request to split the 17-leaf plan
authorizes this story-boundary review; no execution evidence is discarded.

- **Defer all reorganization:** leaves users unable to rename a learned concept.
- **Deliver same-folder rename first:** selected. A useful, evaluable naming
  improvement tests exact correspondence without parent-placement policy.
- **Manual workaround:** keep the old filename, or delete then create through
  the delivered flow. Keeping it does not deliver better naming; delete/create
  deliberately gives fresh identity and therefore cannot preserve learning.
- **Deliver all rename/relocation together:** retains the old broad scope and
  delays the first usable result behind folder rules; split into Stories 6/12.

Both children pass 3V: the owner sees a useful renamed or refiled note through
the complete CLI/Donut/receive loop. Safety checks are acceptance obligations,
not independent product stories. Story 6 retains its anchor and narrower title;
Story 12 has a new anchor. Their union preserves the former Story 6 scope.
The first story remains M with low confidence because identity safety is still
essential; the split removes folder policy rather than disguising it as tests.

### Priority and deferred follow-ons

Same-folder renaming has delivered the first identity promise. Content
divergence and conflict handling remain next because ordinary local/web
overlap blocks the delivered lifecycle. Single-note relocation follows them,
then whole-folder moves; this preserves the earlier choice to address broader workflow blockage before
folder policy. Story 12 depends on Story 6, and Story 7 follows Story 12.
Batching improves history quality after synchronization works.

No new feature story is promoted from the deletion or rename proofs alone.
Keep these possibilities deferred until the stated learning warrants selecting
and refining a concrete outcome:

- **Same-path fresh creation:** revisit when owners need to reuse deleted
  names; decide its interaction with restore and authored path references.
  A separately published addition at another available path is today's
  supported alternative. This is not required for moving a live note.
- **Multiple or mixed deletions and reference cleanup:** revisit when separate
  deletion commits and subsequent referrer edits obstruct real cleanup work.
  Keep the small supported workflow rather than infer demand from a rejected
  test case. Story 11 remains completed within its additions/edits scope.
- **Web structural synchronization or drift recovery:** revisit when web
  creation/deletion/moves interrupt the local workflow. Stories 8 and 9 must
  not claim to repair an uncommitted projection change through Git rebase.
- **Delete/edit or rename/edit conflicts and multiple unpublished commits:** revisit after
  the first content divergence/conflict workflow; explicitly settle identity
  intent before broadening conflict resolution to deletion or recreation.

Safe stopping points:

- After Story 3: basic sequential two-way editing of existing note content;
  creation, deletion, moves, and divergence remain rejected or unsupported.
- After Story 5: basic note content lifecycle without structural identity
  inference.
- After Story 6: one unambiguous same-folder rename preserves private identity.
- After Stories 8 and 9: one unpublished content edit can be reconciled with
  accepted web content edits, including a manually resolved text conflict.
- After Story 12: one note can change existing folders while retaining identity.
- After Story 7: represented folder reorganization also preserves private
  identity within its refined correspondence boundary.
- After Story 10: web-authored history also has the desired editing granularity.

First-to-defer order among unfinished queued stories is:

1. Story 10, accepting extra immutable web commits.
2. Story 7, rejecting folder moves while retaining note relocation.
3. Story 12, retaining same-folder rename while deferring cross-folder filing.
4. Story 9, retaining automatic non-conflicting content rebase from Story 8
   while stopping safely on text conflicts.
5. Story 8, retaining the explicitly sequential synchronize-before-edit loop.

The delivered and queued boundaries still leave parts of Proposed ADR 0002
uncovered, including web structural synchronization and broader accumulated
history. Finishing this queue is not proof of complete ADR-0002 v1 support.
Unsupported operations must fail clearly rather than be approximated.

## Open Decisions

No open decision blocks the bounded rename and relocation stories. Same-path
creation, unrepresented empty descendants in folder moves, and structural conflict
policies need refinement before those capabilities are selected or expanded.

ADR 0002 now reflects the later human discussion recorded here: v1 permits a
required CLI-assisted acquisition and synchronization workflow and defers direct
standard-Git access. Its status remains Proposed; the recorded product
constraints above do not turn it into an Accepted ADR.

The exact CLI verbs, authentication presentation, and whether its Git transport
appears as a dedicated sync command or a Git remote helper do not change these
story outcomes. Slice planning may decide them only if the result keeps Git as
the revision/merge model and adds no Donut metadata to the Portable tree.

## When to Surface

Stories 1–6 and 11 are delivered. Select one remaining story from the
[product backlog](../PRODUCT-BACKLOG.md) before slice planning. Do not turn
the whole seed into one executable plan. Reconcile the Proposed ADR's v1 CLI
boundary as a human-owned advice task; it does not change these story outcomes.

## Breadcrumbs

- Proposed
  [ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  supplies the Git authority, linear `main`, identity, conflict, web-commit,
  history-retention, and future subtree-binding direction. It remains Proposed
  and is not binding.
- Accepted
  [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  defines the Portable notebook tree that every root and later commit contains.
- Accepted
  [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
  defines **Notebook**, **Note**, **Folder**, **Portable notebook tree**, and
  **Portable path**.
- Human decisions from the 2026-09-04 discussion:
  - The primary beneficiary is an Obsidian user who also wants Codex, Cursor,
    or Claude Code to refine their local notes.
  - ZIP export is decoration and is not a useful synchronization alternative.
  - V1 is a dedicated repository rooted at `/`, linear `main`, and rebase-only
    integration. Direct standard-Git clone/fetch/push is later; V1 may use the
    Donut CLI.
  - No Donut identity or synchronization metadata belongs in the Portable
    tree. Minimal repository binding/authentication may use ordinary Git
    configuration and credential storage.
  - Every existing notebook is automatically bootstrapped during one fleet
    migration. Each gets one root commit from the current canonical MySQL
    projection; no earlier Git history is invented and there is no per-notebook
    activation mode. New notebooks begin Git-backed.
  - After cutover, accepted Git `main` is the Portable-content authority and
    MySQL is its current projection plus the authority for Donut-only data.
  - Donut web edits create commits. Continuous same-note edits may be
    coalesced only before publication and only when no other commit cuts the
    sequence.
  - V1 has no Donut historical checkout/revert UI. Reachable Git history is
    nevertheless retained.
- Representative example: edit an existing note in an AI IDE, commit it,
  synchronize, and see the same Donut note updated with its memory trackers
  intact.
- Counterexample: exporting a ZIP, initializing an unrelated local repository,
  and copying files back does not establish common ancestry or identity.
- Boundary: a clean sequential content edit is earlier than divergence;
  ambiguous identity and unresolved conflicts never mutate remote `main`.
- Fresh-agent handoff: this seed contains the product context and story
  decisions, not implementation design. A new agent should read this seed and
  the current ADRs, select one story, then use `slice-planning`, which may
  inspect the codebase and create the executable PLAN.
