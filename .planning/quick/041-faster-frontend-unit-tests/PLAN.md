# Run frontend unit tests faster through cohesive test design

## Source

- Identity: SEED-039#story-4.
- Source: [story](../../seeds/SEED-039-faster-ci-feedback.md#story-4). The owner
  skipped story refinement and asked for the plan to be made during execution,
  through one normal `dough-test-optimization` round on `pnpm frontend:test`.
- Evidence reused: the frontend profiling note in
  [test-optimization candidates](../../test-optimization-candidates.md#frontend-profiling-note)
  (2026-09-19): ~88% of wall time is per-file cost (browser page setup plus
  module-graph import), ~190–280ms per extra spec file; `--no-isolate` was
  rejected (216 of 339 files failed, 5× slower).

## Goal and scope

Contributors wait less for trustworthy frontend unit-test results, locally and
in CI. The speedup comes from fewer, more cohesive spec files: sibling files that
test one component with the same harness and mocks become one file per
behavioral responsibility, and their duplicated `vi.mock` / `beforeEach` /
mount boilerplate goes away.

Excluded: product code changes; Vitest/Vite configuration experiments already
rejected (`--no-isolate`); CI sharding or worker settings (SEED-039 story 3 owns
CI distribution); deleting behavior proof without named surviving proof.

Assumptions:

- Sibling files named `<Component>.<aspect>.spec.ts` share their harness and
  mocks. Checked on `8d79d96a09`: all 11 `RecallPage*` files declare the same
  three `vi.mock` calls; all 21 `RichMarkdownEditor.*` files use
  `createRichMarkdownEditorTestHarness`; 8 of 9 `NoteEditableContent*` files mock
  only `usePopups`.
- Per-file cost still dominates. Slice 1 re-measures this before later slices
  rely on it.

## Baseline

Revision `8d79d96a09`, macOS, local Nix shell, warm pnpm/Vite caches, no filters,
default local mode (`fileParallelism` on, no retry, headless Chromium):

`CURSOR_DEV=true nix develop -c pnpm frontend:test`

| Run | Files | Tests | Vitest duration | Command total |
| --- | --- | --- | --- | --- |
| 1 | 346 | 1937 | 59.73s | 75.8s |

## Family analysis

Largest sibling families (files per component, `8d79d96a09`):

- `tests/components/form/RichMarkdownEditor*` — 21 (property panel, property
  value dialog, key presets, focus/location, value kinds, metadata).
- `tests/pages/RecallPage*` — 11 (activation, daily probe, diligent, just
  review, load more, speaking practice, spelling, threshold, treadmill, view
  history thinking time, base).
- `tests/notes/NoteEditableContent*` — 9 (paste, paste choice ×3, debounced save
  ×2, memory tracker, relation property, HTML normalization, base).
- 4–6 files each: `FolderPage`, `NoteToolbar`, `MemoryTrackerPageView`,
  `BookReadingPage` (+2 `readingControlPanel`), `NoteAudioTools`,
  `SearchResults`, `NoteRefinement` (+2 `extractNote`), `MainMenu`,
  `NoteNewForm`, `AssimilationPanel`, `DiffView`.
- 2–3 files each: ~25 smaller families; not planned unless slice 5's
  reassessment shows they pay.

## Outside-in proof

- Every retained `it` still runs: executed test count per family stays equal
  (or drops only by named duplicates whose surviving proof is cited).
- `CURSOR_DEV=true nix develop -c pnpm frontend:test` passes, and the frontend
  typecheck required by the `frontend` skill passes.
- The whole-suite run under baseline conditions is faster than the baseline,
  repeatably (two runs).

## Experiment loop (every slice)

1. **Hypothesize:** these sibling files pay per-file page and import cost for
   one component; merged by responsibility they pay it once.
2. **Try and measure:** run the family's files focused before and after
   (`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --browser=chromium --browser.headless <family paths>`),
   record files, tests, and duration.
3. **Decide:** keep only when the family is simpler (less boilerplate, clear
   responsibility per file) and focused time drops; otherwise undo and record.
4. **Reassess:** after slice 1, compare whole-suite time with the baseline. If
   the gain per removed file is inside noise, stop merging and record the
   finding before slice 2.

Merge rules: group by behavioral responsibility, never into one file per
component merely for count; hoist shared mocks and setup once; keep test names
readable as behavior; no `.only`/`.skip`.

## Slices

### 1. RichMarkdownEditor property-panel interaction specs share one setup
Type: Structure
Status: done
Proof: focused family run before/after (files, tests, duration);
`pnpm frontend:test` and frontend typecheck pass; whole-suite time vs baseline
recorded for the reassessment.

Merge the property value dialog, key presets, focus, touch focus, panel
location, rename/delete location, and draft refresh files
(`RichMarkdownEditor.propertyValueDialog*`, `.propertyKeyPresets*`,
`.propertyFocus`, `.propertyTouchFocus`, `.propertyPanelLocation`,
`.propertyRenameLocation`, `.propertyDeleteLocation`, `.propertyDraftRefresh`)
into responsibility-named files. Enables slice 2's decision.

Accepted proof: 11 files → 3 (`RichMarkdownEditor.propertyValueDialog`,
`.propertyLocation`, `.propertyEntry`); three one-caller support files removed.
27 tests → 25: three panel open/close tests became one round-trip test keeping
every assertion (route visit still covered by "visiting noteProperty").
Focused before (11 files): 5.27s / 4.91s; after (3 files): 3.49s / 3.24s /
3.19s — ~210ms per removed file, matching the profiling note.
`cd frontend && CURSOR_DEV=true nix develop -c pnpm exec vitest run --browser=chromium --browser.headless tests/components/form/RichMarkdownEditor.property{ValueDialog,Location,Entry}.spec.ts`
passes; `pnpm -C frontend exec vue-tsc --noEmit` passes; full `pnpm frontend:test`
passed (338 files, 1937 tests) before the refactor pass.

### 2. RichMarkdownEditor property-value specs share one setup
Type: Structure
Status: planned
Proof: as slice 1.

Merge the remaining `RichMarkdownEditor.*` files (value kinds: image, wiki
links, relation image index, aliases, overlaps, lists, nested metadata,
properties, memory tracking; base and guarded mode switch) by responsibility.

### 3. RecallPage specs share one setup
Type: Structure
Status: planned
Proof: as slice 1.

Merge the 11 `RecallPage*` files by recall-page responsibility, declaring the
three shared `vi.mock` calls once per file.

### 4. NoteEditableContent specs share one setup
Type: Structure
Status: planned
Proof: as slice 1.

Merge the 9 `NoteEditableContent*` files by responsibility (paste and paste
choice; debounced save; content properties and normalization).

### 5. Mid-size sibling families share one setup
Type: Structure
Status: planned
Proof: as slice 1, per family.

Apply the same merge to the 4–6-file families listed in the family analysis.
If this exceeds the slice budget, deliver it in two halves (page-level
families, then component-level families).

### 6. Re-profile and record the outcome
Type: Structure
Status: planned
Proof: two whole-suite runs under baseline conditions; before/after files,
tests, and duration recorded here.

Update the frontend profiling note in
`.planning/test-optimization-candidates.md` with the measured result and any
remaining candidate.

## Current decisions

- None beyond the merge rules above.

## Learnings

- Slice 1 checkpoint: per-file saving is real (~210ms per removed file in the
  focused run), so merging continues. Whole-suite timing is unreliable while
  other sessions load the machine (load average 40–120 observed): check
  `sysctl -n vm.loadavg` and measure the whole suite only when quiet.
- Merging also exposes one-caller support helpers; fold them into the spec
  rather than keep a helper file per old spec.
- zsh does not split an unquoted `$F` file list; use brace expansion or `${=F}`.
