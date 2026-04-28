# Doughnut Wiki Migration Plan

## Purpose

This document proposes a phased migration plan for moving Doughnut toward the final wiki-style, Obsidian-compatible architecture.

The goal is to implement ongoing/doughnut_wiki_architecture_north_star.md, phase by phase. With the learning from implementing each phase, the north star file will be updated to reflect the latest understanding.

The plan intentionally excludes the safety baseline phase. It starts directly with architectural migration work.

## Target Direction

Doughnut will move from a strict parent-child note model toward a wiki-style model:

```text
notebook = collection boundary
folder = containment and navigation
note = Markdown-like knowledge unit
link = semantic connection
note.slug = notebook-local address (full path within the notebook; see Phase 2)
localSegment = suffix after the last "/" in note.slug (derivable basename within a folder)
id = stable internal identity
```

The most important shift is:

```text
from tree-based knowledge (note parent as containment)

to folder-contained, link-connected wiki knowledge (note parent removed; folders replace it)
```

## Key Identity Rules

### Internal ID

The internal ID remains the stable technical identity.

It is used for:

- database references
- migrations
- synchronization
- conflict resolution
- internal APIs where stability matters

Example:

```text
n1478
```

### Note slug (persisted)

**`note.slug`** is the single persisted address for a note **within its notebook**: a path-like string built from the folder location (if any) plus a slugified **basename** derived from the note **`title`** (or equivalent). It is the notebook-local full path, not only the last segment.

Examples:

```text
douyara
japanese/vocabulary/douyara
```

For **existing data** migrated from the former parent-note containment shape, each derived folder’s **`name`** equals the **title** of the parent note that defined that container (unchanged from Phase 1).

### Local segment (derived, not a column)

The **basename** within a folder is **derivable** from **`note.slug`**: the substring after the last `/`, or the whole string when there is no `/`. Use it where you need “filename in this folder” behavior (for example export filenames or disambiguation), without persisting a second note column.

File and folder slug segments should be generated with the Java library `com.github.slugify:slugify`. Doughnut should use this as the standard slugifier rather than maintaining separate local slug rules.

**Folder `slug`** stores the folder’s notebook-local full path. Its basename is derived from the folder **`name`**, not chosen as a separate arbitrary string (except explicit rename flows that re-slugify). The **basename** part of **`note.slug`** follows the same slugify rules from the note **`title`** (or equivalent), unless a dedicated slug override exists (for example import frontmatter in a later phase).

### Folder slug (persisted)

Folder **`slug`** is the folder’s path segment chain within the notebook. It is combined with the note basename when building **`note.slug`** for foldered notes.

### Uniqueness rule

Persisted invariant for notes:

```text
unique(notebook_id, slug)
```

Folder-scoped “same title in the same folder” behavior is enforced because two notes in the same folder cannot share the same **`note.slug`** (same folder path prefix implies the same basename would collide).

The display title does not need to be unique.

---

# Phase 1 — Introduce Folder

## Goal

Introduce `Folder` as a first-class model before introducing note file slugs.

This is necessary because note slugs are folder-scoped in the final model.

## Rationale

The old `parent` relationship currently carries too many meanings:

```text
parent = containment + navigation + semantic relationship + identity context
```

The new model separates these concerns:

```text
folder = containment and navigation
links = semantic relationship
id = stable identity
fileSlug = local file identity
```

## Model Additions

Introduce:

```text
Folder
  id
  notebookId
  parentFolderId optional
  name
  createdAt
  updatedAt
```

At this phase, folder slug paths may be deferred to Phase 2.

## Migration Strategy

Initially, folders can mirror the existing parent-note structure. For every backfilled folder that represents a former “parent holds children” node, set **`folder.name`** to that parent note’s **`title`** (the same string as `note.title` on the parent). When Phase 2 adds **`folder.slug`**, it stores the folder’s notebook-local full path; its basename is **slugify(`folder.name`)** per `ongoing/doughnut_wiki_architecture_north_star.md`.

Old model:

```text
Parent Note
  Child Note A
  Child Note B
```

Intermediate model:

```text
Folder: Parent Note
  Child Note A
  Child Note B
```

Each note receives a `folderId` derived from its current parent position.

## Expected Result

After this phase:

- folders exist as first-class records
- notes can be assigned to folders
- current parent-child containment can be represented as folder containment
- the UI can start rendering folder-based navigation
- old parent references may still exist for compatibility

## Non-Goals

This phase does not need to:

