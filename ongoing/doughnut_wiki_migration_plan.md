# Doughnut Wiki Migration Plan

## Purpose

This document proposes a phased migration plan for moving Doughnut toward the final wiki-style, Obsidian-compatible architecture.

The goal is to implement ongoing/doughnut_wiki_architecture_north_star.md, phase by phase. With the learning from implementing each phase, the north star file will be updated to reflect the latest understanding.

The plan intentionally excludes the safety baseline phase. It starts directly with architectural migration work.

## Target Direction

Doughnut will move from a strict parent-child note model toward a wiki-style model:

```text
notebook = collection boundary
notebook.name = notebook-owned display name
folder = containment and navigation
note = Markdown-like knowledge unit
index note = optional ordinary root note titled `index` for notebook page content
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

At Phase 1, folder slug paths were intentionally deferred until Phase 2 (they are implemented now—see Phase 2 status below).

## Migration Strategy

Initially, folders can mirror the existing parent-note structure. For every backfilled folder that represents a former “parent holds children” node, set **`folder.name`** to that parent note’s **`title`** (the same string as `note.title` on the parent). **`folder.slug`** stores the folder’s notebook-local full path; its basename is **slugify(`folder.name`)** per `ongoing/doughnut_wiki_architecture_north_star.md`.

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

After **Phase 5**, every note has a required non-empty **`title`**. Legacy null or empty titles are migration-only input and must be backfilled before the Phase 5 closeout.

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

Example URL shapes (exact encoding is an implementation detail; shipped paths use the app’s **`/d/`** prefix, e.g. **`/d/notebooks/:notebookId/notes/`** …):

```text
/d/notebooks/:notebookId/... path segments matching note.slug ...
```

The frontend may still use internal IDs behind the scenes after route resolution.

Support an additional **local segment** route for unambiguous accessible notes (basename only):

```text
/d/notes/:localSegment   (aliases e.g. /n:id)
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

- remove the note parent field from schema or APIs (that is **Phase 7**, after folders own placement in product behavior — see **Phase 6**)
- convert relationship notes
- parse all wiki links
- support Obsidian import/export

## Status and implementation notes

Phase 2 is **complete** in the codebase.

- **Slugifier and assignment:** `com.github.slugify:slugify` via `WikiSlugGeneration` / `WikiSlugPathService` and `WikiSlugPathAssignment`; unit tests cover generation and collision suffixing.
- **Persistence:** Migrations add nullable slug columns then enforce **not null** and **`unique(notebook_id, slug)`** on `folder` and `note` (e.g. `V300000150__folder_note_slug_nullable.sql`, `V300000151__folder_note_slug_not_null_unique.sql`). `Folder` and `Note` entities map **`slug`** as non-null.
- **New data:** Folder and note creation paths assign **`folder.slug`** and **`note.slug`** on create.
- **Backfill (historical):** Production-scale backfill was **idempotent and batched** via temporary admin endpoints; after backfill, slug-specific batch/status APIs and DTOs were **removed**. A generalized **Data migration** admin tab remains with **Run migration** calling an **admin-only stub** (`AdminDataMigrationController` at `/api/admin/data-migration/run`: authorization only, no slug business logic).
- **Resolution:**
  - **Notebook ID + full slug path:** API and frontend route (**`/d/notebooks/:notebookId/notes/:noteSlugPath`**); `NoteShowPage` resolves via `getNoteBySlug` then loads the note by id.
  - **Ambiguous basename:** `showNoteByAmbiguousBasename` when exactly one accessible note matches; otherwise a user-visible error.
  - **Internal id:** existing **`/n:noteId`** (and **`showNote`**) unchanged as a parallel entry.
- **Moves:** `NoteMotionService` recomputes **`note.slug`** after folder alignment (note subtree); uniqueness follows **`unique(notebook_id, slug)`** (tests cover collisions where applicable).
- **Tests:** Backend controller/service tests for migration-era behavior, basename lookup, slug path lookup, and motion; **`NoteShowPage.spec.ts`** covers id-, basename-, and slug-path-based resolution. E2E uses a shared **`wikiBasenameFromTitle`** helper aligned with slug rules for basename navigation; optional **Cypress coverage for navigating by full notebook-relative slug path** can be added if product owners want parity with backend behavior.

---

# Phase 3 — Replace Head Note with Optional Index Note

## Goal

