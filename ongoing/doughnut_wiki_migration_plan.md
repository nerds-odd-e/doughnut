# Doughnut Wiki Migration Plan

- **North star:** `ongoing/doughnut_wiki_architecture_north_star.md`
- **Admin wiki reference batches:** `ongoing/admin-wiki-reference-migration-redo-plan.md`, `ongoing/admin-wiki-reference-migration-status.md`
- **Phase 5 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-5-sub-phases.md`
- **Phase 6 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-6-sub-phases.md` (complete)
- **Phase 7 breakdown:** `ongoing/doughnut_wiki_migration_plan-phase-7-sub-phases.md`

## Purpose

Phased migration toward the wiki-style, Obsidian-compatible architecture in the north star. This file tracks **what remains** and **execution order**; deep design for shipped work lives in the north star and phase-specific docs above.

## Status

| Phase | Status |
|-------|--------|
| 1 — Folder | Done |
| 2 — Slug paths (`folder.slug`, `note.slug`, routing) | Done (retired at boundary before 7) |
| 3 — Index note (no head note) | Done |
| 4 — Properties / YAML frontmatter in `details` | Done |
| 5 — Relationship notes → normal notes + wiki title cache | Mostly done — open items in phase-5 sub-phase doc |
| 6 — Folder-first listing; remove `NoteTopology.shortDetails` | Done |
| Boundary — slug retirement | Next (before Phase 7) |
| 7+ | Not started |

---

## Concepts that affect later phases

**Stable internal id** — DB, sync, conflict resolution; canonical URLs after slug retirement: **`/d/n/:noteId`** (north star).

**Until the boundary below**, the codebase still has persisted **`folder.slug`** / **`note.slug`**, **`unique(notebook_id, slug)`**, and slug-path routes from Phase 2. Basename within a folder = substring after last `/` in `note.slug` (or whole string at root). Slugify: `com.github.slugify:slugify` (`WikiSlugGeneration` / `WikiSlugPathService`).

**Folder** = structural containment; **`Note.parent`** still exists in schema until Phase 7 and was the legacy tree; folder alignment uses `NoteChildContainerFolderService` / `NoteMotionService` (see codebase).

**Index note** — Optional root note titled `index` / slug `index`; notebook has **`name`** and **`description`**; no `headNote` on APIs.

**Properties** — Leading YAML in Markdown `details`; rich editor mirrors via frontmatter UI. Exporter coherence with user YAML → **Phase 11** (`ObsidianFormatService`).

**Phase 5 (in flight / verify in sub-phase doc)** — Relationship notes are normal notes with frontmatter (`type: relationship`, `relation`, `source`/`target` as `[[…]]`). **`note_wiki_title_cache`** backs references/graph reads; titles are required (non-null/non-empty). Legacy parent may appear as migration-only `parent: "[[…]]"` in frontmatter for non-relationship notes.

---

## Completed work (summary only)

Phases **1–4** and **6** are shipped; **5** is largely shipped — use the **phase-5** / **phase-6** markdown files for checklists and tests.

- **1:** `Folder`, `note.folder_id`, backfill from parent-note tree; `Note.parent` still drives legacy navigation/ordering until Phase 7.
- **2:** Full-path slugs, resolution by notebook + slug path and ambiguous basename; moves recompute `note.slug`.
- **3:** Migrations dropped `notebook_head_note`; catalog → notebook page; optional index at slug `index`.
- **4:** Frontmatter round-trip (markdown + rich); unsupported YAML shapes block rich body until fixed in markdown.
- **6:** Primary containment UX is folder-scoped; topology has no `shortDetails`; graph siblings from folder (or notebook root without folder).

---

## Boundary after Phase 6 — retire persisted slugs

Phases **1–6** shipped **persisted** `folder.slug` / `note.slug` and slug-path routing (**Phase 2**).

**Immediately before** removing **`Note.parent`** (**Phase 7**), finish **eliminating persisted slug/path identity**: drop **`note.slug`** and **`folder.slug`** (schema and backups), slug-keyed uniqueness, slug-path lookups and resolving routes built for Phase 2, and UX that treated path strings as the canonical note address — per **`ongoing/doughnut_wiki_architecture_north_star.md`**. Canonical note URLs become **`/d/n/:noteId`**; folders addressed by **`id`**; sibling folders unique by **`name`** under the same parent.

**Phase 7 onward** in this document assumes slug retirement **complete**.

---

# Phase 7 — Remove note parent (folders replace containment)

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

# Phase 8 — Move a folder

## Goal

Reparent a folder (`parentFolderId` or notebook root) while **notes** keep **`folderId`** on the same folder row; descendant folders move with the subtree.

## Model and validation

- Update moved folder’s `parentFolderId` (or null for root).  
- **Folder name uniqueness among siblings** at destination (same as create/rename; north star).  
- Slug/path recomputation is **not** final behavior — no slug columns after boundary.

## Non-goals

Wiki-link parsing (**Phase 9**), folder templates (**Phase 10**), Obsidian export (**Phase 11**).

