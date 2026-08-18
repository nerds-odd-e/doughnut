# Plan: Readme as README.md

**Status:** in progress (slice 2 next)

**Goal:** ZIP maps notebook/folder readme to `README.md` with `type: Readme`. Notes titled `index` / `log` warn, not block. ADR 0004 Decision matches that shape.

## Design

- Export-only `type: Readme` wrap. Do not call persist helpers (`NoteConceptType`) from readme save. Do not Flyway-backfill readme columns.
- `NotebookZipBuilder` already writes notebook and folder readmes through one `writeDirectory` path — one codec change covers both.
- Title warning reuses `PathNameEditor`’s existing non-blocking `displayWarning` (do not add a Bean Validation reject).
- Health lint is a second discovery path for titles the user is not currently editing.

## Slices

### 1. Lock Readme → README.md in ADR 0004 — Structure — done

Proposed ADR 0004 Decision + Validation, and ADR 0001 **Readme** glossary row, now map container Readme to `README.md` / `type: Readme`. Status stays Proposed.

**Learning:** Validation still required missing `index.md`; amended so missing listing is conformant and concept `index.md` / `log.md` warn only.

### 2. ZIP writes readme as README.md with type: Readme — Behavior — planned

**Pre:** Notebook and/or folder with a non-blank readme; another folder with a blank readme.  
**Trigger:** Export catalog ZIP.  
**Post:** Non-blank readme is `README.md` in that directory; leading `type: Readme`; no `index.md` from readme. Blank readme omits the file. Author YAML on the readme is preserved.

E2E: `notebook_export.feature` (unzip the download; assert `README.md` / no `index.md`). Unit: `NotebookZipBuilderTest` (root + nested; missing type inserted; existing fence preserved).

### 3. Creating or renaming a note to index or log warns and still saves — Behavior — planned

**Pre:** Note create or title edit.  
**Trigger:** Title `index`, `index.md`, `log`, or `log.md` (any case).  
**Post:** Non-blocking warning that the portable tree may be OKF-incompatible. Save succeeds. `readme` / `readme.md` still reject.

E2E: `note_creation.feature` (create `index` — warning + note exists) and `note_edit.feature` (rename — warning + title kept). Frontend: PathNameEditor warning predicate.

### 4. Notebook health lists notes whose titles occupy OKF reserved basenames — Behavior — planned

**Pre:** Notebook with a note titled `index` (or `log` / `*.md` variants).  
**Trigger:** Run lint.  
**Post:** A warning-severity finding names those notes. No auto-fix. Empty-folder / dead-wiki groups unchanged.

E2E: `notebook_health.feature`. Rule: new `HealthRule` (warning, not auto-fixable).

### 5. Close D1/C2 on the tracker — Structure — planned

Mark D1/C2 closed (and P7 / P10 as “omit listing; warn on index/log”) in [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) and SEED-003. Drop spent export/docs that treat readme as `index.md`. When this plan is fully executed, remove spent planning history from this directory.

## Out of scope

- P1–P5, P8–P9, O1–O4 listing/`log.md` generation
- Accepting ADR 0004
- Git accept (ADR 0002)
- Persisting or backfilling `type: Readme` on stored readme columns
- Hard-reserving `index` / `log` as note titles
