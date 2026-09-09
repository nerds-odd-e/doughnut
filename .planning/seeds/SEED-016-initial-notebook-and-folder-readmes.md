---
id: SEED-016
status: completed
planted: 2026-09-09
planted_during: README-only folder publication verification
trigger_when: a concrete unsupported initial tree warrants story selection
scope: small
---

# SEED-016: Publish the initial notebook and folder Readmes together

## Why This Matters

Notebook owners can publish the bounded initial Readme/Note layouts below
without splitting their authored commit. These delivered stories preserve
authored bytes and accept the exact commit atomically. Arbitrary initial
trees and bulk import remain deferred.

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

## Ordering and Scope Reduction

Stories 1–5 and their eligibility corrections are delivered. No additional
initial-tree story is selected. Keep CLI/MCP worktree isolation ahead of further
notebook-import expansion, as ordered in the product backlog. Additional
content and bulk initial import remain separate problems.

## Open Decisions

None.

## When to Surface

Stories 1–5 are delivered. Additional content and bulk initial import remain
separate problems.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
- Accepted ADR 0006 permits unimplemented behavior to fail loudly so it creates
  a Failure report instead of requiring a handled client outcome.
