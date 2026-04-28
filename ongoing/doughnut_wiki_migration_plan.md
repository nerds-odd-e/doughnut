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
fileSlug = local filename identity
fullPath = notebook-local address
id = stable internal identity
```

The most important shift is:

```text
from tree-based knowledge

to folder-contained, link-connected wiki knowledge
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

### File Slug

The file slug is the note's local filename-style identity.

Example:

```text
douyara
```

The same slugified style is used for migrated notes and newly created notes. Internal IDs stay separate from slugs.

File and folder slugs should be generated with the Java library `com.github.slugify:slugify`. Doughnut should use this as the standard slugifier rather than maintaining separate local slug rules.

**Folder slugs** are derived from the folder **`name`**, not chosen as a separate arbitrary string (except explicit rename flows that re-slugify). **Note file slugs** are derived from the note **`title`** (or equivalent) in the same way.

For **existing data** migrated from the former parent-note containment shape, each derived folder’s **`name`** equals the **title** of the parent note that defined that container.

### Folder Path

The folder path represents the note's location.

Example:

```text
japanese/vocabulary
```

### Full Path

The full path is the notebook-local address of a note.

Example:

```text
japanese/vocabulary/douyara
```

### Uniqueness Rule

The final uniqueness rule is:

```text
unique(notebook_id, folder_id, file_slug)
```

A denormalized full-path index may also be maintained:

```text
unique(notebook_id, full_path)
```

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

At this phase, folder slug and full path may be deferred to Phase 2.

## Migration Strategy

Initially, folders can mirror the existing parent-note structure. For every backfilled folder that represents a former “parent holds children” node, set **`folder.name`** to that parent note’s **`title`** (the same string as `note.title` on the parent). When Phase 2 adds **`folder.slug`**, it is **slugify(`folder.name`)** per `ongoing/doughnut_wiki_architecture_north_star.md`.

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

---

# Phase 2 — Introduce Slugs and Full Paths

## Goal

Introduce notebook slugs, folder slugs, note file slugs, and note full paths.

## Rationale

The frontend should eventually refer to notes by location-style address rather than internal ID.

The address is:

```text
notebookSlug + folderPath + fileSlug
```

The note's slug should not include the folder path. Instead:

```text
fileSlug = local filename
folderPath = location
fullPath = folderPath + '/' + fileSlug
```

## Model Additions

### Notebook

Add:

```text
slug
```

### Folder

Add:

```text
slug
fullPath
```

`slug` is always produced from the folder **`name`** with the standard slugifier. Collision handling and uniqueness apply at persistence time (see Uniqueness Constraints below).

### Note

Add:

```text
fileSlug
fullPath
```

## Slug Generation

Generate notebook, folder, and note file slugs with `com.github.slugify:slugify`.

The same slugifier should be used anywhere Doughnut derives a file or folder slug, including migrations, note creation, folder creation, import, and export.

**Folder:** `folder.slug` ← slugify(`folder.name`). **Note file:** `fileSlug` ← slugify(note title or the field that defines the filename), unless a dedicated slug override exists (e.g. import frontmatter).

## Uniqueness Constraints

Add:

```text
unique(notebook_id, folder_id, file_slug)
unique(notebook_id, full_path)
```

For folders:

```text
unique(notebook_id, parent_folder_id, folder_slug)
unique(notebook_id, folder_full_path)
```

## Frontend Rule

The frontend should resolve notes by slug/path, not by internal ID.

Example URL:

```text
/notebooks/:notebookSlug/:folderPath/:fileSlug
```

Example:

```text
/notebooks/doughnut-wiki/japanese/vocabulary/douyara
```

The frontend may still use internal IDs behind the scenes after route resolution.

Support an additional note-slug-only route for unambiguous accessible notes:

```text
/notes/:fileSlug
```

This route is mostly to make E2E tests easier. It resolves only when exactly one note with that file slug is visible to the current user, so it does not require a global unique database index.

