---
id: SEED-016
status: active
planted: 2026-09-09
planted_during: README-only folder publication verification
trigger_when: selected now as the top product-backlog story
scope: small
---

# SEED-016: Publish the initial notebook and folder Readmes together

## Why This Matters

A notebook owner starting from an empty accepted notebook can publish one
README-only folder only when it is the sole changed path. If the same initial
commit also authors the notebook's root README, publication refuses the folder
README. The owner should be able to publish that smallest useful initial
container tree without splitting the commit or adding an ordinary note.

## Alternatives and Decision

Keep the existing one-folder-only path and add only the verified adjacent case.
Requiring separate commits would interrupt the owner's initial authored unit;
supporting arbitrary initial trees or the observed 10,406-file commit would
expand this into bulk import and is explicitly deferred.

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

- **Goal:** A notebook owner can publish the smallest currently unsupported
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

**Status:** planned in quick/085.

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
- **Key examples:** Empty accepted tree + `README.md` + `First note.md` stores
  the notebook Readme and root Note. Empty accepted tree + `New
  Folder/README.md` + `New Folder/First note.md` creates that Folder and Note
  while leaving the notebook Readme absent. Adding `README.md` plus two root
  Notes is still unimplemented and surfaces as an uncaught failure without
  advancing the binding.

## Ordering and Scope Reduction

Story 1 was the smallest verified follow-up to the local README-only folder
workflow. Story 2 adds exactly one ordinary Note to that delivered initial
tree. Story 3 isolates adding only the initial notebook Readme. Story 4 joins
each kind of initial container with one Note while keeping the next valid
unimplemented composition visible to developers. Additional content and bulk
initial import remain separate problems.

## Open Decisions

None.

## When to Surface

Stories 1–3 are delivered. Story 4 is selected for execution planning.
Additional content and bulk initial import remain separate problems.

## Breadcrumbs

- [SEED-009 Story 19](SEED-009-git-backed-local-notebook-workflow.md#story-19)
  delivers the sole-changed-path folder case.
- Accepted ADR 0004 defines root and folder README Portable paths and their
  `type: Readme` contract.
- Accepted ADR 0006 permits unimplemented behavior to fail loudly so it creates
  a Failure report instead of requiring a handled client outcome.
