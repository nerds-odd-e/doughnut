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
Notes and their Relationship in one initial publication. Story 8 places that
connected set in one implied root Folder. Preserve authored bytes and
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

**Status:** delivered via quick/094.

- **Goal:** A notebook owner can publish a minimal connected set of knowledge
  from a local checkout in one commit: two Notes and their Relationship,
  alongside the notebook Readme. After publication, the relationship's links
  lead to the newly published Notes.
- **Scope:** An owner-authorized checkout bound to a notebook with no folders or
  live notes and an empty accepted tree publishes one direct single-parent child
  of current accepted main. Exactly four regular Markdown files at notebook
  root: `README.md` with `type: Readme`, two files with `type: Note`, and one with
  `type: Relationship`. Titles come from filenames; authored Markdown, including
  unknown properties, is preserved. Accept the exact commit atomically. Existing
  validation, reference resolution and supported layouts remain unchanged.
  Excluded: Additional Notes or Relationships; README plus just one Note and
  a Relationship; folders or nested paths; existing-notebook edits; attachments;
  multiple unpublished commits or history reconciliation; new relationship
  resolution rules or endpoint creation; unknown document types in the newly
  admitted positions; generic initial-tree import; error-reporting redesign;
  performance guarantees or publishing the actual jap1 checkout.

<a id="story-8"></a>

### 8. Publish two related notes inside one new folder

**Status:** covered by the verified general initial-tree implementation in
[SEED-017 Story 9](SEED-017-cohesive-design-corrections.md#story-9).
[quick/096](../quick/096-initial-related-notes-in-folder/PLAN.md) is superseded;
its prior planning context is retained for retrospective, not execution.
The existing examples below preserve the intended user outcome; their exact
counts must not be carried forward as production restrictions by default.

- **Goal:** A notebook owner can publish a small connected set of locally
  organized knowledge without flattening its Folder or splitting its commit.
- **Scope:** An owner-authorized checkout bound to an empty notebook (no folders
  or live notes; empty accepted tree) publishes one direct single-parent child of
  current accepted main. Exactly four regular Markdown files: notebook `README.md`
  with `type: Readme`, plus two `type: Note` files and one `type: Relationship`
  file directly inside the same new root Folder. The Folder has no README.
  Create one Folder, preserve all authored content and filename-derived titles,
  place all three concepts inside it, and accept the exact commit atomically.
- **Key examples:**
  1. Add `README.md`, `Topic/A.md`, `Topic/B.md`, and
     `Topic/A-related-to-B.md` in one commit. The Relationship carries
     `relation: related-to`, `source: "[[Topic/A]]"`, and `target: "[[Topic/B]]"`.
     Publish → one Topic Folder contains the three concepts; the notebook Readme
     is preserved; download returns the exact head/tree, without adding
     `Topic/README.md`.
  2. Open the published Relationship → both path-qualified links lead to the
     newly published Notes, even when the Relationship sorts before them.
     Existing unresolved-link semantics still apply to absent endpoints; no
     endpoint Notes are synthesized and endpoint existence is not an eligibility gate.
  3. The Relationship has invalid authored `note_level` → deliberate rejection
     with property context leaves no new Folder, Notes, reference rows, notebook
     Readme or accepted-head change.
- **Value / alternative:** This addresses the next structural gap after the
  root-only mixed layout. Flattening into root would lose the author's grouping;
  multiple publications would lose the single atomic commit. Raising another
  file-count limit would not address folder placement.
- **Assumptions:** Existing implied-Folder materialization, authored-document
  persistence and Portable-path resolution suffice. Names and relationship kind
  are examples, not hard-coded eligibility rules.
- **Excluded:** Folder README; omitted notebook README; nested or multiple
  Folders; concepts split between root and Folder; more Notes or Relationships;
  existing-notebook updates; other new document types; attachments; history
  reconciliation; new reference semantics; bulk import or performance guarantees;
  publishing the actual jap1 checkout. Existing supported layouts remain supported.
- **Safe stopping point:** A complete small grouped notebook is usable even if
  later import work is cancelled. No other jap1 gap is required for this story.

<a id="story-9"></a>

### 9. Make initial notebook publication design cohesive

**Moved:** The canonical story, whole Git workflow assessment, and refinement
now live in [SEED-017 Story 9](SEED-017-cohesive-design-corrections.md#story-9).
Its execution is verified. This anchor is retained for existing review references;
Story 8 is covered by the same general initial-tree contract.

## Ordering and Scope Reduction

Stories 1–7 and their eligibility corrections are delivered. Story 8 is covered by
the verified [SEED-017 Story 9](SEED-017-cohesive-design-corrections.md#story-9)
implementation. Plan 096 is superseded. Operating a bulk import remains deferred.

## Open Decisions

Story 9's initial-tree scope and correction order are selected. The remaining
product decision concerns web-write synchronization coverage, recorded in [SEED-017 Story 9](SEED-017-cohesive-design-corrections.md#story-9).

## When to Surface

Story 9's completed plan is available for retrospective; Story 8 needs no separate
execution. Pull and existing-note composition follow in their SEED-017 homes;
bulk-import operations remain separate.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
- Accepted ADR 0006 permits unimplemented behavior to fail loudly so it creates
  a Failure report instead of requiring a handled client outcome.