## Move Behavior

When a note moves to another folder:

```text
move(noteId, newFolderId)
  check unique(newFolderId, note.fileSlug)
  update note.folderId
  recompute note.fullPath
  optionally create redirect from old fullPath to new fullPath
```

## Expected Result

After this phase:

- notebooks have slugs
- folders have slugs and full paths
- notes have file slugs and full paths
- frontend note references can move away from internal IDs
- note lookup by notebook + full path is possible
- note lookup by file slug alone is possible when it is unambiguous among notebooks the user can access
- moving a note requires uniqueness validation in the target folder

## Non-Goals

This phase does not need to:

- make parent optional
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

# Phase 4 — Make Parent Optional

## Goal

Make note parenthood optional and remove parent note as the required containment mechanism.

## Rationale

Once folders exist, notes do not need parent notes for containment.

Final distinction:

```text
folder = where the note lives
link = what the note means
parent = optional semantic relation, if still needed
```

## Model Change

Change note parent reference from required to optional, or deprecate it as a containment mechanism.

A note should be valid with:

```text
notebookId
folderId optional or required depending on root-folder policy
parentNoteId optional
```

## UI Changes

The note tree/navigation should be based on folders, not parent-note hierarchy.

Notes can appear:

- directly inside a folder
- directly inside the notebook root folder
- linked from map notes
- linked from journal notes
- linked from relationship notes

## Expected Result

After this phase:

- notes can exist without parent notes
- note creation does not require a parent note
- containment is folder-based
- old parent references no longer control navigation
- semantic parenthood, if needed, can be represented by links or properties

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
- relationship notes have folder locations and file slugs
- old relationship-note-specific behavior is deprecated
- relationships become portable to Obsidian-style Markdown

---

# Phase 6 — Add Wiki-Link Parser and Link Index

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

# Phase 7 — Add Folder Configuration Behavior

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

# Phase 8 — Export to Obsidian Markdown

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

Each note exports as:

```text
fileSlug.md
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

# Phase 9 — Import and Round Trip from Obsidian

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
duplicate fileSlug in same folder
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

# Phase 10 — Remove Legacy Assumptions

## Goal

Remove old model assumptions after the new model is stable.

## Legacy Concepts to Remove or Isolate

Remove dependency on:

```text
required parent note
head note as notebook root
relationship note as special hidden structure
tree-only navigation
ID-based frontend routing as the normal path
```

## Final Architecture

The final model should be:

Folder **`slug`** is derived from **`name`**. For legacy-derived folders, **`name`** matches the former container parent note’s **`title`** until the user renames the folder.

```text
Notebook
  id
  slug
  title
  content
  config

Folder
  id
  notebookId
  parentFolderId optional
  slug
  fullPath
  name
  config

Note
  id
  notebookId
  folderId optional
  fileSlug
  fullPath
  title
  content
  properties

LinkIndex
  sourceNoteId
  targetNoteId optional
  targetPath
  displayText optional
  resolvedStatus
```

## Expected Result

After this phase:

- parent note is no longer required
- head note is gone from the core model
- relationship notes are normal notes
- folder structure handles containment
- wiki links handle semantic connection
- frontend uses slug/path-based addressing
- internal ID remains stable but hidden from normal user-facing routes
- Obsidian export/import is part of the architecture, not an afterthought

---

# Phase Order Summary

```text
1. Introduce folder
2. Introduce slugs and full paths
3. Move head note content to notebook
4. Make parent optional
5. Convert relationship notes
6. Add wiki-link parser and link index
7. Add folder config behavior
8. Export to Obsidian Markdown
9. Import and round trip from Obsidian
10. Remove legacy assumptions
```

## Dependency Summary

```text
folder
  -> fileSlug and fullPath
    -> notebook content without head note
      -> optional parent
        -> relationship notes as normal notes
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
Folders own containment.
Links own meaning.
```
