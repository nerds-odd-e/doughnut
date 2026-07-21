---
phase: 04-enforce-safe-blocking-boundaries
plan: 02
subsystem: docs
tags: [cohe-02, frontend-api, classification-inventory, cancelable-allowlist, blocking-ui]

requires:
  - phase: 04-enforce-safe-blocking-boundaries
    provides: REFN-05 create-note Cancel-absent proof (04-01)
  - phase: 04-enforce-safe-blocking-boundaries
    provides: Verified Call-Site Inventory seed (04-RESEARCH)
provides:
  - Living COHE-02 classification inventory in frontend-api.mdc
  - Cancelable allowlist limited to layout + extraction-preview
  - Intentionally noncancelable map including create-note REFN-05 safety and book-layout AI ADPT-01 candidate
  - Nonblocking export / thin-bar classifications (D-08)
affects:
  - 04-03 cancelable allowlist guard
  - Future ADPT-01 broader Cancel adoption

tech-stack:
  added: []
  patterns:
    - Persist blocker classifications in frontend-api.mdc (living agent contract), not planning-only diary

key-files:
  created: []
  modified:
    - .cursor/rules/frontend-api.mdc

key-decisions:
  - "Seeded inventory tables directly from 04-RESEARCH Verified Call-Site Inventory"
  - "Book-layout AI suggest marked intentionally noncancelable + ADPT-01 candidate (no Cancel opt-in)"
  - "Create-note Cancel absence documented as REFN-05 intentional safety, not a missing opt-in"

patterns-established:
  - "COHE-02 inventory lives under Blocking classification inventory in frontend-api.mdc with three categories"

requirements-completed: [COHE-02]

coverage:
  - id: D1
    description: frontend-api.mdc inventory covers cancelable, intentionally noncancelable, and nonblocking classifications (D-05, D-10)
    requirement: COHE-02
    verification:
      - kind: other
        ref: .cursor/rules/frontend-api.mdc#Blocking classification inventory (COHE-02)
        status: pass
    human_judgment: false
  - id: D2
    description: Cancelable allowlist names only layout + extraction-preview (D-06); suites remain green
    requirement: COHE-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts
        status: pass
      - kind: unit
        ref: frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts
        status: pass
      - kind: other
        ref: "rg cancelable:true frontend/src — only NoteRefinement.vue + clientSetup.ts"
        status: pass
    human_judgment: false
  - id: D3
    description: Other whole-UI blockers intentionally noncancelable; thin-bar/export nonblocking; no ADPT-01 Cancel migration (D-07, D-08)
    requirement: COHE-02
    verification:
      - kind: other
        ref: .cursor/rules/frontend-api.mdc inventory tables + rg blockUi/runWithBlockingApiLoading crosscheck
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-21
status: complete
---

# Phase 4 Plan 02: Enforce Safe Blocking Boundaries Summary

**Persisted the full COHE-02 blocker classification inventory in `.cursor/rules/frontend-api.mdc` (cancelable allowlist = layout + preview only; create-note and peers intentionally noncancelable; exports/thin-bar nonblocking) without migrating any ADPT-01 site to Cancel.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-07-21T10:12:11Z
- **Completed:** 2026-07-21T10:14:30Z
- **Tasks:** 2/2
- **Files modified:** 1 (frontend-api.mdc)

## Accomplishments

- Extended frontend-api.mdc with cancelable / intentionally noncancelable / nonblocking inventory tables seeded from 04-RESEARCH
- Documented create-note Cancel absence as REFN-05 intentional safety (transactional mutation)
- Marked book-layout AI suggest as ADPT-01 candidate while keeping it noncancelable
- Re-verified layout + preview cancel Vitest suites (9 tests) and `cancelable: true` exclusivity under frontend/src
- Cross-checked `rg 'blockUi: true|runWithBlockingApiLoading'` — no whole-UI site omitted from the inventory

## Task Commits

1. **Task 1: Persist COHE-02 classification inventory in frontend-api.mdc** - `6a1129613b` (docs)
2. **Task 2: Re-verify layout and preview cancelable adopters stay green** - no commit (verification-only; inventory already aligned, suites green)

**Plan metadata:** (see final docs commit)

## Files Created/Modified

- `.cursor/rules/frontend-api.mdc` — Blocking classification inventory (COHE-02)

## Decisions Made

- Exact table layout: Operation | Message | Call site | Mechanism/Notes under three category headings
- No product Cancel migrations; ADPT-01 candidates noted only

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- Task 2 marked `tdd="true"` but is verification of existing Phase 2/3 cancel suites after docs land (no new RED product behavior).
- GREEN: layoutGeneration.cancel + extractionPreview.cancel specs pass (9/9).
- No separate `test(04-02)` / `feat(04-02)` product commits — docs-only Task 1 + verification Task 2.

## Known Stubs

None.

## Threat Flags

None — docs-only surface; no new endpoints, auth paths, or cancelable mutations. T-04-04/T-04-05/T-04-06 mitigated by inventory wording (create noncancelable + safety rationale; book-layout AI ADPT-01 not opted in; AbortError / cancelable runWithBlockingApiLoading forbids retained).

## Self-Check: PASSED

- FOUND: `.planning/phases/04-enforce-safe-blocking-boundaries/04-02-SUMMARY.md`
- FOUND: `.cursor/rules/frontend-api.mdc` inventory section + ADPT-01 candidate
- FOUND: commit `6a1129613b`
