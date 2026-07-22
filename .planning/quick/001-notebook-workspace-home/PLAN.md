# Notebook as workspace home — shipped

**Status:** complete (Phases 1–4)

Notebook page is a workspace home (Home: index primary; Settings: admin), distinct from note (document toolbar) and folder (container admin — no Home/Settings tabs).

## Pointers

- Product: `NotebookPageView`, `NotebookWorkspaceSettings`, `ScopedIndexNoteEditor` + shared `useDebouncedTextAutosave`
- E2E: `e2e_test/features/notebooks/notebook_workspace_home.feature`, `e2e_test/features/note_view/workspace_surface_landmarks.feature`
- Design: `.planning/sketches/` (winner C), `.cursor/skills/sketch-findings-doughnut/`
