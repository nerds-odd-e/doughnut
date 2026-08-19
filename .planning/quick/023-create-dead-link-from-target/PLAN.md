# Create from dead wiki link uses target identity

**Status:** in progress (slice 1 done)  
**Type:** ad-hoc bugfix (not on the GSD roadmap)

## Bug

Creating a note from a dead wiki link used **display** (`displayText`) as the new note title. Slice 1 names create from `targetToken`. Folder still defaults to the source note’s leaf folder.

When display and target differed, the new note did not match the link identity, so the dead link stayed dead after create. Path-shaped targets (`/` in `targetToken`) still use the old create path until slice 2.

The existing E2E (`[[WikiLinks E2E Missing]]`) only covers target === display; slice 1 added a piped case.

**Point-at-existing is out of scope.** Search prefill and rewrite already keep display and change the target. This plan is **create** only.

## Design

Wiki identity is the **target**, not the display ([ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) **Wiki link**; [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) `[[target]]` / `[[target\|display]]` and `[display](/folder/File.md)`).

- Prefill **title** from `targetToken` when it does not contain `/`. Folder stays the source-note leaf folder (unqualified wiki resolves by title in the notebook).
- After create, wiki-title cache refresh on the source note should make the link **live** while **display stays**. Do not rewrite the authored token on create.
- If `targetToken` contains `/`, **do not create**. Warn and stop. Path-shaped create is future work (do not parse path into title/folder in this plan).

## Out of scope

- Creating a note from a path-shaped target (`/` in `targetToken`) — future plan.
- Qualified `Notebook:Title` create into another notebook.
- Changing point-at-existing search prefill (display).

## Slices

### 1. Piped wiki create uses the target title

- **Type:** Behavior
- **Status:** done
- Create names the new note from `targetToken`. Chooser still shows display. Folder unchanged (source leaf).

**Learning:** E2E needs a separate step when click display ≠ new title (`I create a new note titled {string} by following the dead wiki link displayed as {string}`).

### 2. Warn and do not create when the target contains `/`

- **Type:** Behavior
- **Status:** planned
- **Pre-condition:** A note contains a dead wiki link whose `targetToken` contains `/` (path Markdown or path-shaped wiki).
- **Trigger:** Follow the dead link and choose create a new note.
- **Post-condition:** The user sees a warning and no note is created (create form does not open). Point-at-existing remains available.

**Tests**

- E2E in `e2e_test/features/note_topology/path_markdown_link.feature` (existing dead path-Markdown fixture).
- Frontend unit: create modal with `targetToken` containing `/` — create click warns, does not show `note-new-form`.

**Impl:** on create click, if `targetToken` includes `/`, `popups.alert` (or the same warning pattern the app already uses) and stay on the chooser. Do not parse the path.

## Jidoka

- After slice 1, piped wiki (no `/` in target) is fixed; path-shaped targets can still create under the old display-title bug until slice 2.
- Do not start path-shaped create or cross-notebook qualified create without a new plan.
