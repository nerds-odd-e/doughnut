# State

## Current

- **Active plan:** `.planning/quick/001-notebook-workspace-home/PLAN.md`
- **Phase:** 4 next — note and folder stay recognizably different from notebook home
- **Next:** Phase 4 smoke/regression across note vs folder vs notebook main landmarks

## Context

Notebook page should feel like overview / command center (Notion workspace home). Primary job: see/edit notebook `indexContent` + properties.

## Sketch decisions

- **001 winner C:** Default **Home** = index + light properties; **Settings** = management / settings / search-indexing.
- **Wrap-up:** `.cursor/skills/sketch-findings-doughnut/` + `.planning/sketches/WRAP-UP-SUMMARY.md`

## Shipped

- Index (notebook + folder) uses shared `useDebouncedTextAutosave` with note body; Save button removed.
- Notebook page Home / Settings tabs: Home = title + description cue + index canvas; Settings = `NotebookWorkspaceSettings` (book, management, settings, indexing).
- Phase 3 verification: Home index autosave E2E; admin controls asserted absent on Home / present only in Settings.
