# Doughnut Wiki Migration Plan

- **North star:** `ongoing/doughnut_wiki_architecture_north_star.md`
- **Admin wiki reference batches:** `ongoing/admin-wiki-reference-migration-redo-plan.md`, `ongoing/admin-wiki-reference-migration-status.md`
- **Phase 5.24 (drop `note.target_note_id` — optional detail):** `ongoing/doughnut_wiki_migration_plan-phase-5.24-sub-phases.md`
- **Phase 6 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-6-sub-phases.md` (complete)
- **Phase 7 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-7-sub-phases.md`
- **Phase 9 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-9-sub-phases.md`
- **Phase 10 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-10-sub-phases.md`
- **Phase 12 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-12-sub-phases.md`

## Purpose

Phased migration toward the wiki-style, markdown-first architecture in the north star. This file tracks **what remains** and **execution order**; deep design for shipped work lives in the north star and phase-specific docs above.

## Status

| Phase | Status |
|-------|--------|
| 1 — Folder | Done |
| 2 — Slug paths (`folder.slug`, `note.slug`, routing) | Done (retired at boundary before 7) |
| 3 — Index note (no head note) | Done |
| 4 — Properties / YAML frontmatter in note `content` | Done |
| 5 — Relationship notes → normal notes + wiki title cache | Done (title-rename propagation deferred to **Phase 12**; granular **5.24** notes in `doughnut_wiki_migration_plan-phase-5.24-sub-phases.md`) |
| 6 — Folder-first listing; no derived short preview on topology | Done |
| Boundary — slug retirement | Done (before Phase 7) |
| 7 — Remove note parent | Done |
| 8 — Move / dissolve folder (organize) | Done |
| 9 — Wiki-link parser and link index | Not started |
| 10 — Index-scoped configuration (notebook + folder) | Done — container `indexContent` is canonical; `index_note_id` columns removed (10.18). Legacy notes renamed to `index_to_be_deleted` during migration remain for manual review; a later user-facing bulk delete or archive is optional follow-up. |
| 11 — Remove legacy assumptions | Not started |
| 12 — Title rename propagates wiki references (deferred from Phase **5.25**) | Planned — see `doughnut_wiki_migration_plan-phase-12-sub-phases.md` |

---

## Concepts that affect later phases

**Stable internal id** — DB, sync, conflict resolution; canonical URLs after slug retirement: **`/d/n/:noteId`** (north star).

**Until the boundary below**, the codebase still has persisted **`folder.slug`** / **`note.slug`**, **`unique(notebook_id, slug)`**, and slug-path routes from Phase 2. Basename within a folder = substring after last `/` in `note.slug` (or whole string at root). Slugify: `com.github.slugify:slugify` (`WikiSlugGeneration` / `WikiSlugPathService`).

**Folder** = structural containment. **`note.parent_id`** is removed (Phase 7); folder alignment and motion use folder placement and `NoteChildContainerFolderService` / `NoteMotionService` where applicable (see codebase).

**Container index content** — Notebook root and folders store landing-page Markdown plus scoped configuration frontmatter on **`notebook.indexContent`** and **`folder.indexContent`** (Phase **10** complete). Legacy index notes were migrated and renamed to **`index_to_be_deleted`**; **`index`** is reserved as a note title; **`index_note_id`** pointer columns were dropped. Notebook has **`name`** and **`description`**; no `headNote` on APIs. See north star for folder page routing vs note routes.

**Properties** — Leading YAML in Markdown `content`; rich editor mirrors via frontmatter UI.

**Phase 5 (shipped)** — Relationship notes are normal notes with frontmatter (`type: relationship`, `relation`, `source`/`target` as `[[…]]`). **`note_wiki_title_cache`** backs references and graph reads; **`NoteRealm.references`** is the unified note-show surface; titles are required (non-null/non-empty). Legacy parent may appear as migration-only `parent: "[[…]]"` in frontmatter for non-relationship notes. **`note.target_note_id`** is removed (**5.24**). **Reverse-updating referrers when a title changes** is **Phase 12** (formerly **5.25** before closeout).

---

## Completed work (summary only)

Phases **1–8** are shipped (Phase **7**: structural `note.parent_id` / `Note.parent` removed; folder placement only; Phase **8**: move and dissolve folder). **Phase 5 closeout:** relationship notes normalized; graph and note show use cached wiki links; legacy **`Note.target_note_id`** dropped per **5.24** migrations and `ongoing/doughnut_wiki_migration_plan-phase-5.24-sub-phases.md` where slice-level detail matters.

- **1:** `Folder`, `note.folder_id`, backfill from the former parent-note tree (historical migrations).
- **2:** Full-path slugs, resolution by notebook + slug path and ambiguous basename; moves recompute `note.slug`.
- **3:** Migrations dropped `notebook_head_note`; catalog → notebook page; optional index at slug `index`.
- **4:** Frontmatter round-trip (markdown + rich); unsupported YAML shapes block rich body until fixed in markdown.
- **5:** Relationship notes as normal notes + `note_wiki_title_cache`; unified references on note show and graph; `relation_type` and `note.target_note_id` removed. Transitional graph hop flags on related-note DTOs were removed in **Phase 7.13**. Title-rename propagation → **Phase 12**.
- **6:** Primary containment UX is folder-scoped; topology has no derived short preview field; graph siblings from folder (or notebook root without folder).
- **7:** Structural note parent removed from schema and APIs; notes use `folderId` / notebook root; see `doughnut_wiki_migration_plan-phase-7-sub-phases.md`.
- **8:** Folder **move** (`POST …/folders/{folder}/move`) and **dissolve** (`DELETE …/folders/{folder}`): same-notebook reparenting; sibling name uniqueness at destination; dissolve promotes direct notes and **child folders** to the dissolved folder’s parent (with conflict checks); sidebar entry when a folder is active opens one organize dialog (`FolderOrganizeDialog.vue`); shared backend checks in `FolderSiblingNameValidation` and move graph rules in `FolderMoveDestinationRules`.

---

## Boundary after Phase 6 — retire persisted slugs (historical)

Phases **1–6** once shipped **persisted** `folder.slug` / `note.slug` and slug-path routing (**Phase 2**). The **slug retirement** boundary (before Phase **7**) dropped slug columns and path-primary identity in favor of **`/d/n/:noteId`** and folder ids, per **`ongoing/doughnut_wiki_architecture_north_star.md`**.

**Phase 7 onward** in this document assumes slug retirement **complete** and **no** structural note parent column.

---

# Phase 7 — Remove note parent (folders replace containment)

**Status:** Shipped — execution detail in `ongoing/doughnut_wiki_migration_plan-phase-7-sub-phases.md`.

## Goal

Remove the structural **parent note** from the model after: relationship notes no longer depend on parent trees (**Phase 5**), folder-first UX (**Phase 6**), and **slug retirement** (boundary above). Target is **no** `parentNoteId`-style field for containment — folders only.

## Rationale

Parent edges duplicate folder placement. Optional `parentNoteId` would keep two containment stories.

```text
folder = where the note lives
link / content = meaning
(no system parent-note field)
```

## Model / product

Remove note-to-parent association from schema, APIs, and UI. Valid note: **`notebookId`**, optional/required **`folderId`** per policy — **no** parent FK for structure.

## UI

Navigation and creation are folder-based only, not parent-note hierarchy. “Semantic parent” = links or frontmatter, not DB edges.

## Expected result

- No structural parent note; placement folder-only  
- Creation does not use parent note for containment  
- Parent references gone from persistence and primary navigation  

---

# Phase 8 — Move and dissolve folder

**Status:** Shipped.

## Goal

Users can **reparent** a folder within the same notebook (`parentFolderId` or notebook root) and **dissolve** a folder without deleting its notes. On a pure move, **notes** keep **`folderId`** on the same folder rows; descendant folders stay under the moved subtree.

## Model and API

- **Move:** `POST /api/notebooks/{notebook}/folders/{folder}/move` with optional `newParentFolderId` (omit or null for notebook root). Same-notebook only; cannot move into self or a descendant; sibling **name** uniqueness at the destination (aligned with folder create).
- **Dissolve:** `DELETE /api/notebooks/{notebook}/folders/{folder}`. The folder row is removed. Direct notes in that folder get **`folderId`** set to the dissolved folder’s **parent** (notebook root if the folder was at root). **Subfolders:** promoted to that same parent, with sibling-name checks so promotions do not collide. User-facing label for removal: **Dissolve folder** (distinct from note delete).

## UI

One **organize** dialog hosts move and dissolve. Entry: sidebar toolbar when a **folder is active** (user-selected folder scope).

## Validation (backend)

Shared sibling-name checks: `FolderSiblingNameValidation`. Move destination graph rules (not self / not descendant): `FolderMoveDestinationRules`. Persistence query remains `FolderRepository.findCandidateChildContainers`.

## Non-goals

Cross-notebook moves, bulk note moves unrelated to dissolve, wiki-link parsing (**Phase 9**), index-scoped defaults and folder index (**Phase 10**), slug/path columns (retired at boundary).

## Expected result

Tree and listings reflect new parents after move; after dissolve, notes and promoted subfolders appear under the former parent without orphaning content.

---

# Phase 9 — Wiki-link parser and link index

**Scope note:** Current Phase 9 planning is narrowed to display text only; folder-qualified paths are deferred.

## Goal

First-class display text in `[[wiki links]]` in content and rich mode.

## Syntax (authoring)

```markdown
[[note title]]
[[note title|display text]]
```

No folder path syntax in this slice. Existing notebook-qualified title syntax may continue where already supported, but the new work is only the `|display text` part.

## Indexes (conceptual)

```text
OutgoingLinkIndex — sourceNoteId, targetNoteId?, unresolvedToken?, displayText?, resolvedStatus
BacklinkIndex — targetNoteId, sourceNoteId
```

Rule: **index is derived; note content is source of truth.**

## Resolution (authoring-time)

1. Exact **title** in notebook scope when unambiguous  
2. Unique shorthand title when one match among accessible notes  
3. Unresolved — preserve token and display text  

Do **not** treat persisted slug/path on rows as authoritative (none after boundary).

## Expected result

Parse `[[note title|display text]]`, show display text in rich mode, preserve unresolved tokens, and allow a dead-link token to be linked to an existing note while retaining the original wiki text as display text.

---

# Phase 10 — Index-scoped configuration (notebook root + folders)

**Status:** In progress — sub-phases **10.1 through 10.13** are done. Remaining work is redirected in `ongoing/doughnut_wiki_migration_plan-phase-10-sub-phases.md` so index content lives on notebooks/folders instead of canonical index notes.

## Goal

**Notebook root** and each **folder** have optional **`indexContent`** whose **Markdown body and YAML frontmatter** are the portable source of truth for **landing page content** and **scoped behavior**.

## Model

- **Container content** — Notebook and folder rows own nullable **`indexContent`** Markdown. Leading YAML frontmatter on that content carries scoped defaults.
- **Legacy migration** — Existing index notes from 10.1–10.13 are migration input only: copy their content to the owning notebook/folder `indexContent`, then rename the notes to **`index_to_be_deleted`**.
- **Reserved title** — After migration, **`index`** is a reserved note title; create/rename paths reject it so the old convention cannot reappear.
- **No synthetic root folder** — Notebook root remains **`folderId` null**; no hidden folder row that holds all notes.
- **Config location** — Scoped defaults live in **container `indexContent` frontmatter**, not a separate **`folder.config`** / duplicate blob (see Phase 11 target shape).

## Product (UI / UX)

- **Notebook page** and **folder page** embed the **same** markdown + frontmatter editing pipeline as note show where practical (layout polish can follow).
- **Empty index content** — Still show the editor; **first save** updates notebook/folder **`indexContent`** directly.
- **Folder page** — First-class route; sidebar entry navigates to the folder page and uses **container vs note** routing (not **active folder + active note** simultaneously).
- **Breadcrumbs** — Folder segments in the breadcrumb link to the corresponding **folder page** (consistent with sidebar navigation).
- **Sidebar** — With a folder page open, the tree **auto-scrolls** so the active folder is visible in the sidebar.
- **Predefined properties** — Product-defined keys (e.g. **note title pattern**, **question generation instruction**) appear in notebook/folder index editors, not on normal notes.
- **Search** — No search exclusion is needed for container index content because it is not a note.

## Examples (illustrative frontmatter)

```yaml
# creation defaults / AI (examples; exact keys are product-defined)
title_pattern: "{{date}}"
question_generation_instruction: "Focus on definitions; avoid trick wording."
```

```yaml
# journal-style (future / illustrative)
template: daily-note
defaultProperties:
  type: journal
