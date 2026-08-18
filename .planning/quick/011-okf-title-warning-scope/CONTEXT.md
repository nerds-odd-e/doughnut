# OKF title warning scope (follow-up)

**Status:** planned  
**Trigger:** Inspection of the Readme-as-`README.md` / OKF title-warning work (commits `a5ae25f863` … `2eaf85496c`).  
**Do not execute until asked.**

## What that work got right

- ZIP maps non-blank notebook/folder readme to `README.md` with export-only `type: Readme`; blank omits the file.
- Notes titled `index` / `log` (and `.md` variants) still save; PathNameEditor and health warn.
- `readme` / `readme.md` stay hard-reserved.
- `ExportReadmeMarkdown` + `NoteLeadingFrontmatter.ensureTypeKey` is the right persist-vs-export split.
- ADR 0004 stays Proposed; D1/C2 closed on the tracker.

## Meaningful issues

### Bug: OKF warning fires for folders and notebooks

`PathNameEditor` is shared by note create/rename **and** `FolderNewForm`, `NotebookNewForm`, `FolderPage`, and `NotebookPageView` (`AutosavingPageNameEditor`). The warning is hardcoded, so naming a **folder** or **notebook** `index` / `log` shows “portable tree may be OKF-incompatible”.

OKF reserved names are **concept files** `index.md` / `log.md`. A folder becomes a directory; a notebook name is the catalog title / ZIP download name, not a concept file. The warning is a false positive there.

### Redundant tests (missed post-change-refactor)

- `OkfIncompatibleTitleHealthRuleTest.emptyFolderAndDeadWikiGroupsStillReportWhenOkfTitleExists` — rules evaluate independently; sibling rule tests already cover those groups.
- Five `@ParameterizedTest` Spring lints for Set membership (`index`, `INDEX`, `index.md`, `log`, `LOG.MD`). E2E already lists `index`. Keep **one** unit case that E2E does not cover (e.g. case `INDEX`).
- `NotebookZipBuilderTest` first case still asserts `First note.md` body — leftover from the old “index.md and root notes” test; other tests already pin note files.
- E2E “Creating a note titled index” re-asserts the full sibling note tree; unique claims are warning + note exists.

### Not in this plan

- FE vs BE duplicated title sets — same four strings, different subsystems; do not extract a shared module.
- Custom E2E ZIP parser (`readZipEntries.ts`) — adequate for Java `ZipOutputStream`; replacing it is speculative.
- Health `String.trim()` vs `DisplayNamePathSeparators` — persisted titles already go through `DisplayName`.
- Reserving `README.md` inside `NotebookExportFilenames` — `readme` / `readme.md` titles cannot be saved.
- Reopening `index` / `log` as hard-reserved titles (locked in Proposed ADR 0004: warn, save, filename-as-title).
- Accepting ADR 0004.

## Design for the warning

Opt-in on `PathNameEditor` (default **off**). Turn it on only for note title editors (`NoteNewForm`, `NoteEditableTitle`). Folder and notebook editors stay off. Do not key off `editorDataTest` (notebook create still defaults to `note-title`).
