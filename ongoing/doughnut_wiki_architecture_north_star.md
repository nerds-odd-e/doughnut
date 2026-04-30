# Doughnut Wiki Roadmap

## Purpose

This roadmap describes the intended final architecture of Doughnut after the migration toward an Obsidian-compatible, wiki-style note model.

The goal is not to make Doughnut identical to Obsidian, but to make Doughnut compatible with Obsidian’s mental model:

- Notes are Markdown-like knowledge units.
- Folders provide storage and navigation structure.
- Links provide knowledge relationships.
- Filenames/slugs provide human-facing references.
- Internal IDs remain stable technical identities.
- Notebooks are collection boundaries with their own name and configuration.

## Design Direction

Doughnut will move away from a strict parent-child note tree and toward a wiki-like model.

The **final state removes the note parent concept from the product model.** Containment and navigation that today use parent-child edges migrate into **folders**. Notes are placed via `folderId` only; there is no parallel “parent note” field for structure.

Interim migrations may still read legacy parent columns while backfilling folders. **As implemented:** folders are stored and updated when child notes are created or notes are moved while `Note.parent` still drives observable tree behavior until parent-note containment is removed from the product model. Once existing tree and relationship-note layouts are represented as folders (and links where appropriate), parent-note containment is **replaced by folders**, not weakened into optionality.

The previous model overloaded `parent` with multiple meanings:

```text
parent = containment + meaning + identity + navigation
```

The new model separates these concerns:

```text
notebook = collection boundary
folder = containment and navigation
note = knowledge unit
link = semantic connection
slug = frontend/file-facing reference
id = internal stable identity
config = behavior and defaults
```

## Final Conceptual Model

### Notebook

A notebook is the top-level boundary of a knowledge collection.

A notebook may contain:

- notebook name
- optional short plain-text **description** (settings message, separate from note body)
- notebook-level configuration
- optional `index` note
- folders
- notes

The notebook keeps its own name. Notebook page content comes from an optional ordinary note titled `index`, not from a notebook-owned content field.

#### Final state

```text
Notebook
  id
  name
  description (optional short plain text)
  config
  createdAt
  updatedAt
```

#### Index note

A notebook may have an ordinary root-level note titled `index` with slug `index`.

The `index` note supplies the notebook page body summary when it exists. It is optional: the notebook page resolves **`index`** by slug without assuming absence until that load completes, and editing full body content happens on the normal note show route for that slug. Users create or update the `index` note like any other note. Apart from this notebook-page presentation role, the `index` note is a normal note: stable note ID, title, slug, content, links, backlinks, properties, and export/import behavior.

### Folder

A folder is a storage and navigation container.

Folders are similar to Obsidian folders. They organize notes operationally but should not be treated as the main semantic relationship between notes.

A folder may contain:

- subfolders
- notes
- folder-specific configuration

Folders can be used for workflows such as:

- journal organization
- templates
- source notes
- generated notes
- default properties for notes created inside the folder

#### Final state

```text
Folder
  id
  notebookId
  parentFolderId optional
  slug   (notebook-local full path; basename = segment after last "/", or whole string if no "/")
  name
  config
  createdAt
  updatedAt
```

**Name and slug**

- **`name`** is the human-facing folder label.
- **`slug`** is the notebook-local full path. Its basename is derived from **`name`** using the standard slugifier (`com.github.slugify:slugify`), not set as an independent arbitrary string except where product flows explicitly rename and re-slugify. Nested folder slugs prefix that basename with the parent folder’s **`slug`**.
- For **existing data** migrated from the former parent-note containment model, each derived folder’s **`name`** equals the **title** of the parent note that defined that container (the same value as that note’s `title`).

### Note

A note is the core knowledge unit.

A note belongs to a notebook and may belong to a folder. Notes **do not** have a parent note in the final model—placement is folder-based only.

The note is referred to by **`slug`** in the frontend, not by internal ID.

**Persisted `note.slug`** is the notebook-local **full path**: a path-like string (`segment/segment/...`) within the notebook. At notebook root it may be a single segment (the **basename** derived from the title). When a note is in a folder, **`note.slug`** includes the folder path prefix, for example `folder-segment/title-basename`.

#### Final state

