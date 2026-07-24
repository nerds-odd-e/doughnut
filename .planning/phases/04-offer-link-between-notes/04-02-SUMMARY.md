---
phase: 04-offer-link-between-notes
plan: 02
subsystem: ui
tags: [vue, recall, relationship, navigation, vitest]

requires:
  - phase: 04-offer-link-between-notes (04-01)
    provides: MatchedNoteLinkOffer property path; LinkInsertionChoice relationship button (then gated false)
provides:
  - skipNavigation on createRootNoteAtNotebook
  - navigateOnSuccess on AddRelationshipFinalize (default true)
  - MatchedNoteLinkOffer relationship finalize stage with D-07 stay-on-page
affects:
  - 04-03 (E2E / remaining AM-04 surface)
  - toolbar SearchForm (unchanged defaults)

tech-stack:
  added: []
  patterns:
    - "Optional skipNavigation on createRootNoteAtNotebook; recall passes via navigateOnSuccess=false"
    - "SearchForm-like two-ref stage machine in MatchedNoteLinkOffer (selected vs target)"

key-files:
  created: []
  modified:
    - frontend/src/store/StoredApiCollection.ts
    - frontend/src/components/links/AddRelationshipFinalize.vue
    - frontend/src/components/recall/MatchedNoteLinkOffer.vue
    - frontend/tests/links/AddRelationship.spec.ts
    - frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts

key-decisions:
  - "D-07: skipNavigation + navigateOnSuccess=false keeps recall on accidental-match result after relationship create"
  - "Toolbar/SearchForm omit navigateOnSuccess so default navigate-after-create is preserved"

patterns-established:
  - "Client-only stay-on-page flag; does not change createNoteAtNotebookRoot auth"

requirements-completed: [AM-04]

coverage:
  - id: D1
    description: "createRootNoteAtNotebook can skip client navigation while still refreshing storage/caches"
    requirement: AM-04
    verification:
      - kind: unit
        ref: "frontend/tests/links/AddRelationship.spec.ts#creates relationship note and emits success without navigating when navigateOnSuccess is false"
        status: pass
    human_judgment: false
  - id: D2
    description: "Default AddRelationshipFinalize still navigates after create"
    requirement: AM-04
    verification:
      - kind: unit
        ref: "frontend/tests/links/AddRelationship.spec.ts#creates relationship note, navigates, and emits success"
        status: pass
    human_judgment: false
  - id: D3
    description: "Recall offer relationship path creates after confirm, closes dialog, no router navigation (D-07)"
    requirement: AM-04
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts#creates relationship note, closes dialog, and does not navigate"
        status: pass
    human_judgment: false
  - id: D4
    description: "Relationship create API not called until user confirms relation type"
    requirement: AM-04
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts#does not create a relationship note until the user confirms relation type"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-24
status: complete
---

# Phase 04 Plan 02: Offer relationship link Summary

**Recall accidental-match offer now completes AM-04’s relationship path with stay-on-page (D-07), while toolbar create still navigates by default.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-07-24T05:05:41Z
- **Completed:** 2026-07-24T05:12:00Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- Added `skipNavigation` to `createRootNoteAtNotebook` (interface + implementation).
- Added `navigateOnSuccess` (default `true`) on `AddRelationshipFinalize`.
- Wired `MatchedNoteLinkOffer` second stage to `AddRelationshipFinalize` with `:navigate-on-success="false"`; relationship option visible again; success closes dialog without router navigation.

## Task Commits

1. **Task 1 RED:** `a808c70210` — test(04-02): add failing test for navigateOnSuccess false
2. **Task 1 GREEN:** `0fe206e2dc` — feat(04-02): skipNavigation and navigateOnSuccess for stay-on-page create
3. **Task 2 RED:** `7c1404f340` — test(04-02): add failing relationship path tests for MatchedNoteLinkOffer
4. **Task 2 GREEN:** `e910d6a6d3` — feat(04-02): wire MatchedNoteLinkOffer relationship finalize with D-07

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `frontend/src/store/StoredApiCollection.ts` — `skipNavigation?: boolean` on create options
- `frontend/src/components/links/AddRelationshipFinalize.vue` — `navigateOnSuccess` prop → `skipNavigation`
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — relationship stage + D-07
- `frontend/tests/links/AddRelationship.spec.ts` — default navigate + skip-nav assertions
- `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` — relationship + never-auto-write + D-07

## Decisions Made

- Followed plan D-07 resolution: additive `skipNavigation` / `navigateOnSuccess` rather than forking create logic.
- Omitted `relationshipOptionAvailable` binding so LinkInsertionChoice default (`true`) applies.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Interface signature missing skipNavigation**
- **Found during:** Task 1 GREEN commit (vue-tsc pre-commit)
- **Issue:** `StoredApi` interface still typed options without `skipNavigation`, so Vue SFC failed typecheck.
- **Fix:** Added `skipNavigation?: boolean` to the interface declaration as well as the implementation.
- **Files modified:** `frontend/src/store/StoredApiCollection.ts`
- **Verification:** `vue-tsc --noEmit` + AddRelationship.spec.ts green
- **Committed in:** `0fe206e2dc`

## Verification

```
CURSOR_DEV=true nix develop -c pnpm -C frontend test \
  tests/components/recall/MatchedNoteLinkOffer.spec.ts \
  tests/links/AddRelationship.spec.ts
```

Result: 10/10 passed.

## Self-Check: PASSED

- FOUND: `frontend/src/store/StoredApiCollection.ts` (skipNavigation)
- FOUND: `frontend/src/components/links/AddRelationshipFinalize.vue` (navigateOnSuccess)
- FOUND: `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (AddRelationshipFinalize)
- FOUND commits: a808c70210, 0fe206e2dc, 7c1404f340, e910d6a6d3