## Expected result

Users can move folders; descendants and contained notes stay attached without rewriting path-string columns.

---

# Phase 9 — Wiki-link parser and link index

## Goal

First-class `[[wiki links]]` in content; derived indexes for outgoing/backlinks.

## Syntax (authoring)

```markdown
[[note title]]
[[note title|display text]]
[[folder/name/…/segment/note title|display text]]
```

Path segments = **nested folder sibling names** (`Folder.name`), not resurrected slug columns.

## Indexes (conceptual)

```text
OutgoingLinkIndex — sourceNoteId, targetNoteId?, unresolvedToken?, displayText?, resolvedStatus
BacklinkIndex — targetNoteId, sourceNoteId
```

Rule: **index is derived; note content is source of truth.**

## Resolution (authoring-time)

1. Exact **title** in notebook scope when unambiguous  
2. **Folder-qualified path** by `Folder.name` chain + note title in that folder  
3. **Relative** from source note’s folder  
4. **Unique shorthand title** when one match among accessible notes  
5. **Unresolved** — preserve token  

Do **not** treat persisted slug/path on rows as authoritative (none after boundary).

## Expected result

Parse links, show outgoing + backlinks, preserve unresolved, unify graph with wiki semantics.

---

# Phase 10 — Folder configuration behavior

## Goal

Folders define defaults: templates, `defaultProperties`, title patterns, journal/relationship/map-style behaviors.

## Examples (illustrative)

```yaml
# journal-style
template: daily-note
defaultProperties:
  type: journal
titlePattern: "{{date}}"
defaultExportBasenamePattern: "{{date}}"
```

```yaml
# relationship-style
template: relationship-note
defaultProperties:
  type: relationship
titlePattern: "{{source}} vs {{target}}"
defaultExportBasenamePattern: "{{source}}-vs-{{target}}"
```

## Distinction

```text
stable config = long-term product behavior
migration config = temporary transformation rules
```

## Expected result

Folder-level creation defaults and templates; migration-only rules isolated.

---

# Phase 11 — Export to Obsidian Markdown

## Goal

Export notebooks as Obsidian-compatible folder trees; basenames from **title** (or export rules) + collision handling — **not** from removed `note.slug`.

## Shape (illustrative)

```text
Notebook/
  index.md
  Journal/2026/2026-04-28.md
  Notes/douyara.md
  Relationships/….
```

## Note file

Typical frontmatter includes stable **`id`**, timestamps, **`type`**, etc.; body after `# title`. Merge **user-authored leading YAML inside `details`** with exporter metadata so files are single coherent Markdown (**Phase 4** deferral).

## Links export

Default `[[title]]`; disambiguate with **folder-named** paths using **names**, not old slug columns.

## Expected result

Portable Markdown trees, correct links, coherent frontmatter.

---

# Phase 12 — Import and round trip from Obsidian

## Goal

Import Obsidian-style folders; support re-import of exports.

## Priority

```text
frontmatter id > generated id
frontmatter title > first H1 > filename-derived title
folder path → folder records (name uniqueness among siblings)
```

## Importer responsibilities

Parse frontmatter; create/update folders and notes; preserve **id** when present; derive titles without resurrecting **`note.slug`**; parse wiki links; rebuild link indexes; surface conflicts (id vs path, title collisions, missing targets, etc.).

## Round trip

`Doughnut export → Obsidian edit → Doughnut import` with stable **`id`** in frontmatter where possible.

---

# Phase 13 — Remove legacy assumptions

## Goal

Delete remaining legacy concepts once the new model is stable.

## Remove / isolate

```text
note parent for containment
sibling order from old parent-child tree (replaced by explicit ordering when defined)
head note / special relationship structure / tree-only navigation
slug columns and path-primary routes as canonical identity (already removed at boundary)
```

## Target shape (aligns north star)

```text
Notebook — id, name, config
Folder — id, notebookId, parentFolderId?, name (unique among siblings), config
Note — id, notebookId, folderId?, title, Markdown details (+ frontmatter)
(no parentNoteId, no note.slug column)
LinkIndex — derived from content
```

---

# Phase order summary

```text
1. Folder
2. Slug paths (historical; removed at boundary before 7)
3. Optional index note
4. Properties / frontmatter in details
5. Relationship notes → normal + wiki title cache
6. Folder-first listing; remove shortDetails
→ boundary: retire persisted slugs; /d/n/:noteId; folder by id
7. Remove note parent
8. Move folder
9. Wiki-link parser + indexes
10. Folder config
11. Obsidian export
12. Obsidian import / round trip
13. Legacy cleanup
```

## Dependency chain

```text
folders + (historical slugs until boundary)
  → index note, properties
    → relationship migration + cache
      → folder-first UX
        → slug retirement + id-canonical URLs
          → drop Note.parent
            → folder move
              → wiki links + indexes
                → folder config
                  → export → import → legacy cleanup
```

## Principle

```text
Doughnut owns stable identity and behavior.
Markdown owns portability.
Folders own containment.
Links own meaning.
```
