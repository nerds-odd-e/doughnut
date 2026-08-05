---
phase: 08-match-path-and-clickable-titles
plan: 01
subsystem: ui
tags: [vue, vitest, recall, accidental-match, breadcrumb, note-title]

requires:
  - phase: 07-compact-result-resolve-dialog-shell
    provides: AccidentalMatchResolveDialog list host + Resolve CTA / Modal shell
provides:
  - AccidentalMatchResolveRow with client realm hydrate + NoteTitleWithLink + conditional BreadcrumbWithCircle
  - Dialog list host wired to rows (titles-only interim replaced)
  - Vitest boundary coverage for clickable title + distinct notebook path identity
affects:
  - 08-02 (E2E path + clickable title asserts)
  - Phase 9 Build a link (per-row actions on AccidentalMatchResolveRow)

actuals:
  tokens: 1467
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - Per-row getNoteRealmRefAndLoadWhenNeeded hydrate on dialog mount
    - Title immediate from NoteTopology; path omitted until realm present

key-files:
  created:
    - frontend/src/components/recall/AccidentalMatchResolveRow.vue
  modified:
    - frontend/src/components/recall/AccidentalMatchResolveDialog.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts
    - frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts
    - frontend/components.d.ts

key-decisions:
  - "Assert router-link `to` (not href) under RenderingHelper stub — still proves noteShow navigation intent"
  - "Distinct notebook names via accidentalMatchWithTwoMatchedNotes({ notebookNames }) mutate-after-please"
  - "Task 2 fixture expansion folded into helper + canonical path-delta assert (no separate product commit)"

patterns-established:
  - "AccidentalMatchResolveRow owns hydrate + identity chrome; dialog stays thin list host"
  - "seedRealms before mount beats generic showNote mock for path determinism"

requirements-completed: [AMR-04]

coverage:
  - id: D1
    description: Resolve dialog match rows show clickable NoteTitleWithLink toward noteShow and BreadcrumbWithCircle path when realm seeded
    requirement: AMR-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#opens resolve dialog with clickable titles and notebook path identity
        status: pass
    human_judgment: false
  - id: D2
    description: Distinct notebook path text across two match rows via reusable notebookNames seed helper
    requirement: AMR-04
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#opens resolve dialog with clickable titles and notebook path identity
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-05
status: complete
---

# Phase 08 Plan 01: Match path and clickable titles Summary

**Vitest-proven AccidentalMatchResolveRow: clickable NoteTitleWithLink + progressive BreadcrumbWithCircle from client hydrate — no OpenAPI widen, no NoteShow peek.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-05T09:33:51Z
- **Completed:** 2026-08-05T09:38:10Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- Extracted `AccidentalMatchResolveRow` that hydrates via `getNoteRealmRefAndLoadWhenNeeded`, always renders `NoteTitleWithLink`, and mounts `BreadcrumbWithCircle` only when the realm is present
- Replaced titles-only dialog list with row components; `matchedNotes` remains `NoteTopology[]`
- Extended AccidentalMatch Vitest with seeded distinct notebook names and focused title-link + path-delta asserts

## Task Commits

1. **Task 1 (RED): Tracer failing test** — `d5807d722f` (test)
2. **Task 1 (GREEN) + Task 2 fixtures:** AccidentalMatchResolveRow + dialog wire + path-delta asserts — `9cb07d1a7c` (feat)

_Task 2 had no separate product commit: `notebookNames` helper landed in RED; multi-row path delta assert landed with GREEN._

## Files Created/Modified

- `frontend/src/components/recall/AccidentalMatchResolveRow.vue` — hydrate + title link + conditional path
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — thin `v-for` → row
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — AMR-04 boundary coverage
- `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` — `notebookNames` option for seedRealms
- `frontend/components.d.ts` — auto-register new row component

## Decisions Made

- Keep asserting navigation via stubbed `router-link` `to` JSON (href is always `#` in RenderingHelper) — same intent as plan's `/10/` href check
- Prefer mutate-after-`please()` notebook names on the existing factory over extending `NoteRealmBuilder`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Title-link assert used href under stubbed router-link**
- **Found during:** Task 1 (GREEN)
- **Issue:** RenderingHelper stubs `router-link` with `href="#"` and puts the route on `to`; `.toMatch(/10/)` on href failed with `"#"`
- **Fix:** Assert `a.router-link[to]` contains the match id; document stub behavior in the test
- **Files modified:** `AnsweredSpellingQuestionAccidentalMatch.spec.ts`
- **Commit:** `9cb07d1a7c`

## TDD Gate Compliance

- RED: `d5807d722f` test(08-01)
- GREEN: `9cb07d1a7c` feat(08-01)
- REFACTOR: none (post-change-refactor found no concept-bound edits)

## Known Stubs

None.

## Threat Flags

None — reused existing authenticated hydrate and title/breadcrumb chrome; no new endpoints or `v-html`.

## Self-Check: PASSED

- FOUND: `frontend/src/components/recall/AccidentalMatchResolveRow.vue`
- FOUND: `frontend/src/components/recall/AccidentalMatchResolveDialog.vue`
- FOUND: `d5807d722f`
- FOUND: `9cb07d1a7c`
