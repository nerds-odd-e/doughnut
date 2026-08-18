# Plan: OKF title warning only on notes

**Status:** planned (slice 1 next)

**Goal:** Folder and notebook names `index` / `log` do not show the OKF-incompatible warning. Drop redundant tests left from the Readme/`README.md` work.

## Design

- `PathNameEditor` warns only when an opt-in prop is set. Enable it on note create and note title edit only.
- Test cleanup is leftover missed post-change-refactor: delete tests that duplicate another observable surface; keep unique codec / matching / E2E paths.

## Slices

### 1. Folder or notebook named index or log does not warn — Behavior — planned

**Pre:** Folder create/rename or notebook create/rename (not a note).  
**Trigger:** Name `index`, `index.md`, `log`, or `log.md` (any case).  
**Post:** No OKF-incompatible warning. Save succeeds. Creating or renaming a **note** to those titles still warns and still saves.

E2E: extend folder create (e.g. `note_creation.feature`) — create folder `index`, no warning, folder exists. Existing note create/rename scenarios stay green.  
Frontend: PathNameEditor default off; prop on `NoteNewForm` and `NoteEditableTitle` only.

### 2. Drop redundant OKF/readme tests — Structure — planned

No product behavior change. Remaining tests still pin the unique claims.

Remove:

- `emptyFolderAndDeadWikiGroupsStillReportWhenOkfTitleExists`
- Extra `@ParameterizedTest` health cases beyond one non-E2E match (keep metadata-when-empty, `indexical` negative, soft-deleted, and one matching title)
- `First note.md` assertion on the ZIP readme mapping test (note files stay covered elsewhere)
- Full sibling-tree table on the create-`index` E2E; keep warning + note exists

Keep: ZIP insert / canonicalize `readme` → `Readme` / leave other type; PathNameEditor exact warning message; E2E note create **and** rename; E2E health lists `index`.

Enables nothing further — leftover hygiene from the previous plan so the suite does not keep paying for duplicate pins.

## Out of scope

- Shared FE/BE reserved-title module
- Replacing the E2E ZIP parser
- Reopening `index` / `log` as hard-reserved titles (locked in Proposed ADR 0004: warn, save, filename-as-title)
- Accepting ADR 0004