- remove parent notes
- introduce final slugs
- convert relationship notes
- support Obsidian export
- support full wiki-link parsing

## Status and implementation notes

Phase 1 is **complete** in the codebase.

- **Persistence:** `folder` rows (`notebook_id`, optional `parent_folder_id`, `name`, timestamps) and optional `note.folder_id`; migrations include `V300000148__create_folder.sql` and `V300000149__note_folder_backfill.sql`. The backfill derives folders from parent notes that have children (folder `name` equals parent `title`), nests folders to mirror the note tree, assigns child notes to the folder derived from each note’s parent, and leaves head notes and parents without a derived folder with null `folder_id` where applicable.
- **Domain:** `Folder` entity and repository; `Note.folder` mapping.
- **Keeping folders aligned with the tree:** `NoteChildContainerFolderService` finds or creates the child-container folder for a parent note (name matches that parent’s title, within the correct notebook and folder hierarchy). It runs when creating a child note and after note moves (`NoteMotionService`), including cross-notebook subtree moves and promoting a note to top level (new head has no folder; descendants follow the updated parent chain).
- **Behavior:** `Note.parent` remains the source of truth for navigation, ordering, and creation/move semantics users see today; folder data is parallel containment for Phase 2 onward. The current sibling order concept is part of this legacy tree behavior and should be removed in a relatively late cleanup phase, after replacement navigation and meaningful ordering mechanisms are stable.

---

# Phase 2 — Introduce Slug Paths

## Goal

Introduce **folder slugs** and **note slugs** where both **`folder.slug`** and **`note.slug`** are persisted notebook-local **full path** addresses (path-like strings within the notebook). Do **not** add `notebook.slug`; notebook endpoints continue to identify notebooks by internal ID. Do **not** persist separate `file_slug` and `full_path` columns on `Note`.

## Rationale

The frontend should eventually refer to notes by location-style address rather than internal ID.

Within a notebook:

```text
basename = slugified note title (or equivalent), unique among siblings in the same folder after collision handling

note.slug = basename                           -- note at notebook root (no folder)
note.slug = folder.slug + "/" + basename     -- foldered note
```

The **basename** within a folder is **derivable** from **`note.slug`** as the substring after the last `/`, or the whole string when there is no `/`.

## Model Additions

### Folder

Add:

```text
slug
```

**`folder.slug`** stores the notebook-local full path. Its basename is produced from the folder **`name`** with the standard slugifier, with sibling collision handling before prefixing it with the parent folder’s **`slug`**.

### Note

Add:

```text
slug
```

**`note.slug`** stores the notebook-local address as above. One column replaces the older split between “file slug” and “full path.”

## Slug Generation

Generate folder and note basenames with `com.github.slugify:slugify`.

The same slugifier should be used anywhere Doughnut derives a slug or basename, including migrations, note creation, folder creation, import, and export.

**Folder:** compute **basename** from `folder.name`, apply collision handling within sibling folders, then set **`folder.slug`** from the parent **`folder.slug`** and basename (or basename only at root). **Note:** compute **basename** from the note **`title`** (or the field that defines the filename), apply collision handling within the target folder, then set **`note.slug`** from **`folder.slug`** and **basename** as in the rationale.

## Uniqueness Constraints

Add for notes:

```text
unique(notebook_id, slug)
```

Folder-scoped basename uniqueness follows: two notes in the same folder cannot share the same **`note.slug`** when they share the same path prefix.

For folders:

```text
unique(notebook_id, slug)
```

## Frontend Rule

The frontend should resolve notes by slug path, not by internal ID.

Example URL shapes (exact encoding is an implementation detail):

```text
/notebooks/:notebookId/... path segments matching note.slug ...
```

The frontend may still use internal IDs behind the scenes after route resolution.

Support an additional **local segment** route for unambiguous accessible notes (basename only):

```text
/notes/:localSegment
```

This route is mostly to make E2E tests easier. It resolves only when exactly one note’s **basename** (derived **`note.slug`**) is visible to the current user among accessible notes, so it does not require a global unique database index.

## Move Behavior

When a note moves to another folder:

```text
move(noteId, newFolderId)
  recompute basename in the target folder (same as today’s title semantics; resolve collisions)
  recompute note.slug from new folder.slug + basename (or basename only at root)
  check unique(notebook_id, note.slug)
  update note.folderId
  optionally create redirect from old note.slug to new note.slug
```

## Expected Result

After this phase:

