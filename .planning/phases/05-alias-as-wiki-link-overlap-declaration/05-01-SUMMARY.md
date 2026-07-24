---
phase: 05-alias-as-wiki-link-overlap-declaration
plan: 01
subsystem: api
tags: [aliases, wiki-link, overlap, frontmatter, validation]

requires:
  - phase: 01-extend-answer-outcome-api
    provides: AnswerOutcome.OVERLAP and AnsweredQuestion.overlap on contract (unused until Phase 6)
provides:
  - FrontmatterAliases plain-only from* + overlapWikiLinkTokensFrom* segregation
  - Authored accept for well-formed wiki-link overlap alias items (backend + frontend)
  - HTTP content PATCH accept path for mixed plain+wiki-link aliases
affects:
  - 05-02 / 05-03 OVL-03 consumer regressions
  - Phase 6 OVL-01 overlap grading (consumes overlapWikiLinkTokensFrom*)

tech-stack:
  added: []
  patterns:
    - "Classify aliases at parse time: plain vs whole-item wiki-link"
    - "Authored validation = plain ∪ wiki-link; soft from* = plain-only"
    - "Frontend authoredAliasesValidation lockstep with backend message + rules"

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java
    - backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java
    - backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java
    - frontend/src/utils/authoredAliasesValidation.ts
    - frontend/tests/utils/authoredAliasesValidation.spec.ts
    - frontend/tests/utils/noteContentPropertyRows.spec.ts

key-decisions:
  - "Keep fromNoteContent/fromFrontmatter/matchesFromNoteContent plain-only (D-02 least churn)"
  - "Detect wiki-link items with INNER_LINK_PATTERN.matcher(trimmed).matches() + non-empty splitInner target"
  - "overlapWikiLinkTokensFrom* return raw [[…]] tokens; dedupe by normalizedLookupKey of full token"
  - "AUTHORED_ALIASES_MESSAGE updated once for plain + wiki-link overlap declarations (backend/frontend lockstep)"

patterns-established:
  - "Pattern: isWikiLinkAliasItem / isValidPlainAliasText / isAcceptableAuthoredAliasItem split in FrontmatterAliases"
  - "Pattern: frontend WHOLE_WIKI_LINK_ALIAS ^[[…]]$ mirrors Matcher.matches()"

requirements-completed: [OVL-02]

coverage:
  - id: D1
    description: Mixed plain+wiki-link aliases list segregates plain from overlap tokens
    requirement: OVL-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java#fromFrontmatter_returns_only_plain_aliases_when_wiki_link_overlap_declared
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java#overlapWikiLinkTokensFromFrontmatter_returns_wiki_link_tokens_in_order
        status: pass
    human_judgment: false
  - id: D2
    description: Authors can save well-formed wiki-link alias items via content PATCH; malformed rejected
    requirement: OVL-02
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java#accepts_well_formed_wiki_link_overlap_alias_list
        status: pass
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java#rejects_malformed_wiki_link_alias_list_item
        status: pass
    human_judgment: false
  - id: D3
    description: Frontend property-editor preflight accepts the same wiki-link overlap forms as the backend
    requirement: OVL-02
    verification:
      - kind: unit
        ref: frontend/tests/utils/authoredAliasesValidation.spec.ts#accepts well-formed wiki-link overlap alias items
        status: pass
      - kind: unit
        ref: frontend/tests/utils/noteContentPropertyRows.spec.ts#accepts wiki-link overlap alias list rows
        status: pass
    human_judgment: false
  - id: D4
    description: Plain-only accessors remain the choke point for index/match/cloze consumers (declaration seam only)
    requirement: OVL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java#fromFrontmatter_returns_only_plain_aliases_when_wiki_link_overlap_declared
        status: pass
      - kind: other
        ref: "rg MemoryTrackerService — no overlapWikiLinkTokensFrom / OVERLAP wiring"
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-07-24
status: complete
---

# Phase 5 Plan 01: Alias-as-wiki-link overlap declaration tracer Summary

**FrontmatterAliases classifies plain vs wiki-link alias items, exposes overlapWikiLinkTokensFrom*, and ships lockstep frontend authored validation so overlap declarations are authorable without touching grading.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-07-24T05:52:48Z
- **Completed:** 2026-07-24T05:58:00Z
- **Tasks:** 2/2
- **Files modified:** 6

## Accomplishments
- Authored validation accepts D-01 wiki-link forms (`[[Title]]`, qualified, pipe-display) alongside plain aliases; rejects bare `[[`, embedded, and empty-target tokens.
- Soft `fromNoteContent` / `fromFrontmatter` stay plain-only; additive `overlapWikiLinkTokensFromNoteContent` / `FromFrontmatter` return raw tokens for Phase 6.
- HTTP content update accepts mixed lists; frontend `authoredAliasesValidation` message and rules match backend.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end wiki-link alias declaration — parse → authored save → overlap API** - `54e6c9df84` (feat)
2. **Task 2: Frontend authoredAliasesValidation parity for wiki-link overlap items** - `0cd36c4885` (feat)

**Plan metadata:** (pending docs commit)

## Files Created/Modified
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — classification + overlap API + updated message
- `backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java` — capability-named unit proofs
- `backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java` — HTTP accept/reject
- `frontend/src/utils/authoredAliasesValidation.ts` — client parity
- `frontend/tests/utils/authoredAliasesValidation.spec.ts` — accept/reject Vitest
- `frontend/tests/utils/noteContentPropertyRows.spec.ts` — property-row accept case

## Decisions Made
- Kept existing plain-only method names (D-02 / least call-site churn).
- Whole-string `INNER_LINK_PATTERN.matches()` detection (not `find()`).
- Overlap API returns raw token strings; dedupe via full-token `normalizedLookupKey`.
- No MemoryTrackerService / Flyway / OpenAPI changes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] TypeScript strict undefined on wiki-link capture group**
- **Found during:** Task 2 (pre-commit `vue-tsc`)
- **Issue:** `match[1].trim()` failed TS2532 (Object is possibly 'undefined').
- **Fix:** Guard with `match?.[1]` before trim.
- **Files modified:** `frontend/src/utils/authoredAliasesValidation.ts`
- **Verification:** `vue-tsc` + targeted Vitest green
- **Committed in:** `0cd36c4885`

**2. [Rule 2 - Correctness] Deferred OVL-03 requirement checkbox until consumer regressions**
- **Found during:** State/requirements update after Task 2
- **Issue:** Plan frontmatter listed OVL-03, but Wave 2 plans (05-02/05-03) still own index/search/resolve/cloze/AM regression proofs.
- **Fix:** Marked OVL-02 complete; left OVL-03 in progress in REQUIREMENTS.md.
- **Files modified:** `.planning/REQUIREMENTS.md`, `05-01-SUMMARY.md`
- **Verification:** Traceability row notes 05-02/05-03 remaining
- **Committed in:** docs metadata commit

## Threat Flags

None — no new endpoints, auth paths, or schema; authored YAML trust boundary mitigated per T-05-01/T-05-02 via whole-item match + plain-only soft parse.

## Known Stubs

None.

## Self-Check: PASSED

- SUMMARY.md present
- Commits `54e6c9df84`, `0cd36c4885` present
- `FrontmatterAliases.overlapWikiLinkTokensFromNoteContent` / `FromFrontmatter` present
- `authoredAliasesValidation.ts` present
