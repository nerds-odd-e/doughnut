# Doughnut Wiki Roadmap

## Purpose

This roadmap describes the intended architecture of Doughnut as an Obsidian-compatible, wiki-style note model.

The goal is not to make Doughnut identical to Obsidian, but to align with Obsidian’s mental model:

- Notes are Markdown-like knowledge units.
- Folders provide storage and navigation structure.
- Links provide knowledge relationships.
- **Stable note ids** are the canonical identity for URLs, APIs, and references.
- Notebooks are collection boundaries with their own name and configuration.

## Design Direction

Doughnut moves away from a strict parent-child note tree toward a wiki-like model.

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
id = internal stable identity (canonical in URLs)
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

#### Shape

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

A notebook may have an ordinary root-level note titled `index` (title compared case-insensitively for resolution).

The index note supplies the notebook page body summary when it exists. The notebook page loads it by **`indexNoteId`** from the notebook API once that id is known. Editing full body content uses the normal note show route **`/d/n/:noteId`**. The index note is otherwise a normal note: stable id, title, content, links, backlinks, properties, and export/import behavior.

### Folder

A folder is a storage and navigation container (similar to Obsidian folders). It organizes notes operationally but is not the main semantic relationship between notes.

#### Shape

```text
Folder
  id
  notebookId
  parentFolderId optional
  name
  createdAt
  updatedAt
```

Sibling folders under the same parent are distinguished by **name** within the notebook (enforced in persistence).

### Note

A note is the core knowledge unit.

A note belongs to a notebook and may belong to a folder. Notes **do not** have a parent note in the final model—placement is folder-based only.

The **canonical web URL** for a note is **`/d/n/:noteId`** (numeric id).

#### Shape

```text
Note
  id
  notebookId
  folderId optional
  title
  content (Markdown details, including optional YAML frontmatter)
  properties (derived editing view of content frontmatter)
  createdAt
  updatedAt
```

Listing and card surfaces use **title** (and ids internally); there is no separate derived **short-details** field on topology or wire DTOs—the body is **content** only.

Note properties are represented as leading YAML frontmatter in the note's Markdown content. The Markdown content is the portable source of truth; rich editing surfaces may parse the frontmatter and show it as editable property rows, then serialize changes back into the same leading frontmatter block.

After the relationship-note migration, **`title`** is required for every note: it cannot be null or empty. Legacy title-less notes are migration input only and must be backfilled before the parent-note model is removed.

### Link

Links are the primary mechanism for expressing knowledge relationships.

A note can refer to another note using wiki-style links, for example:

```markdown
This idea is related to [[どうやら]].
```

The default link text is the note title. The displayed text may differ from the target title when an alias is useful: `[[note title|display text]]`.

Resolved links are indexed (e.g. graph, backlinks, search). Wiki-title cache rows store `target_note_id` and `link_text`; client surfaces use **note id** for navigation.

#### Persisted wiki cache (Phase 5)

Table `note_wiki_title_cache`: `id`, source `note_id`, `target_note_id`, `link_text` (full token inside `[[]]`). Only resolved links are persisted.

## Identity and URLs

- **Internal id** is stable and used for database references, synchronization, and **`/d/n/:noteId`** routes.
- **Titles** and optional YAML keys in frontmatter support human editing and import/export filenames; they are not alternate primary keys in the HTTP API.
- **Folders** are addressed by id in APIs; names are unique among siblings under the same parent folder within a notebook.
- **No persisted path slugs** — The product model does **not** keep **`note.slug`** or **`folder.slug`** columns, **`unique(notebook_id, slug)`**, or path strings as canonical identity on notes or folders. Earlier migration phases may have used those fields; they are intentionally absent from this target architecture.

## Import and export

- Export (e.g. Obsidian zip) may derive filenames from note **title** or from frontmatter fields the importer understands.
- Frontmatter is user-owned: keys like `title`, `alias`, or import-specific metadata are preserved as scalar fields where the parser allows.

## Principles

1. **Single structural truth** — Folder placement and wiki links are orthogonal; folders do not replace semantic links.
2. **Stable ids** — Prefer note id everywhere the product must not break when titles change.
3. **Markdown as source of truth** — Body and leading YAML frontmatter carry portable meaning.
4. **Notebook boundary** — Sharing, assessment, and configuration attach to notebooks.
