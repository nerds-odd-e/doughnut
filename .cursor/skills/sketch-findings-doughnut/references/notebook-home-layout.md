# Notebook home layout

## Design Decisions

- **Winner C — Home / Settings tabs:** Default **Home** shows notebook title, light index property pills (`title_pattern`, aliases), and the **indexContent** editor as the primary canvas. **Settings** holds today’s Notebook Management / Settings / Indexing (search) cards so admin does not compete with the intro document.
- Rejected **A (hero + gear)** as less explicit for discovering settings; rejected **B (property rail)** as denser and closer to a permanent admin column.
- **Autosave (post-sketch product decision):** No “Save index” button; index uses the same debounced autosave path as note body (`useDebouncedTextAutosave`, 1000ms, flush on blur).

## CSS Patterns

- Shared app chrome: `grid` with sidebar + global bar (matches `NotebookSidebarLayout` mentally).
- Home tab content: max-width readable column (~52rem), surface editor with border/radius.
- Property pills: rounded-full chips with mono values for `title_pattern` / aliases.
- Settings: stacked `.settings-card` panels (bordered surfaces) only on the Settings tab.

## HTML Structures

- Top: editable `notebook.name` + tab row (`Home` | `Settings`).
- Home: property strip → index editor (`indexContent`) → autosave status (no primary Save).
- Settings: description / `skipMemoryTrackingEntirely` / Move / Share / Attach book / search index actions.

## What to Avoid

- Treating notebook home like a note (`NoteToolbar`, full-height document-only chrome).
- Vertical settings stack above or beside the index on first paint (today’s smell).
- Copying notebook hero/tabs onto **folder** unless a later plan says so — folder stays container/admin.
- Duplicating autosave debounce logic outside `useDebouncedTextAutosave`.

## Origin

Synthesized from sketches: 001  
Source files available in: `sources/001-notebook-home-layout/`
