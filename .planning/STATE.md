# State

## Current

- **Active plan:** `.planning/quick/001-notebook-workspace-home/PLAN.md`
- **Phase:** 3 next — thin verification that index stays primary on Home and admin stays on Settings (Phase 2 already demoted admin)
- **Next:** Phase 3 verification / optional property-strip (sketch 002); then Phase 4 note/folder distinctness

## Context

Notebook page should feel like overview / command center (Notion workspace home). Primary job: see/edit notebook `indexContent` + properties.

## Sketch decisions

- **001 winner C:** Default **Home** = index + light properties; **Settings** = management / settings / search-indexing.
- **Wrap-up:** `.cursor/skills/sketch-findings-doughnut/` + `.planning/sketches/WRAP-UP-SUMMARY.md`

## Shipped

- Index (notebook + folder) uses shared `useDebouncedTextAutosave` with note body; Save button removed.
- Notebook page Home / Settings tabs: Home = title + description cue + index canvas; Settings = `NotebookWorkspaceSettings` (book, management, settings, indexing). E2E `notebook_workspace_home.feature`.
