# Doughnut Wiki Roadmap

## Purpose

This roadmap describes the intended final architecture of Doughnut after the migration toward an Obsidian-compatible, wiki-style note model.

The goal is not to make Doughnut identical to Obsidian, but to make Doughnut compatible with Obsidian’s mental model:

- Notes are Markdown-like knowledge units.
- Folders provide storage and navigation structure.
- Links provide knowledge relationships.
- Filenames/slugs provide human-facing references.
- Internal IDs remain stable technical identities.
- Notebooks are collection boundaries with their own content and configuration.

## Design Direction

Doughnut will move away from a strict parent-child note tree and toward a wiki-like model.

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

- notebook title
- notebook content
- notebook-level configuration
- folders
- notes

The notebook itself may have content. This replaces the old “head note” concept.

#### Final state

```text
Notebook
  id
  title
  content
  config
  createdAt
  updatedAt
```

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
  slug
  name
  config
  createdAt
  updatedAt
```

### Note

A note is the core knowledge unit.

A note belongs to a notebook and may belong to a folder. A note does not require a parent note.

The note is referred to by slug in the frontend, not by internal ID.
When a note is in a folder, its slug is folder-qualified as `folder_slug/note_title`.

#### Final state

```text
Note
  id
  notebookId
  folderId optional
  slug
  title
  content
  properties
  createdAt
  updatedAt
```

### Link

Links are the primary mechanism for expressing knowledge relationships.

A note can refer to another note using wiki-style links.

Example:

```markdown
This idea is related to [[n1478-douyara|どうやら]].
```

The displayed text may be different from the target slug.

```markdown
[[target-slug|display text]]
```

Doughnut may index these links for graph view, backlinks, search, and relationship discovery.

#### Final state

```text
LinkIndex
  sourceNoteId
  targetNoteId
  linkText
  targetSlug
  createdAt
```

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

It is used for:

- frontend routes
- wiki links
- exported filenames
- Obsidian-compatible references

Example:

```text
n1478-douyara
```

For notes inside folders, the slug includes the folder slug:

```text
folder_slug/note_title
```

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

### Uniqueness Rule

Within the same notebook and folder:

```text
note filename/slug must be unique
```

The display title does not necessarily need to be globally unique.

Recommended invariant:

```text
notebookId + folderId + slug = unique
```

Optional stronger invariant:

```text
notebookId + fullFolderPath + slug = unique
```

## Frontend Reference Rule

In the frontend, a note is referred to by slug rather than internal ID.
The containing notebook is still identified by notebook ID.

Example route:

```text
/notebooks/:notebookId/notes/:noteSlug
```

For a foldered note, `:noteSlug` includes the folder prefix:

```text
folder_slug/note_title
```

If a note is inside nested folders, the same rule extends to the full folder path.

Recommended final decision:

```text
Slugs are unique within the same folder.
Frontend note references use folder-qualified slugs when needed.
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
  content
  folders
  notes
```

The notebook itself owns its top-level content.

For Obsidian export, notebook content may be represented as:

```text
_index.md
```

or:

```text
README.md
```

But internally, it is not modeled as an ordinary note.

## Parent Note Optionality

Notes do not require parent notes.

A note may be:

- inside a folder
- linked from other notes
- included in a map note
- connected through backlinks
- related through explicit relationship notes

But it does not need a parent note.

If semantic parenthood is still useful, it should be represented explicitly, not as storage containment.

Possible representation:

```markdown
---
semanticParent: [[some-note-slug]]
---
```

or simply:

```markdown
Parent idea: [[some-note-slug]]
```

## Folder vs Parent

The final distinction is:

```text
folder = where the note lives
link = what the note means
parent = optional semantic relation, not required containment
```

A folder is operational structure. A parent note, when it exists conceptually, should be represented through links, properties, or relationship notes.

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
source: "[[n23403-niwa-ataranai|〜にはあたらない]]"
target: "[[n10102-te-tamaranai|〜てたまらない]]"
---

# 〜にはあたらない vs 〜てたまらない

[[n23403-niwa-ataranai|〜にはあたらない]] is often confused with
[[n10102-te-tamaranai|〜てたまらない]].

## Difference

...
```

The relationship note itself is a knowledge artifact.

Its filename/slug should be shorter and readable.

Example:

```text
n23943-niwa-ataranai-vs-te-tamaranai.md
```

or:

```text
niwa-ataranai-vs-te-tamaranai.md
```

Recommended approach:

```text
include internal ID in slug if long-term round-trip stability matters
```

## Obsidian Export Model

Doughnut should export to a normal folder of Markdown files.

Example:

```text
Doughnut Notebook/
  _index.md

  Journal/
    2026/
      n3001-2026-04-28.md

  Notes/
    n1478-douyara.md
    n28329-sekijitsu.md

  Relationships/
    n23943-niwa-ataranai-vs-te-tamaranai.md

  Maps/
    pkm-design.md
```

Each note may include frontmatter:

```markdown
---
id: n1478
title: どうやら
slug: n1478-douyara
type: knowledge
created: 2021-05-17
updated: 2026-04-28
aliases:
  - どうやら
---

# どうやら

...
```

Links should prefer slug-based stable references with readable aliases:

```markdown
[[n1478-douyara|どうやら]]
```

## Import Model

When importing from Obsidian-style Markdown:

- filename becomes slug if no explicit slug exists
- frontmatter `id` is used if available
- frontmatter `title` is used if available
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

or with stable ID:

```text
Journal/
  2026/
    n3001-2026-04-28.md
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

Option A:

```text
n1478-douyara.md
```

Pros:

- stable
- easier round trip
- avoids collisions
- good migration safety

Cons:

- less clean in Obsidian

Option B:

```text
douyara.md
```

Pros:

- cleaner
- more Obsidian-like

Cons:

- harder collision handling
- harder long-term identity preservation

Recommended default:

```text
Use ID-prefixed slugs for migrated notes.
Allow clean slugs for newly created notes if uniqueness is safe.
```

### Should notebook content export as `_index.md` or `README.md`?

Option A:

```text
_index.md
```

Pros:

- clearly index-like
- common in static site systems

Option B:

```text
README.md
```

Pros:

- familiar in GitHub/file systems
- natural folder introduction

Recommended:

```text
Use _index.md by default.
Allow configurable export filename.
```

### Should display title be unique?

Recommended:

```text
No.
```

Only filename/slug needs to be unique within the folder.

## Final Architecture Summary

The final Doughnut architecture should look like this:

```text
Notebook
  owns content
  owns folders
  owns notes
  owns config

Folder
  provides containment
  provides navigation
  may provide behavior/config
  does not define semantic truth

Note
  is a Markdown-like knowledge unit
  has stable internal ID
  has frontend/file-facing slug
  has human title
  may live in a folder
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
from tree-based knowledge
to folder-contained, link-connected wiki knowledge
```

## Desired End State

After migration, Doughnut should be able to:

- store notes without requiring parent notes
- refer to notes in the frontend by slug
- preserve stable internal IDs
- export notebooks as Obsidian-friendly Markdown folders
- import Obsidian-style Markdown folders
- represent relationships as ordinary linked notes
- support chronological journals and unordered factual knowledge in the same notebook
- use folders for templates and workflow behavior
- use links and map notes for semantic organization