```text
Note
  id
  notebookId
  folderId optional
  slug   (notebook-local full path; basename = segment after last "/", or whole string if no "/")
  title
  content (Markdown details, including optional YAML frontmatter)
  properties (derived editing view of content frontmatter)
  createdAt
  updatedAt
```

Listing and card surfaces use **title** and **slug** path; there is no separate derived **short-details** (or similar) field on topology or wire DTOs—the body is **content** only.

After Phase 6 of the migration plan, note graph sibling retrieval follows the same placement rule: foldered notes get siblings from their folder; root notes with no folder get siblings from the notebook root.

Note properties are represented as leading YAML frontmatter in the note's Markdown content. The Markdown content is the portable source of truth; rich editing surfaces may parse the frontmatter and show it as editable property rows, then serialize changes back into the same leading frontmatter block. The backend does not need a separate property bag to preserve properties, though later indexing or import workflows may derive selected fields from frontmatter when there is a product need.

After the relationship-note migration, **`title`** is required for every note: it cannot be null or empty. Legacy title-less notes are migration input only and must be backfilled before the parent-note model is removed.

### Link

Links are the primary mechanism for expressing knowledge relationships.

A note can refer to another note using wiki-style links.

Example:

```markdown
This idea is related to [[どうやら]].
```

The default link text is the note title. The displayed text may be different from the target title when an alias is useful.

```markdown
[[note title|display text]]
```

Doughnut may index these links for graph view, backlinks, search, and relationship discovery.

#### Phase 5 persisted wiki cache (minimal)

The first persisted shape is table `note_wiki_title_cache`: `id`, source `note_id`, `target_note_id`, and `link_text` (full token inside `[[]]`). Slug and notebook for client surfaces are joined from the target note when building DTOs; they are intentionally not duplicated on the cache row.

#### Final state (longer-term derived index sketch)

```text
LinkIndex
  sourceNoteId
  targetNoteId
  linkText
  targetSlug
  createdAt
```

This richer index (extra display/audit fields) is a directional target for later phases—for example path-based resolution and unresolved-link handling—not the Phase 5 physical table.

The link index is derived from note content. The content remains the source of truth.

## Slug and Identity

Doughnut distinguishes between internal identity and external/frontend identity.

### Internal ID

The internal ID is stable and never changes.

It is used for:

- database references
- synchronization
- internal APIs where appropriate
- long-term identity preservation

Example:

```text
n1478
```

### Slug

The slug is the frontend-facing and file-facing reference.

For **notes**, the persisted **`slug`** is the notebook-local **full path** (see the Note model above). For navigation and exports you may use the **basename** (suffix after the last `/`) as the file-facing filename when a single segment is enough.

It is used for:

- frontend routes
- wiki links
- exported filenames
- Obsidian-compatible references

Examples:

```text
douyara
japanese/vocabulary/douyara
```

File and folder slug segments should be generated with the Java library `com.github.slugify:slugify`. Doughnut should use this as the standard slugifier instead of maintaining separate local slug rules. **Folder `slug`** stores the folder’s notebook-local full path; its basename is produced from the folder **`name`** (same rules as other slugified titles). The **basename** portion of a **note `slug`** comes from the note **`title`** (or equivalent naming field) unless a dedicated rename/slug workflow applies; the **full `note.slug`** prefixes that basename with the folder **`slug`** when the note is foldered.

### Title

The title is human-facing.

It is used for:

- display
- search
- reading
- link aliases
- generated filenames or slugs

Example:

```text
どうやら
```

Invariant after the relationship-note migration:

```text
note.title must be non-empty
```

### Uniqueness Rule

Within the same notebook:

```text
note.slug must be unique (slug is the full notebook-local path)
```

Basenames (local segments) must be unique among notes that share the same folder path prefix, which follows from **`unique(notebookId, slug)`**.

The display title does not necessarily need to be globally unique.

Recommended invariant:

```text
notebookId + note.slug = unique
```

## Frontend Reference Rule

In the frontend, a note is referred to by **`slug`** rather than internal ID.
The containing notebook is identified by internal **notebook ID** in endpoints.

Example route:

```text
/notebooks/:notebookId/... note path matching note.slug ...
```

For a foldered note, `:noteSlug` matches the persisted **`note.slug`** (folder path plus basename). Encode path segments as appropriate for the router.

If a note is inside nested folders, the same rule extends to the full folder path.