Remove the conceptual dependency on the head note by converting it into an ordinary optional root note titled `index`.

## Rationale

The head note currently represents both:

```text
notebook identity
notebook content
```

In the final model, the notebook owns its own name. Notebook page content comes from an optional ordinary `index` note.

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
  name
  config
  folders
  notes
    index (optional ordinary root note)
```

## Model Changes

Keep on notebook:

```text
name
```

Do not add a notebook content field. The former head note becomes a normal note:

```text
Note
  title = "index"
  slug = "index"
  folderId = null
  content = former head note content
```

Assume there is no current production data with the title `index`, so the migration can use the `index` title and slug without resolving existing-note collisions.

An `index` note is optional. New notebooks do not need to create one until the user writes notebook page content.

## Migration Strategy

For each notebook:

1. Preserve the notebook's own name independently of note title/content.
2. Rename the current head note to title `index` and slug `index`, or otherwise migrate its content into a normal root note with those values.
3. Prefer preserving the former head note ID if feasible, because the migrated `index` note is still a normal note with stable identity.
4. Clear notebook/head-note coupling so notebook lookup no longer requires a head note.
5. Stop creating head notes for new notebooks. The optional root `index` note is created only when the user creates or edits that note like any other note (not precreating it for new notebooks).

## UI Changes

The notebook UI shifts from head-note/settings editing to a notebook page:

- remove “edit notebook settings” as the default entry; notebook settings remain reachable from the notebook page where needed
- remove the legacy `/d/notebooks/:notebookId/edit` route; the notebook page is the entry surface
- clicking a notebook goes directly to the notebook page
- notebook name and a short plain-text **`description`** (notebook-owned) appear on the notebook page; optional **`index`** content is resolved by slug (`index`) with explicit loading state and an edit affordance to the normal note route (**`/d/notebooks/:notebookId/notes/index`**)
- notebook page uses the same sidebar behavior as the note show page

## Export Implication

When exporting to Markdown, the optional `index` note exports like a normal note at the notebook root:

```text
index.md
```

If the notebook has no `index` note, export omits `index.md`.

## Expected Result

After this phase:

- notebook keeps its own name and optional **`description`**
- the former head note is represented as a normal root note titled `index` (when migrated)
- an `index` note is optional for each notebook; body editing uses the normal note show path
- first-layer notes no longer need to be children of a head note
- new notebooks do not require head notes or pre-created `index` notes
- `NoteRealm` exposes **`notebookId`** only (no embedded notebook on the wire); Obsidian export emits **`index.md`** only when an index note exists; **`notebook_head_note`** is removed after migration

## Status and implementation notes

Phase 3 is **complete** in the codebase.

- **Persistence:** Migrations add **`notebook.description`**, migrate former head notes to root **`index`** notes, then drop **`notebook_head_note`** (e.g. `V300000152` through `V300000156`).
- **API / clients:** Notebook DTOs carry **`description`**; no **`headNote`** / **`headNoteId`**; **`NoteRealm`** carries **`notebookId`** only (regenerate client after OpenAPI changes).
- **Frontend:** Catalog and cards link to **`notebookPage`**; notebook page loads optional index via slug API, avoids flashing “no index” while loading, and links edit to slug **`index`** on the note show route; legacy notebook **`/edit`** route removed.
- **Tests:** Controller tests assert absent head fields on JSON where applicable; Vitest and E2E cover notebook page and catalog navigation.

---

# Phase 4 — Introduce Note Properties

## Goal

Give each note an editable set of **properties** (key-value metadata) that:

- in the **markdown** editing surface, appear as a **YAML frontmatter** block (`---` … `---`) at the top of the content and are included when the note is saved
- are **persisted** as part of the note's Markdown details, without a separate backend property bag
- in the **rich text** editor, appear as **Properties** above the Quill body: a list of key-value pairs that can be edited inline, with the ability to add a new property or remove one

Edits in either surface update the same leading frontmatter block and must round-trip on save and reload.

## Rationale

The north-star architecture treats notes as Markdown-like units that may carry **frontmatter** (`ongoing/doughnut_wiki_architecture_north_star.md`). Keeping metadata in the Markdown details instead of ad hoc columns supports Obsidian-style Markdown, export/import, and later phases that put structured fields in content (for example relationship notes in **Phase 5** and note export in **Phase 11**).

## Observable behavior

**Pre-condition:** Users can open a note in markdown and rich editing modes; slugs and notebook boundaries behave as in prior phases.

**Trigger:** The user edits the YAML header in markdown and/or edits the Properties list above Quill in rich mode, then saves.

**Post-condition:** After save and reload, the persisted note details contain the frontmatter the user set; the markdown buffer shows that frontmatter; rich mode shows the same values as property rows.

## Persistence and surfaces

- **Backend:** Persist note details as Markdown text, including any leading YAML frontmatter. The backend does not own or expose a separate property map in this phase.
- **Markdown editor:** The editing buffer is the persisted Markdown details, including leading YAML frontmatter when present.
- **Rich editor:** Parse leading frontmatter from the Markdown details, show it as an editable key-value list above the Quill editing area, and serialize row changes back into the leading frontmatter block before saving through the same details update path.

## Testing (phase-complete)

- **E2E:** Assert the main user path for this capability—edit properties in rich mode (and, where product flows expose it, in markdown), save, reload, and see the same values—in a capability-named feature; prefer a targeted `--spec` rather than the full suite.
- **Unit / controller-level:** Cover frontend YAML/frontmatter serialization edges, invalid or ambiguous YAML handling in rich mode, and backend preservation of opaque Markdown details where needed.

## Non-goals

This phase does not need to:

- convert relationship notes into normal notes (**Phase 5**)
- remove the note parent field (**Phase 7**)
- parse wiki links in body content (**Phase 9**)
- add folder-level `defaultProperties` or folder templates (**Phase 10**)
- deliver full Obsidian export/import semantics for every conceivable frontmatter key (later phases build on persisted frontmatter)

## Expected Result

After this phase:

- notes have user-editable properties stored as YAML frontmatter in Markdown details
- markdown editing shows and saves that YAML frontmatter directly
- rich editing shows the same properties above Quill with inline add/edit/remove
- both surfaces remain consistent after save and reload

## Status and implementation notes

Phase 4 is **complete** in the codebase.

- **Persistence:** Note details remain Markdown text with optional leading YAML frontmatter; no separate properties column or API field ([ongoing/doughnut_wiki_architecture_north_star.md](ongoing/doughnut_wiki_architecture_north_star.md)).
- **Markdown editing:** The editing buffer is the full persisted details, including frontmatter.
- **Rich editing:** Properties appear above the Quill body via `RichMarkdownEditor` / `RichFrontmatterProperties`; scalar YAML is edited as rows. Unsupported shapes (nested YAML, duplicate keys in source, etc.) surface an error and keep rich body editing read-only until fixed in Markdown mode.
- **Testing:** E2E [`e2e_test/features/note_creation_and_update/note_edit.feature`](e2e_test/features/note_creation_and_update/note_edit.feature) covers YAML round-trip across markdown and rich flows; Vitests cover parsing/composition and editor wiring.

## Deferred / follow-up

- **Obsidian export and user-authored frontmatter:** Export behavior was not changed as part of Phase 4. [`ObsidianFormatService`](backend/src/main/java/com/odde/doughnut/services/ObsidianFormatService.java) currently emits exporter-owned YAML plus a `# title` line and appends raw `details`; reconciling that with user-authored leading YAML in `details` so exported files are coherent belongs in **Phase 11 — Export to Obsidian Markdown** (see the follow-up note there).

