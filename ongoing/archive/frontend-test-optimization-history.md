# Frontend UT test optimization — archive (2026-07-10)

Thirty-six optimization phases targeting the slowest Vitest unit tests (top 10% by file grouping), closed with a full re-profile.

## Before / after

| Metric | Before (baseline) | After (re-profile) | Change |
|--------|-------------------|--------------------|--------|
| Test count | 1574 | 1535 | −39 (merged redundant cases) |
| Suite wall (Vitest) | ~61s | ~63s | ~flat (within machine noise) |
| Process wall (incl. Nix) | ~70s | ~63s | −7s |
| Eligible after blacklist | 1574 | 1535 | No frontend Skip entries |
| Top 10% sum (n=158 → 154) | ~5086ms | **~2726ms** | **−2360ms (~46%)** |
| Suite result | 1574 passed | 1535 passed | all green |

Profile command: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json` (tee → `/tmp/frontend-profile-after.log`). Raw JSON: `ongoing/frontend-profile-results.json` (baseline), `ongoing/frontend-profile-results-after.json` (after) — **do not commit**.

Verify touched suites with browser mode: `CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>`.

## Top 10% after (n = ceil(1535 × 0.10) = 154)

Slowest 25 shown; full list in local `ongoing/frontend-profile-results-after.json`.

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 36 | `tests/notes/WikidataAssociationDialog.spec.ts` | shows the current wikidata ID in the input field |
| 2 | 35 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows LoadingModal during extract preview and hides on success or failure |
| 3 | 33 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | submits only checked descendants when parent is indeterminate ('remove') |
| 4 | 32 | `tests/pages/FolderPage.spec.ts` | retries cross-notebook folder move with merge after 409 conflict |
| 5 | 31 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows inline error when retry preview API fails |
| 6 | 31 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | saves scalar as list when user switches to list mode in popup |
| 7 | 30 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | rejects empty list items on save |
| 8 | 29 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | allows duplicate list items in popup save |
| 9 | 29 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | saves an empty list from popup |
| 10 | 28 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | shows LoadingModal while removing layout points and hides on success or failure |
| 11 | 28 | `tests/pages/FolderPage.spec.ts` | sends destinationNotebookId and newParentFolderId for cross-notebook folder move |
| 12 | 28 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | confirms retry when preview fields were edited, keeping edits on cancel and replacing on confirm |
| 13 | 28 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | removes non-contiguous selected layout points |
| 14 | 27 | `tests/notes/NoteEditableContent.spec.ts` | should preserve second edit when first save response arrives after second edit |
| 15 | 27 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | includes parent id when all descendants are selected again |
| 16 | 26 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | uses the current note id when note prop changes without remount |
| 17 | 26 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | emits valid aliases list edits from popup |
| 18 | 25 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'Canine' with action 'append' updates title as 'dog' |
| 19 | 25 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | preset dropdown for 'occupied image preset' shows options and sets key on selection |
| 20 | 25 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | saves list as scalar when user switches to text mode in popup |
| 21 | 25 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'Canine' with action 'replace' updates title as 'Canine' |
| 22 | 24 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | preset dropdown for 'occupied url preset' shows options and sets key on selection |
| 23 | 24 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | replaces preview fields when Ask AI to retry is clicked |
| 24 | 24 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | uses confirm when relationship note source does not resolve |
| 25 | 23 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows LoadingModal while retrying extract preview |

**Baseline slow tail** was dominated by `BookReadingPage` (~49 specs, 52–57ms each) and `PopButton`/`Modal` (~55–66ms). After optimization the slowest single test is **36ms** (WikidataAssociationDialog).

## Phases and commits

| Phase | Focus | Commit |
|-------|-------|--------|
| 1 | BookReadingPage | `5b42eff754` |
| 2 | NoteRefinement.extractNote | `f39c505455` |
| 3 | HorizontalMenu | `966e6b2850` |
| 4 | SearchDialog | `2dc72f0302` |
| 5 | FolderPage | `1f7c4ff98b` |
| 6 | NoteNewForm | `affea591e5` |
| 7 | propertyValuePopupModeSwitch | `641f4de343` |
| 8 | useBookReadingBootstrap | `ddd6e91685` |
| 9 | NoteEditableContent | `e3a2b79eb6` |
| 10 | NoteRefinement.layoutSelection | `643add6ffe` |
| 11 | WikidataAssociationDialog | `cd3f79ed85` |
| 12 | propertyKeyPresets | `ac2c77a147` |
| 13 | PopButton | `ac2c77a147` |
| 14 | AssimilationSettings | `f8971b35e0` |
| 15 | aliasesProperty | `af28574dce` |
| 16 | Questions | `af28574dce` |
| 17 | Modal | `af28574dce` |
| 18 | propertyRelationImageIndex | `c8f449d1b4` |
| 19 | RichMarkdownEditor.properties | `7701ff5101` |
| 20 | MemoryTrackerPageView | `0922d5d0e8` |
| 21 | propertyValuePopup | `7701ff5101` |
| 22 | InsertWikiLink | `0922d5d0e8` |
| 23 | Quiz | `0e8b4297a4` |
| 24 | FailureReportList | `d14c6f6eb5` |
| 25 | exportExtractRequest | `fea2cfee96` |
| 26 | NotebookPage | `71d35f73cb` |
| 27 | deleteNote.relationship | `d43d0cfcae` |
| 28 | removeLayout.loading | `e18cb61d80` |
| 29 | propertyTouchFocus | `b27010ee8e` |
| 30 | SeamlessTextEditor | `f662ddeeb9` |
| 31 | AssimilationPanel | `bad9baeade` |
| 32 | PdfBookViewer.gestureZoom | `fcf4da844c` |
| 33 | QuestionGenerationBatchStatus | `9a04c98b3e` |
| 34 | TextContentWrapper | `d2842b733a` |
| 35 | NoteRefinement.removeLayout | `286ef2206c` |
| 36 | NoteUndoButton | `15f40d8792` |

Support modules for phases 20/22: `0922d5d0e8`. Earlier exploratory batches (`0e82333346` … `bad9baeade`) preceded the named phase plan.

### Tactics that worked

- Replace `vi.waitFor` / `vi.waitUntil` (multi-second timeouts) with `flushPromises` + `nextTick` polls or fake timers.
- Minimal memory routers instead of full `routes` imports for Modal/PopButton/page specs.
- Hoist mount, mocks, and `querySelector` / `data-testid` helpers into `*TestSupport.ts` files.
- Merge redundant cases with `it.each` (loading success+failure, close-button+ESC, indeterminate parent extract/remove).
- Fake minimal PDF/EPUB bytes in bootstrap tests instead of loading real fixtures.
- `createDeferredGate` for in-flight API assertions instead of real timer waits.
- Split oversized specs (e.g. NoteEditableContent) into cohesive files under 250 lines.

### Blacklist candidates (not promoted)

See `ongoing/test-optimization-blacklist.md` — **no frontend Candidates** were proposed during this run. E2E-only entries remain.

## Grouping

Baseline: by file (36 groups) vs batches of 3 (53 groups) — chose **by file** (fewer groups; tie-break).