A note may also be visited by **basename alone** when it resolves to exactly one note among notebooks the user can access:

```text
/notes/:basename
```

This route is primarily a testing and convenience path. It must not require a global unique index in the database; resolution can reject ambiguous basenames when more than one accessible note matches.

Recommended final decision:

```text
Persisted note.slug is unique within the notebook (full path).
Frontend note references use full note.slug paths when folder-qualified addresses are needed.
Basename-only frontend references are allowed only when unambiguous for the current user.
Internal resolution still maps to stable note ID.
```

## Head Note Removal

The old head note concept is removed.

Previously:

```text
Notebook
  headNote
    child note
    child note
```

Final model:

```text
Notebook
  name
  config
  folders
  notes
    index (optional ordinary root note)
```

The current head note becomes the notebook's optional `index` note. During migration, assume there is no current production data with the title `index`, so the former head note can be renamed to title `index` and slug `index` without colliding with existing user notes.

The notebook keeps its own name separately from the `index` note. The `index` note is otherwise a normal note, and a notebook may have no `index` note at all.

For Obsidian export, the `index` note may be represented as:

```text
index.md
```

If a notebook has no `index` note, export may omit `index.md`.

Internally, the `index` note is modeled as an ordinary note.

## Removal of the Note Parent Concept

In the final model there is **no** note-to-note parent field for containment or navigation.

A note may be:

- inside a folder (the only structural placement)
- linked from other notes
- included in a map note
- connected through backlinks
- related through explicit relationship notes

If something like “semantic parent” is still useful for the reader, it belongs in **content**—links or frontmatter—not as a built-in parent pointer.

Possible representation:

```markdown
---
semanticParent: [[Some Note Title]]
---
```

or simply:

```markdown
Parent idea: [[Some Note Title]]
```

That is ordinary wiki semantics, not the old parent-note hierarchy.

## Folder vs Former Parent

After migration, the distinction is:

```text
folder = where the note lives (replaces parent-note containment)
link = what the note means
(note parent as a system concept — removed)
```

A folder is operational structure. The legacy “parent holds children” tree is expressed as **folder hierarchy + note membership**, not as parent note records.

## Folder Configuration

Folders may have configuration.

Folder configuration can support:

- default template
- default note properties
- slug generation policy
- title generation policy
- journal date rules
- import/export behavior

Example:

```yaml
template: daily-note
defaultProperties:
  type: journal
titlePattern: "{{date}}"
slugPattern: "{{date}}"
```

Long-term folder behavior should remain clean and intentional. Temporary migration properties should not pollute the final domain model.

## Relationship Notes

A relationship note is a normal note.

It lives under a folder and expresses the relationship in content using links.

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

The relationship note itself is a knowledge artifact.

Its title is derived from the relationship itself: source note title, relationship label, and target note title. For migrated old relationship notes, persist that derived title and truncate it so it does not exceed the note title length limit before generating the note slug.

Its filename/slug should be shorter and readable.

Example:

```text
niwa-ataranai-vs-te-tamaranai.md
```

Recommended approach:

```text
use slugified titles for migrated and newly created notes; keep internal IDs separate from slugs
```

## Obsidian Export Model

Doughnut should export to a normal folder of Markdown files.

Example:

```text
Doughnut Notebook/
  index.md

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

Each note may include frontmatter as part of the Markdown file content:

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

Links should prefer the default note-title format:

```markdown
[[どうやら]]
```

## Import Model

When importing from Obsidian-style Markdown:

- filename becomes slug if no explicit slug exists
- frontmatter `id` is used if available
- frontmatter `title` is used if available
- the remaining frontmatter is preserved with the Markdown content
- first heading may become title if no frontmatter title exists
- folder path becomes folder
- wiki links are parsed and indexed
- unresolved links are preserved as unresolved references

Import priority:

```text
frontmatter id > generated id
frontmatter slug > filename slug
frontmatter title > first H1 > filename title
folder path > default folder
```

## Journal Notes

Journal notes should be organized chronologically by folder and filename.

Example:

```text
Journal/
  2026/
    2026-04-28.md