---

# Phase 5 — Convert Relationship Notes into Normal Notes

## Goal

Convert special structured relationship notes into ordinary Markdown-like notes, and move relationship/reference behavior onto frontmatter plus a persisted wiki-title cache.

Relationship notes today depend on the **parent** pointer for structure; this phase runs **before** folder-first containment UX (**Phase 6**) and **before** removing the note parent from schema (**Phase 7**) so migration can read that structure reliably.

The expanded Phase 5 also removes the relationship-specific `relation_type` / link-type and `target_note_id` fields after the frontmatter/cache representation can serve current relationship, reference, and graph behavior.

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

For old relationship notes, derive the note **`title`** from the relationship itself: source note title, relationship label, and target note title. Persist the derived title on the relationship note and truncate it before saving so it does not exceed `Note.MAX_TITLE_LENGTH` (currently 150 characters). The note slug is then generated from that title with the existing slug path rules.

This phase is also the point where the title invariant closes: after Phase 5, a note title cannot be null or empty in runtime behavior or persisted data.

## Wiki Title Cache

Phase 5 introduces a persisted cache of wiki-title references derived from note details and frontmatter. The cache is refreshed when a note's details are updated and backfilled during migration.

The cache is used to populate `NoteRealm.wikiTitles`, incoming references, and note graph relationships. For existing non-relationship notes, migration adds a `parent: "[[Parent Title]]"` frontmatter property when a legacy parent exists, then refreshes the cache. New notes do not get this property by default.