- folders have a single **`slug`** column holding the notebook-local full path
- notes have a single **`slug`** column holding the notebook-local full path
- frontend note references can move away from internal IDs
- note lookup by notebook ID + slug path is possible
- note lookup by basename alone is possible when it is unambiguous among notebooks the user can access
- moving a note requires uniqueness validation for the recomputed **`note.slug`**

## Non-Goals

This phase does not need to:

- remove the note parent field from schema or APIs (that is a later phase, after folders own placement)
- convert relationship notes
- parse all wiki links
- support Obsidian import/export

---

# Phase 3 — Move Head Note Content to Notebook

## Goal

Remove the conceptual dependency on the head note by moving its content into the notebook.

## Rationale

The head note currently represents both:

```text
notebook identity
notebook content
```

In the final model, the notebook owns its own content.

Old model:

```text
Notebook
  Head Note
    First Layer Note A
    First Layer Note B
```

New model:

```text
Notebook
  content
  folders
  notes
```

## Model Changes

Add to notebook:

```text
content
```

Possibly also:

```text
properties
```

## Migration Strategy

For each notebook:

1. Read the current head note content.
2. Copy or move the content to `Notebook.content`.
3. Preserve the head note ID during the transition if needed.
4. Stop creating new head notes.
5. Update UI to display notebook content from the notebook itself.

## Export Implication

When exporting to Markdown, notebook content may become:

```text
_index.md
```

or:

```text
README.md
```

This should be export configuration, not an internal note model requirement.

## Expected Result

After this phase:

- notebook has its own content
- UI can display and edit notebook content directly
- first-layer notes no longer need to be children of a head note
- new notebooks do not require head notes
- legacy head notes may still exist temporarily for compatibility

---

# Phase 4 — Remove Note Parent (Folders Replace Containment)

## Goal

**Remove the note parent concept** from the model after containment has been migrated into folders. The target is not “optional parent”—it is **no parent-note field** for structure; folders are the sole mechanism for where a note lives.

## Rationale

Once folders mirror former tree placement (including migrated relation-note layouts), parent-note edges duplicate folder semantics. Retaining an optional `parentNoteId` would keep two competing stories for containment.

Final distinction:

```text
folder = where the note lives (replaces parent-note containment)
link = what the note means
(note parent as a system field — removed)
```

If users want “semantic parent” for reading, it belongs in content (links, frontmatter), not as a structural parent pointer.

## Model Change

Remove the note-to-parent association used for containment and navigation from schema, APIs, and UI (exact steps depend on prior phases: migration must have assigned `folderId` and, where applicable, converted old relationship-note trees into folder + links).

A valid note has:

```text
notebookId
folderId optional or required depending on root-folder policy
(no parentNoteId — removed, not optional)
```

## UI Changes

The note tree/navigation should be based on folders only, not parent-note hierarchy.

Notes can appear:

- directly inside a folder
- directly inside the notebook root folder
- linked from map notes
- linked from journal notes
- linked from relationship notes

## Expected Result

After this phase:

- the product model has no structural parent note; placement is folder-based only
- note creation does not offer or require a parent note for containment
- old parent references are gone from persisted model and navigation
- any “semantic parent” is expressed only via links or properties in content, not DB parent edges

---

# Phase 5 — Convert Relationship Notes into Normal Notes

## Goal

Convert special structured relationship notes into ordinary Markdown-like notes.

## Current Relationship Note

The current relationship note has:

```text
parent
 target
link type
optional details
```

## Final Relationship Note

A relationship note becomes a normal note under a folder.

It expresses the relationship using links and content.

Example:

```markdown
---
type: relationship
relation: confused-with
source: "[[〜にはあたらない]]"
target: "[[〜てたまらない]]"
---

# 〜にはあたらない vs 〜てたまらない

[[〜にはあたらない]] is often confused with
[[〜てたまらない]].

## Difference

...
```

## Naming Rule

Relationship notes should have shorter, readable names.

Example:

```text
niwa-ataranai-vs-te-tamaranai.md
```

Use the same slugified naming style for migrated and newly created relationship notes. Keep internal IDs separate from filenames.

## Expected Result

After this phase:

- relationship notes are ordinary notes
- relationship fields are represented in content and/or frontmatter
- relationship notes have folder locations and note slugs (`note.slug` as full path)
- old relationship-note-specific behavior is deprecated
- relationships become portable to Obsidian-style Markdown

---

# Phase 6 — Move a Folder

## Goal

Support **moving or reparenting a folder** within a notebook: change `parentFolderId` (including moving to notebook root) while keeping **`folder.slug`** and every affected **`note.slug`** consistent with the path model from Phase 2.