title_pattern: "{{date}}"
defaultExportBasenamePattern: "{{date}}"
```

```yaml
# relationship-style (future / illustrative)
template: relationship-note
defaultProperties:
  type: relationship
title_pattern: "{{source}} vs {{target}}"
defaultExportBasenamePattern: "{{source}}-vs-{{target}}"
```

**Inheritance** — Folder `indexContent` → parent folder `indexContent` → notebook `indexContent` can be defined in implementation so defaults compose predictably.

## Distinction

```text
stable config = long-term product behavior (container indexContent frontmatter + notebook row settings as needed)
migration config = temporary transformation rules
```

## Expected result

Notebook and folder landing pages save directly to container `indexContent`; index-only predefined properties are available in those container editors; scoped defaults and instructions come from `indexContent` frontmatter; legacy index notes are migrated to `index_to_be_deleted`; **`index`** is reserved as a note title; no parallel **`folder.config`** column for the same data.

---

# Phase 11 — Remove legacy assumptions

## Goal

Delete remaining legacy concepts once the new model is stable.

## Remove / isolate

```text
note parent for containment
structural peer list order by note id (folder / notebook root scope)
head note / special relationship structure / tree-only navigation
slug columns and path-primary routes as canonical identity (already removed at boundary)
```

## Target shape (aligns north star)

```text
Notebook — id, name, description?, config, indexContent?, …
Folder — id, notebookId, parentFolderId?, name (unique among siblings), indexContent?, …
Note — id, notebookId, folderId?, title, Markdown content (+ frontmatter)
(no parentNoteId, no note.slug column; scoped defaults on notebook/folder indexContent, not a duplicate Folder.config blob)
LinkIndex — derived from content
```

---

# Phase 12 — Title rename propagates wiki references

## Goal

When a note’s **title** changes, notes that wiki-linked the **old** title are **reverse-updated** according to an explicit user choice, and **`note_wiki_title_cache`** rows are refreshed so **`NoteRealm.references`**, graph, and search stay consistent. Execution detail lives in `ongoing/doughnut_wiki_migration_plan-phase-12-sub-phases.md`.

## Rationale

This slice was **Phase 5.25** before Phase **5** closeout and is scheduled **late** so Phase 5 could ship relationship persistence, unified note-show references, and cleanup first. Implementation may align with **Phase 9** (link index) when that work clarifies a single derived-index story; until then, behavior can build on the existing wiki-title cache from Phase **5**.

## Pre-condition

Referring notes are discoverable via the wiki-title cache; title updates are an established API path.

## Trigger

A user or automation changes a note title.

## Post-condition

Sources that pointed at the old title either show updated visible reference text or preserve their current visible text while linking to the renamed note; cache rows point at the renamed note. Tests cover title update with at least one referrer, the backend exception when no reference-handling choice is provided, one successful E2E browser scenario, and reference updates when a note moves to another notebook.

## Verify

Focused backend tests, generated API refresh after DTO changes, and targeted E2E for the wiki-link feature.

## Expected result

Renaming a note does not strand stale wiki tokens or cache rows in common cases; referrers and note show stay coherent.

---

# Phase order summary

```text
1. Folder
2. Slug paths (historical; removed at boundary before 7)
3. Optional index note
4. Properties / frontmatter in content
5. Relationship notes → normal + wiki title cache
6. Folder-first listing; no short preview on topology
→ boundary: retire persisted slugs; /d/n/:noteId; folder by id
7. Remove note parent
8. Move / dissolve folder
9. Wiki-link parser + indexes
10. Index-scoped config (notebook + folder indexContent)
11. Legacy cleanup
12. Title rename propagates wiki references (deferred from Phase 5.25)
```

## Dependency chain

```text
folders + (historical slugs until boundary)
  → index note, properties
    → relationship migration + cache
      → folder-first UX
        → slug retirement + id-canonical URLs
          → drop structural note parent (done)
            → folder move / dissolve (done)
              → wiki links + indexes
                → index-scoped config (notebook + folder indexContent)
                  → legacy cleanup
                    → title rename propagates wiki references
```

## Principle

```text
Doughnut owns stable identity and behavior.
Markdown owns portability.
Folders own containment.
Links own meaning.
```
