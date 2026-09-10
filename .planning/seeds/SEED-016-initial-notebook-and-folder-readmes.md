---
id: SEED-016
status: dormant
planted: 2026-09-09
planted_during: README-only folder publication verification
trigger_when: a concrete unsupported initial tree warrants story selection
scope: small
---

# SEED-016: Publish the initial notebook and folder Readmes together

## Why This Matters

Notebook owners can publish the delivered initial Readme/Note layouts below
without splitting their authored commit. Story 6 extends this to three more
small layouts exposed by the jap1 investigation. Story 7 joins two endpoint
Notes and their Relationship in one initial publication. Preserve authored bytes and
accept the exact commit atomically. Arbitrary initial trees and bulk import
remain deferred.

## Story Decomposition

<a id="story-1"></a>

### 1. Publish the initial notebook README with one README-only folder

**Status:** delivered. Recover quick/079 from `034be9f99d`.

- **Goal:** A notebook owner can publish one useful initial container tree from
  a local checkout: the notebook Readme and one README-only root Folder, kept
  together as the authored commit.
- **Scope:** The notebook is empty of folders and the accepted Portable tree is
  empty (empty folders alone do not qualify). Exactly one direct-child commit
  adds regular-file `README.md` and `New Folder/README.md`, both valid,
  nonblank `type: Readme` Markdown. Publication creates one fresh root Folder,
  stores both authored Readmes, and accepts the exact commit atomically.
  Excluded: ordinary notes, additional or nested folders, more changed paths,
  an already non-empty notebook (including existing empty folders), README
  edits to existing containers, multiple unpublished commits, stale/divergent
  history, attachments, and bulk import.
  Empty-notebook eligibility correction: quick/080.

<a id="story-2"></a>

### 2. Publish one initial note inside the new README-backed folder

**Status:** delivered. Recover quick/081 from its merge onto main;
ordinary-Note third-path eligibility via quick/082.

- **Goal:** A notebook owner can publish the smallest useful initial notebook
  that contains knowledge: the notebook Readme, one new root Folder with its
  Readme, and one ordinary Note inside that Folder, kept as one authored
  commit.
- **Scope:** The notebook has no folders or live notes and its accepted
  Portable tree is empty. Exactly one direct-child commit adds regular-file
  `README.md`, `New Folder/README.md`, and `New Folder/First note.md`. Both
  Readmes are valid nonblank `type: Readme` Markdown; the note is valid
  `type: Note` Markdown with a valid filename-derived title. Publication stores
  both Readmes, creates one fresh root Folder and one fresh ordinary Note in
  that Folder, and accepts the exact commit atomically. Excluded: any fourth
  path, root notes, more notes or folders, nested folders, existing notebook
  content (including empty folders), relationships, attachments, multiple
  unpublished commits, stale/divergent history, and bulk import.

<a id="story-3"></a>

### 3. Publish the initial notebook README by itself

**Status:** delivered. Recover quick/083 from its merge onto main.

- **Goal:** A notebook owner can publish the smallest
  local tree: the notebook's root Readme as the first and only file.
- **Scope:** The notebook has no folders or live notes and its accepted
  Portable tree is empty. Exactly one direct-child commit adds regular-file
  `README.md` containing valid, nonblank `type: Readme` Markdown. Publication
  stores that authored notebook Readme and accepts the exact commit atomically.
  Excluded: every second path, folders, ordinary notes, existing notebook
  content, README edits, blank Readmes, attachments, multiple unpublished
  commits, stale/divergent history, and bulk import.

<a id="story-4"></a>

### 4. Publish a minimal initial container with one note

**Status:** delivered. Recover quick/085 from its merge onto main.

- **Goal:** A notebook owner can publish either smallest two-file initial
  container-and-note tree without splitting the authored commit, while a
  larger valid initial composition that remains unimplemented surfaces loudly
  to developers instead of being misreported as reserved-README misuse.
- **Scope:** The notebook has no folders or live notes and its accepted
  Portable tree is empty. One direct-child commit adds exactly either (a)
  root `README.md` plus one root ordinary Note, or (b) one root Folder's
  `README.md` plus one ordinary Note directly inside that Folder, with no
  notebook Readme. Readmes are valid nonblank `type: Readme` Markdown and the
  note is valid `type: Note` Markdown with a valid filename-derived title.
  Publication stores the authored content, creates the required Note and
  Folder projection, and accepts the exact commit atomically. After all
  supported shapes are considered, another safe and valid initial Readme/Note
  composition is allowed to fail loudly under ADR 0006 rather than becoming a
  handled reserved-README client error. Invalid Markdown, unsafe paths,
  non-regular files, stale/divergent history, and authorization failures retain
  their deliberate outcomes. Excluded: accepting any third path, nested
  folders, relationships, attachments, existing notebook content or README
  edits, multiple unpublished commits, and bulk import.

<a id="story-5"></a>

### 5. Publish the next small initial Readme-and-Note trees

**Status:** delivered via quick/087 (`33b2b34ebc`); exact root-Note eligibility
corrected by quick/091 (`5d2c5b3d9d`).

- **Goal:** A notebook owner can publish the next six small, valid initial
  Readme-and-Note tree shapes as one authored commit instead of restructuring
  the checkout to fit one previously hard-coded shape.