## Rationale

Phase 2 establishes **`folder.slug`** and **`note.slug`** as notebook-local full paths and defines **note** move with slug recomputation. A **folder** move is the subtree case: the moved folder’s slug changes, so every **descendant folder** and every **note** under that folder must get updated slugs (or the operation fails if uniqueness would break).

Final distinction from note-only move:

```text
note move = one note, new folder, recompute that note’s slug
folder move = one folder, new parent, recompute that folder’s slug and all descendant folder and note slugs
```

## Model and Validation

- Update the moved folder’s **`parentFolderId`** (or clear it for root) and recompute its **`folder.slug`** with the same slugify and sibling collision rules as folder creation.
- Recursively update descendant **`folder.slug`** values and all **`note.slug`** values whose path prefix belonged to the old location.
- Enforce **`unique(notebook_id, slug)`** on folders and notes after the move; reject or surface conflicts the same way as note move (Phase 2), without silent merges.

## Non-Goals

- Parsing wiki links in content (Phase 7)
- Folder configuration templates (Phase 8)
- Obsidian export/import (later phases)

## Expected Result

After this phase:

- users (or the supported API) can move a folder to another parent or to the notebook root
- descendant paths remain valid and addressable; broken partial updates are not left behind
- folder move is the subtree counterpart to single-note move, not a duplicate of Phase 2 but an extension of the same slug invariants

---

# Phase 7 — Add Wiki-Link Parser and Link Index

## Goal

Make wiki-style links in note content first-class.

## Link Syntax

Support:

```markdown
[[note title]]
[[note title|display text]]
[[folder/path/note title|display text]]
```

## Rationale

In the final model, links are the primary semantic connection mechanism.

Content should become the source of truth for relationships.

Derived indexes support efficient navigation and queries.

## Derived Indexes

Introduce derived indexes such as:

```text
OutgoingLinkIndex
  sourceNoteId
  targetNoteId optional
  targetPath
  displayText optional
  resolvedStatus
```

```text
BacklinkIndex
  targetNoteId
  sourceNoteId
```

The exact physical model can vary, but the architectural rule is:

```text
link index is derived
note content is source of truth
```

## Resolution Rules

A link may resolve by:

1. full path within notebook
2. relative path from current folder
3. file slug within current folder
4. unique file slug within notebook, if unambiguous
5. unresolved link placeholder

## Expected Result

After this phase:

- Doughnut can parse wiki links from note content
- Doughnut can show outgoing links
- Doughnut can show backlinks
- unresolved links can be preserved
- graph view can use derived link data
- relationship notes can be understood through normal link parsing

---

# Phase 8 — Add Folder Configuration Behavior

## Goal

Allow folders to define workflow behavior and defaults.

## Rationale

Folders are operational containers. They can influence how notes are created, displayed, migrated, or exported.

Examples:

- journal folder uses date-based title pattern
- source folder uses source-note template
- relationship folder uses relationship-note template
- map folder uses map-note template

## Folder Config Examples

```yaml
template: daily-note
defaultProperties:
  type: journal
titlePattern: "{{date}}"
slugPattern: "{{date}}"
```

For relationship notes:

```yaml
template: relationship-note
defaultProperties:
  type: relationship
slugPattern: "{{source}}-vs-{{target}}"
```

For migration-only behavior:

```yaml
migration:
  addDateToTitle: true
  dateSource: createdAt
  titlePattern: "{{date}} {{oldTitle}}"
```

## Important Distinction

Separate stable folder behavior from temporary migration behavior.

```text
stable config = long-term product behavior
migration config = temporary transformation rule
```

## Expected Result

After this phase:

- folders can provide note creation defaults
- folders can provide templates
- journal folders can generate date-based names
- migration-specific folder rules can support cleanup work
- folder config remains operational, not semantic truth

---

# Phase 9 — Export to Obsidian Markdown

## Goal

Export Doughnut notebooks as Obsidian-compatible Markdown folder trees.

## Export Shape

Example:

```text
Doughnut Notebook/
  _index.md

  Journal/
    2026/
      2026-04-28.md

  Notes/
    douyara.md
    sekijitsu.md

  Relationships/
    niwa-ataranai-vs-te-tamaranai.md

  Maps/
    pkm-design.md
```

## Note Export

Each note exports as a file named from the **basename** (local segment of `note.slug`), for example:

```text
douyara.md
```

With frontmatter:

```markdown
---
id: n1478
title: どうやら
slug: douyara
type: knowledge
created: 2021-05-17
updated: 2026-04-28
aliases:
  - どうやら
---

# どうやら

...
```

## Link Export

Prefer the default note-title link format:

```markdown
[[どうやら]]
```

If the target needs disambiguation, folder-qualified links may be allowed:

```markdown
[[japanese/vocabulary/どうやら]]
```

## Notebook Content Export

Notebook content exports as either:

```text
_index.md
```

or:

```text
README.md
```

Recommended default:

```text
_index.md
```

## Expected Result

After this phase:

- exported notebooks can be opened in Obsidian
- folder structure is preserved
- note content is Markdown
- links are Obsidian-style wiki links
- metadata is represented as frontmatter
- relationship notes are normal Markdown files

---

# Phase 10 — Import and Round Trip from Obsidian

## Goal

Support importing Obsidian-style Markdown folders into Doughnut, including re-import of exported notebooks.

## Import Priority

Use this priority when importing notes:

```text
frontmatter id > generated id
frontmatter slug > filename slug
frontmatter title > first H1 > filename title
folder path > default folder
```

## Import Behavior

The importer should:

- read Markdown files
- parse frontmatter
- create folders from folder paths
- create or update notes
- preserve IDs when available
- preserve slugs when possible
- parse wiki links
- rebuild link indexes
- preserve unresolved links

## Conflict Detection

Detect and report:

```text
same id, changed full path
same full path, different id
duplicate basename in same folder (same `note.slug` prefix)
missing linked target
renamed file
changed title
changed slug
changed folder path
deleted note
```

## Round Trip Goal

The following workflow should become possible:

```text
Doughnut export -> Obsidian edit -> Doughnut import
```

Doughnut should preserve stable identity through frontmatter IDs where possible.

## Expected Result

After this phase:

- Doughnut can import Obsidian-style folders
- Doughnut can re-import its own exported notebooks
- external edits can be merged or reported as conflicts
- file/folder movement can be detected
- link indexes can be rebuilt after import

---

# Phase 11 — Remove Legacy Assumptions

## Goal

Remove old model assumptions after the new model is stable.

## Legacy Concepts to Remove or Isolate

Remove dependency on:

```text
note parent field for containment (folders replace it)
current sibling order concept from the parent-child note tree
head note as notebook root
relationship note as special hidden structure
tree-only navigation
note-ID-based frontend routing as the normal note path
```

## Final Architecture

The final model should be:

Folder **`slug`** is the folder’s notebook-local full path. Its basename is derived from **`name`**. For legacy-derived folders, **`name`** matches the former container parent note’s **`title`** until the user renames the folder.

```text
Notebook
  id
  title
  content
  config

Folder
  id
  notebookId
  parentFolderId optional
  slug   (notebook-local full path)
  name
  config

Note
  id
  notebookId
  folderId optional
  slug
  title
  content
  properties
  (no parentNoteId — removed from final model)

LinkIndex
  sourceNoteId
  targetNoteId optional
  targetPath
  displayText optional
  resolvedStatus
```

## Expected Result

After this phase:

- note parent is absent from the model; folders replace parent-note containment
- head note is gone from the core model
- relationship notes are normal notes
- folder structure handles containment
- wiki links handle semantic connection
- frontend uses note slug/path-based addressing under notebook-ID endpoints
- internal note ID remains stable but hidden from normal user-facing routes
- Obsidian export/import is part of the architecture, not an afterthought

---

# Phase Order Summary

```text
1. Introduce folder
2. Introduce slug paths
3. Move head note content to notebook
4. Remove note parent (folders replace containment)
5. Convert relationship notes
6. Move a folder
7. Add wiki-link parser and link index
8. Add folder config behavior
9. Export to Obsidian Markdown
10. Import and round trip from Obsidian
11. Remove legacy assumptions
```

## Dependency Summary

```text
folder
  -> note.slug (notebook-local full path)
    -> notebook content without head note
      -> remove note parent (folders own placement)
        -> relationship notes as normal notes
          -> move a folder (subtree slug updates)
            -> wiki-link parser and link index
              -> folder config
                -> Obsidian export
                  -> Obsidian import / round trip
                    -> legacy cleanup
```

## Final Architectural Intention

Doughnut should become a wiki-style knowledge system that preserves its own stronger internal model while remaining friendly to Obsidian and plain Markdown.

The final principle is:

```text
Doughnut owns stable identity and richer behavior.
Markdown owns portability.
Folders own containment (replacing note-parent containment).
Links own meaning.
```