When a note title changes, the cache identifies notes that reference the old title so their wiki references can be updated to the new title and refreshed.

The production admin migration should run as a resumable batched job after the cache exists. It combines the already-implemented old relationship title/details backfill with the relationship cache backfill and legacy-parent frontmatter/cache backfill. Progress is stored in a temporary persisted migration table so the admin frontend can show progress between batches and resume safely after backend errors, deploys, or timeouts.

## Sub-Phase Plan

Phase 5 is decomposed in `ongoing/doughnut_wiki_migration_plan-phase-5-sub-phases.md`. Each sub-phase is intended to be a small, closed commit with its own targeted tests.

## Expected Result

After this phase:

- relationship notes are ordinary notes
- relationship fields are represented in content and/or frontmatter
- relationship notes have folder locations and note slugs (`note.slug` as full path)
- migrated relationship notes have derived, truncated, non-empty titles
- `NoteRealm.wikiTitles`, incoming references, and note graph relationship lookup use the persisted wiki-title cache
- existing non-relationship notes preserve legacy parent semantics as migration-only `parent` frontmatter wiki links
- relationship `relation` and `target` behavior no longer depends on note-level `relation_type` / link-type or `target_note_id` fields
- graph retrieval no longer treats children or child relationship notes as the source of incoming references for existing notes, and its reference quota is increased for cache-backed wiki references
- note titles are required for all notes; null or empty note titles are no longer valid after Phase 5
- old relationship-note-specific behavior is deprecated
- relationships become portable to Obsidian-style Markdown

---

# Phase 6 — Folder-First Child Listing; Remove Note Short Details

## Goal

Present **structural containment** through **folders** instead of **note-attached children**: primary navigation and listing APIs treat notes as members of a **folder** (or notebook root), not as a **children** collection hanging off a parent note. Remove **`shortDetails`** (the derived truncation of note body/details on **`NoteTopology`**) from wire shapes and UI so notes align with the north-star **title + Markdown content** surface—without a parallel summary field on topology DTOs.

## Rationale

Phase 1 already persists **`folderId`** parallel to **`Note.parent`** and keeps folders aligned on create/move. Users still experience much of the old tree through parent-child navigation and cards until this phase. **`ongoing/doughnut_wiki_architecture_north_star.md`** assigns **folder** as the only structural container; cards and sidebars should not anchor containment on “this note’s children.”

**Short details** are a legacy preview derived from HTML/Markdown **details**. The final model uses **title**, persisted **slug** path, and full **content**; optional excerpts belong in content/frontmatter or a later explicit policy, not **`NoteTopology.shortDetails`**.

## Observable behavior

**Pre-condition:** Relationship notes are ordinary notes (**Phase 5** complete). Folders and slugs behave as in prior phases.

**Trigger:** User browses a notebook, opens a folder, views note cards, or creates a note in what used to be a “parent holds children” place.

**Post-condition:** Structural peers are shown as **folder siblings** (or notebook-root notes when there is no folder) through the main flows; **`shortDetails`** is gone from **`NoteTopology`**, OpenAPI, generated clients, and listing/card surfaces that displayed it. Note graph sibling retrieval uses the same rule: siblings come from the note's folder, or from the notebook root when the note has no folder.

## Model / API / UI

- Prefer **folder-scoped** listing and navigation for “what lives here”; retire parent-note **children** lists as the primary containment UI (creation targets the relevant **folder**, not a structural parent note pointer in product semantics).
- Remove **`shortDetails`** from **`NoteTopology`** and regenerate the API client.
- For note graph retrieval, resolve structural siblings from **folder scope**: same **`folderId`** for foldered notes, or same notebook with no **`folderId`** for notebook-root notes.

## Sub-Phase Plan

Phase 6 is decomposed in `ongoing/doughnut_wiki_migration_plan-phase-6-sub-phases.md`. Each sub-phase is intended to be a small, closed commit with its own targeted tests.

## Non-goals

