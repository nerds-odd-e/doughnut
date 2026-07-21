---
phase: 03-cancel-extraction-preview-generation
plan: 03
subsystem: ui
tags: [vue, cancelable, extraction-preview, frontend-api, vitest, idempotency]

requires:
  - phase: 03-cancel-extraction-preview-generation/02
    provides: Cancelable runExtractionPreview + core REFN-03/04 cancel suite
  - phase: 01-shared-cancellation-contract
    provides: Exact-id Cancel + CancelableApiResult
provides:
  - Retry-fresh / idempotent / concurrent / create-note noncancelable edge coverage
  - frontend-api.mdc documents extraction-preview as second cancelable opt-in
affects:
  - Phase 4 create-note cancel (REFN-05) — must remain noncancelable until opted in
  - Future callers copying cancelable patterns from frontend-api.mdc

tech-stack:
  added: []
  patterns:
    - "Extract retry after cancel re-enters cancelable extractNotePreview with fresh signal"
    - "Create-note stays on noncancelable runWithBlockingApiLoading; Cancel absent while AI is creating note..."
    - "frontend-api.mdc lists layout + extraction-preview as the only product cancelable opt-ins"

key-files:
  created:
    - frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts
  modified:
    - frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts
    - .cursor/rules/frontend-api.mdc

key-decisions:
  - "No NoteRefinement.vue product fix — edges already green from 03-02 cancelable preview"
  - "Split cancel suite edges into dedicated file to stay under 250-line gate"
  - "Docs name extractNotePreview / AI is generating preview... as second allowed opt-in; forbid create-note cancel"

patterns-established:
  - "Bare NoteRefinement concurrent tests use direct DOM selects (avoid WithGlobalLoading wrapper type clash)"
  - "rg exclusivity: cancelable true only NoteRefinement.vue + clientSetup.ts under frontend/src"

requirements-completed: [REFN-03, REFN-04]

coverage:
  - id: D1
    description: Extract after cancel starts a fresh cancelable preview request
    requirement: REFN-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts#retries Extract with a fresh cancelable preview after cancel
        status: pass
    human_judgment: false
  - id: D2
    description: Double Cancel is idempotent; selection preserved; no toast
    requirement: REFN-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts#ignores a second Cancel click after accepted cancel
        status: pass
    human_judgment: false
  - id: D3
    description: Older concurrent blocker survives preview Cancel
    requirement: REFN-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts#keeps an older concurrent blocker after preview Cancel
        status: pass
    human_judgment: false
  - id: D4
    description: Create-note pending shows AI is creating note... without Cancel
    requirement: REFN-03
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts#create-note pending shows creating message without Cancel
        status: pass
    human_judgment: false
  - id: D5
    description: frontend-api.mdc documents extraction-preview cancelable opt-in; mutations remain forbidden
    requirement: REFN-03
    verification:
      - kind: other
        ref: .cursor/rules/frontend-api.mdc#Cancelable blocking calls
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-07-21
status: complete
---

# Phase 03 Plan 03: Preview Cancel Edges + Docs Summary

**Hardened extraction-preview cancel edges (retry-fresh, idempotent, concurrent, create-note noncancelable) and documented preview as the second frontend-api cancelable opt-in.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-07-21T09:25:04Z
- **Completed:** 2026-07-21T09:30:11Z
- **Tasks:** 2/2
- **Files modified:** 3

## Accomplishments

- Edge suite proves Extract retry after cancel, double-Cancel idempotency, concurrent survivor, and create-note Cancel-absent (D-10).
- No product Vue change required — 03-02 cancelable preview already satisfied the edges.
- `frontend-api.mdc` documents layout + extraction-preview as allowed read-only opt-ins; mutations / cancelable `runWithBlockingApiLoading` still forbidden.

## Task Commits

1. **Task 1: Green retry-fresh, idempotent, and create-note boundary edges** - `22fa49bd78` (test)
2. **Task 2: Document extraction-preview cancelable opt-in in frontend-api.mdc** - `463ba3eee6` (docs)
3. **Post-change-refactor: split cancel edge suite** - `fe97bc69dc` (refactor)

**Plan metadata:** `2f327f8ee5` (docs: complete plan)

## Files Created/Modified

- `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` — retry / idempotent / concurrent / create-note edges
- `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` — core REFN-03/04 cases only
- `.cursor/rules/frontend-api.mdc` — second cancelable opt-in + create-note forbid example

## Decisions Made

- No `NoteRefinement.vue` fix — edges green from existing cancelable preview path.
- Split edges into a dedicated spec so both files stay under the 250-line gate.
- Docs mirror the literal overload; do not invent a parallel cancelable helper.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Concurrent test used wrong layout fixture / helper types**
- **Found during:** Task 1
- **Issue:** Hand-built layout items lacked `children` (Vue render crash); typed select/extract helpers rejected bare `NoteRefinement` wrappers (vue-tsc pre-commit).
- **Fix:** Use `refinementLayoutItems`; drive concurrent case via direct DOM like the layout cancel suite.
- **Files modified:** cancel edges spec
- **Verification:** cancel + extractNote + layout cancel suites green
- **Committed in:** `22fa49bd78` / refined in `fe97bc69dc`

**2. [Rule 3 - Blocking] Cancel suite exceeded 250 lines after edges**
- **Found during:** post-change-refactor
- **Issue:** Combined cancel suite grew to 264 lines.
- **Fix:** Split edges into `NoteRefinement.extractionPreview.cancel.edges.spec.ts`.
- **Files modified:** core cancel spec + new edges spec
- **Verification:** both specs + regressions green
- **Committed in:** `fe97bc69dc`

**Total deviations:** 2 auto-fixed
**Impact on plan:** Test-only; no product scope creep.

## Issues Encountered

None beyond the deviations above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 3 complete for REFN-03/04.
- Phase 4 may start create-note cancel (`REFN-05`) / cohesion audit (`COHE-02`); create-note remains noncancelable until then.
- Do not start Phase 4 from this plan wrap-up.

## Post-change-refactor

- **Duplication / naming / shotgun / dead code:** none beyond expected toast-mock setup duplication across cancel specs (same as layout suite).
- **File size:** split cancel suite (131 + 167 lines).
- **Related tests:** cancel core (3), cancel edges (4), extractNote (15), layoutGeneration.cancel (6) — all pass.

## REFACTOR COMPLETE

## Self-Check: PASSED

- `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` — FOUND
- `.cursor/rules/frontend-api.mdc` — FOUND
- Commits `22fa49bd78`, `463ba3eee6`, `fe97bc69dc` — FOUND

---
*Phase: 03-cancel-extraction-preview-generation*
*Completed: 2026-07-21*