- **Scope:** The notebook has no folders or live notes and its accepted
  Portable tree is empty. One direct-child commit contains one of exactly these
  layouts: (1) one Note inside one new root Folder without a Folder Readme; (2)
  one nested Folder Readme; (3) two sibling root Folder Readmes; (4) notebook
  Readme plus two root Notes; (5) one root Folder Readme plus two Notes directly
  inside it; or (6) notebook Readme plus one root Folder Readme plus one root
  Note. Root means no Folder prefix; a Note under a different Folder is not
  a root Note. Files are regular, authored Markdown with role-correct `type: Readme`
  or `type: Note`; Note filenames produce valid titles. Publication creates
  every required Folder and Note, preserves authored bytes, and accepts the
  exact commit atomically. Valid unmatched Readme/Note trees continue to fail
  loudly under ADR 0006. Excluded: every other layout, more than three files,
  more than two new Folders or Notes, folder depth beyond the single
  nested-Folder example, Relationship or unknown document types, attachments,
  existing notebook content or README edits, multiple unpublished commits,
  and bulk import.

<a id="story-6"></a>

### 6. Publish three small initial layouts exposed by the jap1 failure

**Status:** delivered via quick/092.

- **Goal:** A notebook owner can publish a small initial notebook as one authored
  commit in each of the three layouts identified during the jap1 investigation,
  without manually splitting the commit to fit the current publication handlers.
- **Scope:** An owner-authorized checkout bound to an empty notebook (no folders
  or live notes; empty accepted Git tree) publishes one direct single-parent
  child of current accepted main. Exactly one of: notebook README plus three root
  Notes; two ordinary Notes in one implied root Folder with no README; or
  notebook README plus one root Relationship with absent endpoints under
  existing unresolved-reference semantics. Preserve authored Markdown,
  filename-derived titles and placements, and accept the exact commit atomically.
  An invalid authored property in the implied-folder layout is rejected with no
  Folder, Note, reference-index, or accepted-head change. Existing supported
  layouts remain supported. The Relationship case does not admit a Relationship
  in every previously supported ordinary-Note position. Excluded: combining the
  three layouts; arbitrary note counts or folder depths; four root Notes with
  README; mixed Note/Relationship batches; relationship endpoint creation or new
  resolution rules; unknown types in these newly admitted positions;
  existing-notebook changes; README editing; stale/divergent history; multiple
  unpublished commits; attachments; import tooling or error-reporting redesign;
  performance/volume guarantees; publishing the actual jap1 checkout.

<a id="story-7"></a>

### 7. Publish two notes and their relationship together

**Status:** refined; planned in [quick/094](../quick/094-initial-notes-with-relationship/PLAN.md).

- **Goal:** A notebook owner can publish a minimal connected set of knowledge
  from a local checkout in one commit: two Notes and their Relationship,
  alongside the notebook Readme. After publication, the relationship's links
  lead to the newly published Notes.
- **Value:** Story 6 accepted an isolated Relationship with missing endpoints.
  This story makes that relationship useful together with its locally authored
  endpoints, without manually splitting publication into several commits.
- **Scope:** An owner-authorized checkout bound to a notebook with no folders or
  live notes and an empty accepted tree publishes one direct single-parent child
  of current accepted main. Exactly four regular Markdown files at notebook
  root: `README.md` with `type: Readme`, two files with `type: Note`, and one with
  `type: Relationship`. Titles come from filenames; authored Markdown, including
  unknown properties, is preserved. Accept the exact commit atomically. Existing
  validation, reference resolution and supported layouts remain unchanged.
- **Key examples:**
  1. `README.md`, `A.md`, `B.md`, and `A-related-to-B.md` (Relationship with
     `relation: related-to`, `source: "[[A]]"`, `target: "[[B]]"`) are added in one
     commit → publication succeeds; the Readme and all three concepts are stored
     at root and downloading returns the exact authored Git head/tree.
  2. Open the published Relationship → its A and B wiki links resolve to the two
     newly published Notes. Do not rely on Git path order: the Relationship may
     sort before its endpoints. A relationship that names an absent endpoint
     still follows existing unresolved-reference semantics; endpoint existence
     is not an additional publication eligibility rule.
  3. The same four-file layout has an invalid authored `note_level` on the
     Relationship → publication rejects with property context and leaves the
     Readme, Notes, reference rows and accepted head unchanged.
- **Excluded:** Additional Notes or Relationships; README plus just one Note and
  a Relationship; folders or nested paths; existing-notebook edits; attachments;
  multiple unpublished commits or history reconciliation; new relationship
  resolution rules or endpoint creation; unknown document types in the newly
  admitted positions; generic initial-tree import; error-reporting redesign;
  performance guarantees or publishing the actual jap1 checkout.
- **Assumptions:** The existing source-owned reference index and current-state
  resolver handle same-commit endpoints without a new import/linking pass. Scope
  is the four-file composition, not a new validity rule requiring exact A/B names
  or a particular relationship kind.
- **Alternative considered:** Publish root Notes first, then publish the
  Relationship separately. It requires splitting the author's commit and loses
  the single atomic initial-publication outcome. Extending only another note
  count would not test mixed Note/Relationship composition.
- **Safe stopping point:** The completed story publishes a useful connected
  notebook even if bulk import is never implemented. No other jap1 gaps are
  prerequisites or implicit follow-up scope.

## Ordering and Scope Reduction

Stories 1–6 and their eligibility corrections are delivered. Story 7 is the next
bounded continuation selected for refinement/planning on 2026-09-10. No product
backlog reorder is implied. Full initial import remains deferred.

## Open Decisions

None.

## When to Surface

Stories 1–6 are delivered. Story 7 is ready for execution upon authorization.
Additional content and bulk initial import remain separate problems.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
- Accepted ADR 0006 permits unimplemented behavior to fail loudly so it creates
  a Failure report instead of requiring a handled client outcome.
