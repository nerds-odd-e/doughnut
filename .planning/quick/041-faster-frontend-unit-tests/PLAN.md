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
Status: done
Proof: as slice 1.

Merge the remaining `RichMarkdownEditor.*` files (value kinds: image, wiki
links, relation image index, aliases, overlaps, lists, nested metadata,
properties, memory tracking; base and guarded mode switch) by responsibility.

Accepted proof: 11 files → 6 (`RichMarkdownEditor.spec.ts` body and mode
switch; `.frontmatter`; `.listProperties` with aliases/overlaps as `it.each`;
`.propertyWikiLinks`; `.propertyRowEditing`; `.propertyMemoryTracking`), all
≤250 lines; five one-caller support files removed. 65 → 63 tests: four compose
tests became one `it.each` (scalar/list) keeping every assertion. Back-to-back
focused pairs (11 vs 6 files): 6.53/4.72s, 5.62/4.89s.
`cd frontend && CURSOR_DEV=true nix develop -c pnpm exec vitest run --browser=chromium --browser.headless tests/components/form/RichMarkdownEditor*.spec.ts`
→ 9 files, 88 tests pass; `pnpm -C frontend exec vue-tsc --noEmit` passes.

### 3. RecallPage specs share one setup
Type: Structure
Status: done
Proof: as slice 1.

Merge the 11 `RecallPage*` files by recall-page responsibility, declaring the
three shared `vi.mock` calls once per file.

Accepted proof: 11 files → 5 (`RecallPage.spec.ts` loading and daily probe,
real timers; `.dueQueue` activation/load more/diligent; `.queueProgress` just
review/treadmill; `.answering` threshold/speaking practice/thinking time;
`.spelling`; fake timers), all ≤250 lines. 36 tests kept. Back-to-back focused
pairs (11 vs 5 files): 8.18/5.18s, 5.58/4.27s, 8.16/5.10s.
`cd frontend && CURSOR_DEV=true nix develop -c pnpm exec vitest run --browser=chromium --browser.headless tests/pages/RecallPage*.spec.ts`
→ 5 files, 36 tests pass (4.35s); `pnpm -C frontend exec vue-tsc --noEmit` passes.

### 4. NoteEditableContent specs share one setup
Type: Structure
Status: done
Proof: as slice 1.

Merge the 9 `NoteEditableContent*` files by responsibility (paste and paste
choice; debounced save; content properties and normalization).

Accepted proof: 10 files → 6 (`NoteEditableContent.spec.ts` switching notes;
`.debouncedSave`; `.saveResponse`; `.paste`; `.pasteChoice`;
`.pasteChoiceActionBar`), all ≤250 lines; one async mount helper and shared
`mountAndPaste`/`choiceShown`/`useOriginalText` replace per-file copies.
48 → 44 tests, each removal with a named survivor (in-flight "persists a newer
ordinary edit…"; "should auto-save … without blur" gained the not-before-
debounce assertion; `it.each` dismissal "…on explicitDismissal"; the two
rich-mode replace tests merged keeping all assertions). Alternating focused
runs (10 vs 5–6 files): 5.50/5.30/5.57s vs 4.45/4.36/4.26s.
`cd frontend && CURSOR_DEV=true nix develop -c pnpm exec vitest run --browser=chromium --browser.headless tests/notes/NoteEditableContent*.spec.ts`
→ 6 files, 44 tests pass; `pnpm -C frontend exec vue-tsc --noEmit` passes.
Full `pnpm frontend:test` over slices 1–4: 323 files, 1929 tests pass.

### 5. Mid-size sibling families share one setup
Type: Structure
Status: done
Proof: as slice 1, per family.

Apply the same merge to the 4–6-file families listed in the family analysis.
If this exceeds the slice budget, deliver it in two halves (page-level
families, then component-level families).

Delivered in three parallel, file-disjoint parts (pages; notes and toolbars;
components). Accepted proof, all files ≤250 lines, every removed test naming
its survivor or folded into an `it.each` row with exact per-row assertions:

- Pages: FolderPage 6 → 3, MemoryTrackerPageView 5 → 3, BookReadingPage 7 → 5
  (18 → 11 files, 106 → 101 tests). Alternating focused runs 5.86/5.35/5.26s
  → 5.37/4.49/4.49s. `readingControlPanel.*` stay apart (together >250 lines);
  `BookReadingPage.snap.spec.ts` (404 lines) was already over the limit and is
  untouched.
- Notes and toolbars: NoteToolbar 6 → 3, NoteAudioTools 5 → 3, NoteNewForm
  4 → 2, MainMenu 4 → 3 (19 → 11 specs plus one support file, 92 → 91 tests).
  Alternating runs 6.66/6.53/8.01s → 5.20/5.30/5.06s.
- Components: NoteRefinement 11 → 5, AssimilationPanel 4 → 3, SearchResults
  5 → 2, DiffView 4 → 1 (24 → 11 specs plus two support files, 96 → 73
  tests). NoteRefinement alternating runs 6.16/4.88/5.13s → 4.78/3.73/3.63s.

Focused commands: `cd frontend && CURSOR_DEV=true nix develop -c pnpm exec vitest run --browser=chromium --browser.headless <family globs>`;
`pnpm -C frontend exec vue-tsc --noEmit` passes.

### 6. Re-profile and record the outcome
Type: Structure
Status: done
Proof: two whole-suite runs under baseline conditions; before/after files,
tests, and duration recorded here.

Update the frontend profiling note in
`.planning/test-optimization-candidates.md` with the measured result and any
remaining candidate.

Accepted proof: throwaway worktree at `8d79d96a09` vs story branch at
`8df4a87262`, alternating `CURSOR_DEV=true nix develop -c pnpm frontend:test`,
load average 4.6–12.4:

| Round | Old (346 files, 1937 tests) | New (295 files, 1900 tests) |
| --- | --- | --- |
| 1 | 47.28s | 37.49s |
| 2 | 46.61s | 42.24s |
| 3 | 55.84s | 39.58s |

Mean Vitest duration 49.9s → 39.8s (~20%); all runs pass. The candidates
note records the result and the unpicked opportunities; no new candidate.

## Current decisions

- None beyond the merge rules above.

## Learnings

- Slice 1 checkpoint: per-file saving is real (~210ms per removed file in the
  focused run), so merging continues. Whole-suite timing is unreliable while
  other sessions load the machine (load average 40–120 observed): check
  `sysctl -n vm.loadavg` and measure the whole suite only when quiet.
- Merging also exposes one-caller support helpers; fold them into the spec
  rather than keep a helper file per old spec.
- The refactor skill's file-size check (`refactor-checks.md`, 250 lines) bounds
  merging: group by responsibility and shared setup, and split along cohesive
  seams when a merged file exceeds 250 lines.
- Fair focused timing on a loaded machine: copy the HEAD versions into a
  temporary sibling folder (with their support files) and alternate old/new
  runs; separate runs varied by more than 2s.
- A file's timer mode (real vs fake) is part of its shared setup; RecallPage
  loading tests need real timers.
- A module-level `vi.mock` decides which files can merge: files that need the
  real module (e.g. `AssimilationPanel.loadingModal`) stay apart.
- Once default SDK mocks move into a shared `beforeEach`, add
  `vi.restoreAllMocks()` in `afterEach` so tests can assert exact call counts.
- zsh does not split an unquoted `$F` file list; use brace expansion or `${=F}`.
