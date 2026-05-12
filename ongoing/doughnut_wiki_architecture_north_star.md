# Doughnut Wiki Roadmap

## Purpose

This roadmap describes the intended architecture of Doughnut as a wiki-style, markdown-first note model.

The goal is to align with common personal-knowledge patterns:

- Notes are Markdown-like knowledge units.
- Folders provide storage and navigation structure.
- Links provide knowledge relationships.
- **Stable note ids** are the canonical identity for URLs, APIs, and references.
- Notebooks are collection boundaries with their own name and configuration.

## Design Direction

Doughnut moves away from a strict parent-child note tree toward a wiki-like model.

The **final state removes the note parent concept from the product model.** Containment and navigation use **folders**. Notes are placed via `folderId` (or notebook root when absent); there is **no** persisted structural parent-note column or FK on `note`.

Historical Flyway scripts may still mention `note.parent_id` for one-time backfills. **As implemented after Phase 7:** structural containment is folder-only.

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
config = behavior and defaults (see notebook/folder indexContent and notebook row)
```

**Notebook root is first-class.** Notes may have **`folderId` absent** to live at notebook root. The product does **not** introduce a hidden default top-level folder that holds all notes; that would duplicate the notebook boundary.

## Final Conceptual Model

### Notebook

A notebook is the top-level boundary of a knowledge collection.

A notebook may contain:

- notebook name
- optional short plain-text **description** (settings message, separate from note body)
- notebook-level configuration (settings and API-facing fields distinct from index body text, where the product keeps them)
- optional **indexContent** Markdown (landing page body + scoped configuration frontmatter)
- folders
- notes

The notebook keeps its own name. **Notebook page** body content comes from notebook-owned **`indexContent`** rather than a special note.

#### Shape

```text
Notebook
  id
  name
  description (optional short plain text)
  config
  indexContent optional (Markdown body, including optional YAML frontmatter)
  createdAt
  updatedAt
```

#### Container index content (notebook scope and folder scope)

**Index content** is Markdown stored on a notebook or folder and used as that container's **landing page** and **scoped configuration carrier**.

- **Notebook index content** — Markdown stored on the notebook row as **`indexContent`**.
- **Folder index content** — Markdown stored on the folder row as **`indexContent`**.

Notebook and folder pages edit this content directly. It is not addressable as **`/d/n/:noteId`**, does not participate in backlinks as a normal note, and does not create a note row.

**Scoped configuration in frontmatter** — Leading YAML on container `indexContent` may hold **product-defined keys** for defaults and behavior in that scope (e.g. **`title_pattern`** for note-title templates, **`question_generation_instruction`** for AI prompts). The Markdown remains the portable source of truth. **Predefined property rows** in the UI apply to notebook/folder index editors so normal notes are not overloaded with folder/notebook admin fields.

**Empty content** — If `indexContent` is absent or blank, the notebook or folder page still shows the editor. The first persist writes the container's own `indexContent` field.

**Reserved note title** — **`index`** is reserved for the legacy migration boundary and may not be used as a note title after the migration. Legacy notes that previously carried index content are renamed to **`index_to_be_deleted`** during migration so users can inspect or delete them without reactivating the old convention.

**Navigation (product)** — A **folder page** is a first-class route (not only a sidebar selection). Opening a folder from the sidebar navigates to that page and **does not** keep a separate “active note” for that context: **container page** (notebook or folder) vs **note page** is mutually exclusive at the routing level. Sidebar chrome may still reflect location (e.g. folder containing the open note) without contradicting that rule.

**Breadcrumbs** — Each **folder** segment in the breadcrumb trail links to that folder’s **folder page** (same destination as choosing that folder in the sidebar), not only a structural highlight.

**Sidebar scroll** — When a **folder page** is open, the folder tree **scrolls** (or otherwise scrolls into view) so the **active folder** row is visible without manual scrolling.

### Folder

A folder is a storage and navigation container (like a file tree in a notebook). It organizes notes operationally but is not the main semantic relationship between notes.

#### Shape

```text
Folder
  id
  notebookId
  parentFolderId optional
  name
  indexContent optional (Markdown body, including optional YAML frontmatter)
  createdAt
  updatedAt
```

Sibling folders under the same parent are distinguished by **name** within the notebook (enforced in persistence).

**Folder-scoped defaults** — Behavioral defaults for notes in that folder (templates, title patterns, question instructions, etc.) are **not** a separate persisted **`folder.config`** blob in the target model: they live in the **folder `indexContent` frontmatter** (and inherit from parent folders or notebook index content when policy defines inheritance).

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
  content (Markdown body, including optional YAML frontmatter)
  properties (derived editing view of content frontmatter)
  createdAt
  updatedAt
```

Listing and card surfaces use **title** (and ids internally); there is no separate derived **short preview** field on topology or wire DTOs—the body is **content** only.

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
- **`index` is a reserved note title** after the container `indexContent` migration.
- **No persisted path slugs** — The product model does **not** keep **`note.slug`** or **`folder.slug`** columns, **`unique(notebook_id, slug)`**, or path strings as canonical identity on notes or folders. Earlier migration phases may have used those fields; they are intentionally absent from this target architecture.

## Import and export

- Portable Markdown export (when offered) may derive filenames from note **title** or from frontmatter fields a future importer understands.
- Frontmatter is user-owned: keys like `title`, `alias`, or import-specific metadata are preserved as scalar fields where the parser allows.

## Principles

1. **Single structural truth** — Folder placement and wiki links are orthogonal; folders do not replace semantic links.
2. **Stable ids** — Prefer note id everywhere the product must not break when titles change; container index content is addressed through notebook/folder ids, not note titles.
3. **Markdown as source of truth** — Body and leading YAML frontmatter carry portable meaning; notebook/folder **`indexContent`** frontmatter carries scoped defaults where the product defines keys.
4. **Notebook boundary** — Sharing attaches to notebooks; **notebook-level** settings may live on the notebook row, while **scoped creation defaults and landing content** for notebook root and each folder concentrate in container **`indexContent`**.
