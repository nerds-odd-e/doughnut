# Frontend slow unit test optimization

Profiled with `CI=true npx vitest run --browser=chromium --reporter=json` (JSON at `/tmp/frontend-vitest-profile.json`).
Suite wall time ~25s; 1370 tests.

**Grouping:** per test file (6 groups for top 10 — smaller batch than one group of 10).

**Optimization rules (all phases):**

- Remove or simplify redundant tests first.
- Strictly no fixed-time waits (`sleep`, `setTimeout` used only to wait a duration, `vi.waitFor` with excessive timeout as a substitute for sleep).
- Flaky = failure; tests must be deterministic.

| # | Time | File :: test |
|---|------|----------------|
| 1 | 2.106s | `useThinkingTimeTracker.spec.ts` :: starts timer after nextTick and requestAnimationFrame |
| 2 | 2.076s | `FullScreen.spec.ts` :: enters fullscreen mode when button is clicked |
| 3 | 1.902s | `TextArea.spec.ts` :: expands based on content up to 'autoExtendUntil' limit |
| 4 | 1.208s | `NoteExportForm.spec.ts` :: downloads graph JSON when download button is clicked |
| 5 | 1.123s | `SearchDialog.spec.ts` :: calls moveNoteToNotebookRootInNotebook with notebook id after confirm |
| 6 | 1.123s | `SearchDialog.spec.ts` :: calls moveNoteToFolder with folder id after confirm |
| 7 | 1.122s | `InsertWikiLink.spec.ts` :: does not call the inserter when Add a new relationship note is clicked |
| 8 | 1.122s | `SearchDialog.spec.ts` :: shows confirm when move is blocked by soft-deleted title at destination |
| 9 | 1.120s | `InsertWikiLink.spec.ts` :: calls the registered inserter with a wiki link text when Insert as a wiki link is clicked |
| 10 | 1.119s | `InsertWikiLink.spec.ts` :: calls the wiki-property inserter when Add wiki link as a new property is clicked |

---

### Phase 1: Speed up useThinkingTimeTracker.spec.ts
Status: done

Target: `frontend/tests/composables/useThinkingTimeTracker.spec.ts` — slowest case ~2.1s (`starts timer after nextTick and requestAnimationFrame`; also `resumes when window gains focus` ~0.93s).

Remove redundant timing/rAF tests if covered elsewhere; mock timers (`vi.useFakeTimers`) instead of real rAF waits; no sleep.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/composables/useThinkingTimeTracker.spec.ts`

---

### Phase 2: Speed up FullScreen.spec.ts
Status: planned

Target: `frontend/tests/common/FullScreen.spec.ts` — `enters fullscreen mode when button is clicked` (~2.08s).

Slim browser fullscreen setup; remove duplicate fullscreen coverage; no sleep.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/common/FullScreen.spec.ts`

---

### Phase 3: Speed up TextArea.spec.ts
Status: planned

Target: `frontend/tests/components/form/TextArea.spec.ts` — `expands based on content up to 'autoExtendUntil' limit` (~1.9s).

Reduce DOM churn / large content fixtures; merge redundant resize tests; no sleep.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/TextArea.spec.ts`

---

### Phase 4: Speed up NoteExportForm.spec.ts
Status: planned

Target: `frontend/tests/notes/NoteExportForm.spec.ts` — `downloads graph JSON when download button is clicked` (~1.21s).

Mock download / graph build; avoid heavy graph fixtures; dedupe export tests.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteExportForm.spec.ts`

---

### Phase 5: Speed up SearchDialog.spec.ts (top-10 cases)
Status: planned

Target: `frontend/tests/links/SearchDialog.spec.ts` — three top-10 cases (~1.12s each) plus other ~1.1s cases in same file.

Shared setup helper; remove redundant dialog flow tests; no debounce sleep — use fake timers or trigger completion directly.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/SearchDialog.spec.ts`

---

### Phase 6: Speed up InsertWikiLink.spec.ts (top-10 cases)
Status: planned

Target: `frontend/tests/links/InsertWikiLink.spec.ts` — three top-10 cases (~1.12s each).

Shared mount helper; remove redundant inserter tests; no sleep.

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/InsertWikiLink.spec.ts`

---