- Dropping **`Note.parent`** from persistence (**Phase 7**).
- Folder subtree move (**Phase 8**).
- Wiki-link parsing (**Phase 9**).

## Expected Result

After this phase:

- folder-first listing matches the wiki containment story while **`Note.parent`** may still exist until **Phase 7**
- topology and cards no longer expose **`shortDetails`**
- note graph sibling relationships come from folder membership, or notebook-root membership when the note has no folder

---

# Phase 7 — Remove Note Parent (Folders Replace Containment)

## Goal

**Remove the note parent concept** from the model after containment has been migrated into folders, relationship notes no longer depend on parent-pointer structure (**Phase 5**), and product behavior already treats folders as the containment surface (**Phase 6**). The target is not “optional parent”—it is **no parent-note field** for structure; folders are the sole mechanism for where a note lives.

## Rationale

Once relationship notes are converted and folders mirror former tree placement, parent-note edges duplicate folder semantics. Retaining an optional `parentNoteId` would keep two competing stories for containment.

Final distinction:

```text
folder = where the note lives (replaces parent-note containment)
link = what the note means
(note parent as a system field — removed)
```

If users want “semantic parent” for reading, it belongs in content (links, frontmatter), not as a structural parent pointer.

## Model Change

Remove the note-to-parent association used for containment and navigation from schema, APIs, and UI (exact steps depend on prior phases: migration must have assigned `folderId` and converted old relationship-note trees into folder + links in Phase 5; Phase 6 completes folder-first UX before this schema removal).

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

# Phase 8 — Move a Folder

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

- Parsing wiki links in content (Phase 9)
- Folder configuration templates (Phase 10)
- Obsidian export/import (later phases)

## Expected Result

After this phase:

- users (or the supported API) can move a folder to another parent or to the notebook root
- descendant paths remain valid and addressable; broken partial updates are not left behind
- folder move is the subtree counterpart to single-note move, not a duplicate of Phase 2 but an extension of the same slug invariants

---

# Phase 9 — Add Wiki-Link Parser and Link Index

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

# Phase 10 — Add Folder Configuration Behavior

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

# Phase 11 — Export to Obsidian Markdown

## Goal

Export Doughnut notebooks as Obsidian-compatible Markdown folder trees.

## Export Shape

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

### Persisted YAML vs export layout (Phase 4 deferral)

Phase 4 stores optional properties as leading YAML inside note `details`. Until Phase 11 export explicitly merges that with exporter metadata, [`ObsidianFormatService`](backend/src/main/java/com/odde/doughnut/services/ObsidianFormatService.java) may emit overlapping structure (`generateFrontMatter` plus `# title` and raw `details`). Delivering single coherent Markdown files per note (including user frontmatter) is part of completing Phase 11 export semantics.

## Link Export

Prefer the default note-title link format:

```markdown
[[どうやら]]
```

If the target needs disambiguation, folder-qualified links may be allowed:

```markdown
[[japanese/vocabulary/どうやら]]
```

## Notebook Index Export

The optional root `index` note exports as:

```text
index.md
```

If the notebook has no `index` note, export omits this file.

## Expected Result

After this phase:

- exported notebooks can be opened in Obsidian
- folder structure is preserved
- note content is Markdown
- links are Obsidian-style wiki links
- metadata is represented as frontmatter
- relationship notes are normal Markdown files

---

# Phase 12 — Import and Round Trip from Obsidian

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

# Phase 13 — Remove Legacy Assumptions

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
  name
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
  content (Markdown details, including optional YAML frontmatter)
  properties (derived editing view of content frontmatter)
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
- head note is gone from the core model; optional notebook page content is a normal `index` note
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
3. Replace head note with optional index note
4. Introduce note properties (YAML frontmatter, persisted)
5. Convert relationship notes
6. Folder-first listing; remove note short details
7. Remove note parent (folders replace containment)
8. Move a folder
9. Add wiki-link parser and link index
10. Add folder config behavior
11. Export to Obsidian Markdown
12. Import and round trip from Obsidian
13. Remove legacy assumptions
```

## Dependency Summary

```text
folder
  -> note.slug (notebook-local full path)
    -> optional index note without head note
      -> note properties (YAML frontmatter, rich + markdown surfaces)
        -> relationship notes as normal notes
          -> folder-first listing; remove note short details from topology
            -> remove note parent (folders own placement)
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
