---
phase: 08-resolve-pull-export-story-1
plan: 02
subsystem: testing
tags: [export, cli, e2e, doughnut_id, wiki-links, attachments, cypress]

requires:
  - phase: 08-resolve-pull-export-story-1
    provides: Backend zip with doughnut_id, wiki→relative MD, absolute attachment URLs (08-01)
provides:
  - Green cli_export.feature proofs for D-01, D-04, D-05 on disk via /export
  - Phase 8 Wave 0 / nyquist validation complete
affects:
  - Phases 9–10 (Stories 2–3 consume same zip identity/link contract)
  - EXP-01 requirement closure

actuals:
  tokens: 1200
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Capability E2E proves zip shape via /export + destinationFileShouldHold substrings (D-06: no CLI rewrite)"
    - "Attachment seed via moon.jpg upload step; wiki seed via notes Content [[title]]"

key-files:
  created: []
  modified:
    - e2e_test/features/cli/cli_export.feature
    - .planning/phases/08-resolve-pull-export-story-1/08-VALIDATION.md
    - .planning/STATE.md
    - .planning/ROADMAP.md
    - .planning/REQUIREMENTS.md

key-decisions:
  - "No new step glue — reused destinationFileShouldHold substring asserts"
  - "Standard granularity: one green E2E commit (no separate RED authoring commit)"
  - "Attachment absolute URL asserted as http + /attachments/images/ (origin is LB→backend Host)"

patterns-established:
  - "Story 1 gaps proven only through cli_export.feature consuming backend zip bytes"

requirements-completed: [EXP-01]

coverage:
  - id: D1
    description: Exported note file holds doughnut_id frontmatter on disk after CLI /export
    requirement: EXP-01
    verification:
      - kind: e2e
        ref: e2e_test/features/cli/cli_export.feature#Export includes doughnut_id frontmatter on each note
        status: pass
    human_judgment: false
  - id: D2
    description: Resolvable wiki link exports as ordinary Markdown link containing ]( and target.md
    requirement: EXP-01
    verification:
      - kind: e2e
        ref: e2e_test/features/cli/cli_export.feature#Export rewrites resolvable wiki links to ordinary Markdown links
        status: pass
    human_judgment: false
  - id: D3
    description: Attachment image ref exports absolute URL with http prefix and /attachments/images/
    requirement: EXP-01
    verification:
      - kind: e2e
        ref: e2e_test/features/cli/cli_export.feature#Export rewrites attachment refs to absolute remote URLs
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-03
status: complete
---

# Phase 08 Plan 02: Resolve pull/export story 1 — CLI `/export` E2E proofs Summary

**CLI `/export` now leaves `doughnut_id`, ordinary Markdown wiki targets, and absolute `/attachments/images/` URLs on disk — proven by three green `cli_export.feature` scenarios with no CLI-side rewrite.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-03T06:41:08Z
- **Completed:** 2026-08-03T06:45:00Z
- **Tasks:** 2/2
- **Files modified:** 1 product (feature) + planning wrap-up

## Accomplishments

- Added three focused E2E scenarios under existing Feature tags/Background
- All eight `cli_export.feature` scenarios green (5 existing + 3 new); no `@wip`/`@focus`/`@only`
- Phase wrap-up: Jidoka (EXP-01 gaps closed, HYG-02/D-07 allowlist clean), empty post-change-refactor scope, VALIDATION Wave 0 + nyquist signed off

## Task Commits

1. **Task 1: Prove Story 1 gaps on disk via cli_export E2E** - `688bfcdbf6` (test)
2. **Task 2: Phase wrap-up** - (docs commit with SUMMARY/STATE/ROADMAP/VALIDATION)

## Files Created/Modified

- `e2e_test/features/cli/cli_export.feature` — three Story 1 gap scenarios (D-01, D-04, D-05)
- `.planning/phases/08-resolve-pull-export-story-1/08-VALIDATION.md` — Wave 0 done, nyquist_compliant true
- `.planning/STATE.md` / `ROADMAP.md` / `REQUIREMENTS.md` — Phase 8 / EXP-01 complete

## Decisions Made

- Reused existing `should hold` substring steps; no `cli_export.ts` / `exportDestination.ts` changes
- Seeded wiki via Content `[[target]]`; seeded attachment via `moon.jpg` upload (frontmatter `image:` path rewritten by backend)
- Asserted attachment absolute URL with separate `http` and `/attachments/images/` substrings (Host is LB-proxied backend origin)

## Deviations from Plan

None - plan executed exactly as written.

**Post-change-refactor:** empty uncommitted product scope after Task 1 commit — `## REFACTOR COMPLETE` (no edits).

## Auth Gates

None.

## Known Stubs

None.

## Threat Flags

None — E2E seeds use fixtures only; inventory scenarios unchanged (no credential paths).

## Self-Check: PASSED

- FOUND: `e2e_test/features/cli/cli_export.feature` with doughnut_id / wiki / attachment scenarios
- FOUND: commit `688bfcdbf6`
- FOUND: `cli_export.feature` Cypress 8/8 green
- FOUND: `pnpm backend:test_only` green
- FOUND: diff excludes `cli/src/sync/`, `applyPull.ts`, `Frontmatter.java`
- FOUND: `08-02-SUMMARY.md`, Wave 0 / nyquist in `08-VALIDATION.md`
