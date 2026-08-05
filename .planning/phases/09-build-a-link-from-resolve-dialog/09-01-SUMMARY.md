---
phase: 09-build-a-link-from-resolve-dialog
plan: 01
subsystem: ui
tags: [vue, recall, accidental-match, MatchedNoteLinkOffer, Vitest]

requires:
  - phase: 08-match-path-and-clickable-titles
    provides: AccidentalMatchResolveDialog list + AccidentalMatchResolveRow path/title hydrate
  - phase: 07-compact-result-resolve-dialog-shell
    provides: Resolve PopButton Modal shell; MatchedNoteLinkOffer unused until Phase 9
provides:
  - Single-Modal list|link step host mounting MatchedNoteLinkOffer
  - Gated Build a link CTA with link-to-matched-note-* testids
  - Vitest evidence for AMR-06 stay-on-result and AMR-07 readonly/unload gates
affects:
  - 09-02 E2E untag of Build a link scenarios
  - Phase 11 Add as overlapped note (same gate rules)

actuals:
  tokens: 4188
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Single-Modal ResolveStep list|link swap (never nest PopButton around offer)"
    - "canOfferBuildLink host gate: currentUser + reviewedRealm + readonly !== true + matched realm"
    - "MatchedNoteLinkOffer @close-dialog → returnToList (not Modal closer)"

key-files:
  created: []
  modified:
    - frontend/src/components/recall/AccidentalMatchResolveDialog.vue
    - frontend/src/components/recall/AccidentalMatchResolveRow.vue
    - frontend/src/components/recall/AnsweredSpellingQuestion.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts
    - frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts

key-decisions:
  - "Step state lives in AccidentalMatchResolveDialog; AnsweredSpellingQuestion only passes reviewedNoteId"
  - "Offer closeDialog maps to returnToList — outer Modal dismiss unchanged"
  - "Build a link omitted via v-if canBuildLink when readonly or realms unloaded"

patterns-established:
  - "Pattern: resolve dialog step host for mutate offers without nested Modals"
  - "Pattern: openResolveAccidentalMatch test helper for document.body Modal queries"

requirements-completed: [AMR-06, AMR-07]

coverage:
  - id: D1
    description: "Writable seeded realms show two Build a link CTAs; click opens Link to: in same Modal; property success returns to list with alert still present"
    requirement: AMR-06
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#builds a link as a same-Modal step and returns to the match list after success"
        status: pass
    human_judgment: false
  - id: D2
    description: "Build a link omitted when reviewed notebook readonly or realms unloaded"
    requirement: AMR-07
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#omits Build a link when reviewed notebook is readonly"
        status: pass
      - kind: unit
        ref: "frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts#omits Build a link when note realms are not loaded"
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-05
status: complete
---

# Phase 9 Plan 01: Build a link Vitest Summary

**Resolve dialog hosts MatchedNoteLinkOffer as a same-Modal step with gated Build a link CTAs; Vitest proves stay-on-result and readonly/unload omit.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-05T12:03:36Z
- **Completed:** 2026-08-05T12:07:17Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- AccidentalMatchResolveDialog owns `list|link` step state and mounts MatchedNoteLinkOffer with `@close-dialog=returnToList`
- Per-row gated **Build a link** (`link-to-matched-note-{id}`) via `canOfferBuildLink`
- Vitest at AnsweredSpellingQuestion boundary: writable step+stay, readonly omit, unloaded-realm omit

## Task Commits

1. **Task 1 RED: Tracer failing Vitest** - `0ad4e1138a` (test)
2. **Task 1 GREEN: Same-Modal Build a link step** - `c9726497b6` (feat)
3. **Task 2: AMR-07 gate Vitest + open-resolve helper** - `2a9e293373` (test)

**Plan metadata:** included in `2a9e293373` (SUMMARY + STATE + ROADMAP + REQUIREMENTS)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Biome empty Promise executor**
- **Found during:** Task 2 commit hook
- **Issue:** `new Promise(() => {})` flagged `noEmptyBlockStatements`
- **Fix:** Intentional never-settle comment inside executor
- **Files modified:** AnsweredSpellingQuestionAccidentalMatch.spec.ts

**2. [Rule 3 - Blocking] Spec over 250-line limit after gate cases**
- **Found during:** post-change-refactor
- **Issue:** Spec reached 251 lines
- **Fix:** Extracted `openResolveAccidentalMatch` into answeredSpellingQuestionTestSupport; reused across dialog opens
- **Files modified:** answeredSpellingQuestionTestSupport.ts, AnsweredSpellingQuestionAccidentalMatch.spec.ts

## TDD Gate Compliance

- RED commit `0ad4e1138a` present
- GREEN commit `c9726497b6` present after RED
- Task 2 gates were already implemented in tracer GREEN (D-06/D-07 in Task 1 action); Task 2 added verification coverage only

## Self-Check: PASSED

- AccidentalMatchResolveDialog.vue FOUND
- AccidentalMatchResolveRow Build a link FOUND
- reviewed-note-id wiring FOUND
- Commits 0ad4e1138a, c9726497b6 FOUND
- Vitest 7/7 green