```

Recommended properties:

```yaml
type: journal
date: 2026-04-28
```

Journal order comes from date, not from note parenthood.

A journal note may link to factual notes:

```markdown
Today I thought about [[wiki-subpages|Wikipedia subpages]] and how this affects [[doughnut-wiki-model|Doughnut's wiki model]].
```

## Knowledge Notes

Knowledge notes are not ordered by time.

They are connected through:

- wiki links
- backlinks
- tags
- map notes
- relationship notes
- search
- graph view

Example:

```text
Notes/
  wikipedia-subpages.md
  category-vs-article.md
  note-identity.md
```

## Map Notes

Map notes provide curated navigation.

They replace the need for strict tree-based conceptual organization.

Example:

```markdown
# PKM Design Map

## Identity

- [[note-identity]]
- [[slug-vs-id]]
- [[filename-as-title]]

## Organization

- [[folder-as-storage]]
- [[links-as-meaning]]
- [[category-vs-parent]]
```

Map notes are ordinary notes.

## Ordering

Obsidian does not give notes a built-in semantic order.

Doughnut should support order only where order is meaningful.

Examples:

```text
Journal = chronological order
Course material = curated order
Book notes = source order
Knowledge notes = no required order
```

Ordering can be represented by:

- date
- filename prefix
- explicit frontmatter
- map note sequence
- folder-specific configuration

Example frontmatter:

```yaml
order: 10
```

But order should not be required for ordinary knowledge notes.

The current sibling order concept belongs to the old parent-child note tree. It may remain during migration so existing navigation stays stable, but it should be removed in a relatively late cleanup phase after folder navigation, map notes, and any domain-specific ordering mechanisms have replaced it.

## Compatibility Principles

### Principle 1: Markdown is the portable surface

The exported form should be understandable as plain Markdown.

Doughnut-specific features should degrade gracefully.

### Principle 2: Links carry meaning

Semantic relationships should be expressed through links and note content, not only hidden metadata.

### Principle 3: Folders are operational

Folders may influence templates and workflow, but should not be treated as the single source of knowledge meaning.

### Principle 4: IDs are stable, slugs are usable

Internal IDs protect long-term identity.

Slugs make the system usable in URLs, filenames, and wiki links.

### Principle 5: Content is source of truth where possible

Derived indexes such as backlinks, graph edges, and relationship indexes should be rebuilt from content.

## Open Decisions

### Should note slug include internal ID?

Decision:

```text
No. Use slugified titles for migrated notes and newly created notes.
```

Example:

```text
douyara.md
```

Internal IDs remain stable in the database and frontmatter where needed. Slug collisions are handled by folder-scoped uniqueness and normal conflict resolution, not by embedding IDs in slugs.

### Should the notebook `index` note export as `index.md`?

Decision:

```text
Yes. Export the optional root `index` note as index.md.
```

If the notebook has no `index` note, there is no notebook-introduction file to export.

### Should display title be unique?

Recommended:

```text
No.
```

Only the note **basename** (local segment of **`note.slug`**) needs to be unique within the same folder, which follows from **`notebookId + note.slug`** uniqueness.

## Final Architecture Summary

The final Doughnut architecture should look like this:

```text
Notebook
  owns name
  owns folders
  owns notes
  owns config
  may have an optional ordinary root note named index

Folder
  provides containment
  provides navigation
  may provide behavior/config
  does not define semantic truth

Note
  is a Markdown-like knowledge unit
  has stable internal ID
  has persisted slug (notebook-local full path)
  has human title
  may live in a folder (no note-parent field; folders replace parent-note containment)
  links to other notes

Relationship
  is expressed as note content
  may be indexed
  may have frontmatter
  is no longer a special hidden structure

Link Index
  is derived from note content
  supports backlinks, graph, and navigation
```

The core architectural shift is:

```text
from tree-based knowledge (parent-child note edges)
to folder-contained, link-connected wiki knowledge (parent-note containment removed, not optionalized)
```

## Desired End State

After migration, Doughnut should be able to:

- store and navigate notes using folders only—no note-parent field for containment
- refer to notes in the frontend by slug
- preserve stable internal IDs
- keep notebook names on notebooks while using an optional normal `index` note for notebook page content
- export notebooks as Obsidian-friendly Markdown folders
- import Obsidian-style Markdown folders
- represent relationships as ordinary linked notes
- support chronological journals and unordered factual knowledge in the same notebook
- use folders for templates and workflow behavior
- use links and map notes for semantic organization
