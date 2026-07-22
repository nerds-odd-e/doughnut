---
name: sketch-findings-doughnut
description: Validated design decisions, CSS patterns, and visual direction from sketch experiments. Auto-loaded during UI implementation on doughnut.
---

<context>
## Project: doughnut

Notebook page as an **overview / command center** (Notion workspace home): land on identity + **index content** (intro) and properties. Distinct from note (document editor) and folder (container admin). Shared sidebar chrome stays; differentiate the main column. Demote management / settings / search-indexing so they do not compete with the index canvas.

**Reference points:** Notion workspace home; current Doughnut `NotebookPageView` + `ScopedIndexNoteEditor`.

**Interaction:** Index markdown uses the same **debounced autosave** as note body content (`useDebouncedTextAutosave`) — no explicit Save button.

Sketch sessions wrapped: 2026-07-22
</context>

<design_direction>
## Overall Direction

- **Layout:** Home / Settings tabs on the notebook page (sketch 001 winner C). Default tab = command-center home (title + light properties + index canvas). Settings tab holds management, notebook settings, and search-index maintenance.
- **Feel:** Overview / command center, not a document tool chrome and not a settings form stack.
- **Palette / type (sketch theme):** Calm light surfaces, teal primary (`--color-primary`), Fraunces display + DM Sans UI — see `sources/themes/default.css`.
- **Autosave:** Index and note body share one debounced autosave composable (1s); flush on blur; dirty corner indicator.
</design_direction>

<findings_index>
## Design Areas

| Area | Reference | Key Decision |
|------|-----------|--------------|
| Notebook home layout | references/notebook-home-layout.md | Home / Settings tabs; index primary on Home |

## Theme

The winning theme file is at `sources/themes/default.css`.

## Source Files

Original sketch HTML files are preserved in `sources/` for complete reference.
</findings_index>

<metadata>
## Processed Sketches

- 001-notebook-home-layout
</metadata>
