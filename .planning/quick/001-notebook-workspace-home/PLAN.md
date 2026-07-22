# Notebook as workspace home (distinct from note / folder)

## Intent

Make the **notebook page** feel like an **overview / command center** (Notion workspace home), clearly distinct from **note** (document editor) and **folder** (container admin). Primary job: **see and edit the notebook index** (intro + properties), not a settings form stack.

## Status

| Phase | Type | Status | Observable outcome |
|-------|------|--------|--------------------|
| 1 | Structure (design) | planned | Winning HTML sketch direction chosen for notebook home layout |
| 2 | Behavior | planned | Opening a notebook shows a workspace-home main column (hero + index primary), not a settings stack |
| 3 | Behavior | planned | Index content + properties are the primary editable surface; settings/admin actions are secondary |
| 4 | Behavior | planned | Folder and note pages remain recognizably different from notebook home (no regression of their roles) |

## Design direction (from intake)

- **Feel:** overview / command center
- **Reference:** Notion workspace home
- **Core action:** read/edit `indexContent` (intro) and update index/notebook properties
- **Not:** denser admin forms; not a second note editor with the same chrome as `NoteShow`

## Discoveries (session)

- All three routes share `NotebookSidebarLayout` (GlobalBar + tree sidebar). Distinction must live in the **main column**.
- **Note** = full-height document (`NoteToolbar`, auto-save editor).
- **Notebook + folder** today = max-w-6xl **settings stacks** + shared `ScopedIndexNoteEditor` (`indexContent`, explicit Save, `isIndexContext`, frontmatter incl. `title_pattern`).
- Notebook-only below the fold: attached book, management (move/share), settings (`description`, `skipMemoryTrackingEntirely`), search-index reset — these currently compete with index for attention.
- Stack: Vue 3 + Tailwind 4 + DaisyUI (`daisy-*`), Lucide; tokens via Daisy `--color-base-*`.
- Key product fields for mocks: `notebook.name`, `notebook.description`, realm `indexContent`, `title_pattern`, sidebar tree, settings as demoted.

## Sketch backlog (Phase 1 — resume `/gsd-sketch` here)

Present as design questions (first round: dramatic variants):

| Sketch | Design question | Approach | Risk |
|--------|-----------------|----------|------|
| 001 | Does notebook home layout read as command center vs note/folder? | A: Notion-like hero + index canvas; B: split canvas + property rail; C: tabbed Home / Settings | **High** — sets page structure |
| 002 | Where do index properties live without feeling like a form? | A: light strip under hero; B: side rail; C: inline in editor frontmatter only | Medium |
| 003 | How are management/settings/indexing demoted? | A: gear drawer; B: “Settings” secondary tab; C: footer accordion | Medium |

Artifacts: `.planning/sketches/` (MANIFEST, theme, `NNN-*/index.html`). No product code in Phase 1.

## Phase details

### Phase 1 — Structure (design): choose notebook home direction

**Goal:** Finish GSD sketch intake → build 2–3 HTML variants for sketch 001 (layout), pick a winner; optionally 002/003 if needed before build.

**Done when:** Winning variant marked in sketch README + MANIFEST; direction clear enough for UI-SPEC / implement.

**Resume:** `/gsd-sketch` (or continue this chat: create theme + `001-notebook-home-layout` variants). Optional `/gsd-ui-phase` after winner.

### Phase 2 — Behavior: notebook opens as workspace home

**Pre:** User owns/opens a notebook with optional `indexContent`.

**Trigger:** Navigate to `/notebooks/:notebookId`.

**Post:** Main column reads as home: strong `notebook.name` (and description cue), **Index** as primary canvas; not a vertical stack of settings cards at first paint. Sidebar chrome unchanged.

**Tests:** Targeted E2E asserting notebook home landmarks (capability-named feature, e.g. notebook workspace / notebook page — not phase-numbered).

### Phase 3 — Behavior: index + properties primary; admin secondary

**Pre:** Notebook home from Phase 2.

**Trigger:** Edit index markdown / properties; open settings/management.

**Post:** Index save/edit path remains obvious; move/share/skip-memory/search-index/attach-book reachable but **not** competing with index on the default view (drawer, tab, or below-fold secondary).

**Tests:** E2E for index edit + that settings are secondary; extend existing notebook page specs if present.

### Phase 4 — Behavior: note and folder stay distinct

**Pre:** Note and folder routes as today (or lightly adjusted only if needed for contrast).

**Trigger:** Navigate note vs folder vs notebook.

**Post:** User can tell surfaces apart: note = document tool chrome; folder ≠ notebook home (folder keeps container/admin role or its own lesser treatment — **do not** copy notebook hero onto folder unless a later plan says so).

**Tests:** Smoke/regression E2E across the three routes’ main landmarks.

## Out of scope (this quick plan)

- Redesigning GlobalBar / sidebar tree
- Changing note auto-save / `NoteToolbar`
- Full bazaar / sharing UX redesign
- Search-index backend behavior

## Next action

Resume **Phase 1**: build `.planning/sketches/001-notebook-home-layout` with variants A/B/C and get a winner before product code.
