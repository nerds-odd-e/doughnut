# Sketch Manifest

## Design Direction

Notebook page as an **overview / command center** (Notion workspace home): land on identity + **index content** (intro) and properties. Distinct from note (document editor) and folder (container admin). Shared sidebar chrome stays; differentiate the main column. Demote management / settings / search-indexing so they do not compete with the index canvas.

**Winner (001):** Variant **C** — Home / Settings tabs.

**Autosave:** Index markdown uses the same debounced autosave as note body (`useDebouncedTextAutosave`); no Save button.

## Reference Points

- Notion workspace home
- Current Doughnut: `NotebookPageView` + `ScopedIndexNoteEditor` (`indexContent`, `title_pattern`, settings stack)

## Sketches

| # | Name | Design Question | Winner | Tags |
|---|------|-----------------|--------|------|
| 001 | notebook-home-layout | Does notebook home layout read as command center vs note/folder? | **C** — Home / Settings tabs | layout, notebook, home |
