---
phase: 06-overlap-try-again-no-credit
plan: 01
subsystem: recall
tags: [overlap, spelling, srs, daisyui, vitest]

requires:
  - phase: 05-alias-as-wiki-link-overlap-declaration
    provides: FrontmatterAliases.overlapWikiLinkTokensFromNoteContent
  - phase: 01-extend-answer-outcome-api
    provides: AnswerOutcome.OVERLAP and AnsweredQuestion.overlap
provides:
  - Dual-match OVERLAP grading on correct path with zero SRS mutation
  - Warning try-again UI and stay/retry remount for same tracker
affects:
  - 06-02 (durable D-04 threshold exclusion)
  - 06-03 (dead-target / edge cases)
  - 06-04 (E2E)

tech-stack:
  added: []
  patterns:
    - Correct-path OVERLAP early return mirroring AM wrong-path (no markAsRecalled/markAsAccidentalMatch)
    - RecallPage stay-and-retry via spellingRetryNonce remount key

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java
    - frontend/src/components/recall/AnsweredSpellingQuestion.vue
    - frontend/src/pages/RecallPage.vue
    - frontend/src/components/recall/Quiz.vue
    - frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts
    - frontend/tests/pages/RecallPage.spec.ts

key-decisions:
  - "Dual-match OVERLAP only when reviewed matchAnswer and a resolved overlap target also matchAnswer (D-01)"
  - "OVERLAP sets correct=false + outcome=OVERLAP with zero mark path (D-03)"
  - "Strip [[…]] via INNER_LINK_PATTERN before resolveWikiLinkToken"
  - "Frontend keys OVERLAP before !correct error styling; Try again remounts via spellingRetryNonce"

patterns-established:
  - "Overlap third path: persist Answer audit trail then return SpellingAnswerResult(prompt, List.of()) without SRS mutation"
  - "Stay-and-retry: skip moveToNextMemoryTracker + getThresholdExceeded; bump remount nonce on @retry"

requirements-completed: []  # OVL-01 spans 06-01..06-04; leave open until phase complete

coverage:
  - id: D1
    description: Dual-match shared-title spelling grades OVERLAP with correct=false, overlap=true, empty matchedNotes, unchanged SRS fields
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldGradeAsOverlapWhenAnswerMatchesReviewedAndResolvedOverlapTarget
        status: pass
    human_judgment: false
  - id: D2
    description: Distinguishing plain alias on overlap-declaring note earns CORRECT with recallCount bump
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldGradeCorrectWithCreditWhenDistinguishingPlainAlias
        status: pass
    human_judgment: false
  - id: D3
    description: OVERLAP result shows warning try-again alert and Try again emits retry; matched-notes absent
    requirement: OVL-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts#shows warning try-again alert without matched notes and emits retry
        status: pass
    human_judgment: false
  - id: D4
    description: OVERLAP keeps queue index, skips threshold API, Try again remounts spelling via nonce
    requirement: OVL-01
    verification:
      - kind: unit
        ref: frontend/tests/pages/RecallPage.spec.ts#stays on the same tracker, skips threshold, and remounts spelling on Try again
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-24
status: complete
---

# Phase 6 Plan 01: Overlap try-again tracer Summary

**Dual-match OVERLAP grading with zero SRS credit, warning try-again UI, and same-tracker remount retry — distinguishing plain aliases still earn CORRECT.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-24T12:06:46Z
- **Completed:** 2026-07-24T12:14:23Z
- **Tasks:** 2/2
- **Files modified:** 8

## Accomplishments

- Correct-path dual-match OVERLAP in `MemoryTrackerService.answerSpelling` (strip wiki-link brackets → resolve → exclude self → any target matchAnswer)
- `AnsweredQuestion.overlap=true` with empty matchedNotes; zero `markAsRecalled` / `markAsAccidentalMatch`
- Warning alert + Try again CTA; RecallPage stays on tracker and remounts spelling via `spellingRetryNonce`
- Distinguishing plain alias (`color`) still grades CORRECT with credit

## Task Commits

1. **Task 1 (RED): End-to-end overlap try-again proofs** - `36c7c4335d` (test)
2. **Task 1 (GREEN): Overlap grading + stay/retry UI** - `83dad557e0` (feat)
3. **Task 2: Distinguishing plain alias CORRECT** - covered by same RED nested tests + GREEN implementation (no extra commit)

## Files Created/Modified

- `MemoryTrackerService.java` — OVERLAP dual-match branch + `isNonDistinguishingOverlap`
- `AnsweredQuestion.java` — set `overlap=true` when outcome is OVERLAP
- `AnsweredSpellingQuestion.vue` — warning alert, Try again `@retry`, OVERLAP before error styling
- `RecallPage.vue` — OVERLAP stay path + `onOverlapRetry` nonce bump
- `Quiz.vue` — spelling key includes `spellingRetryNonce`
- Controller + Vitest proofs for dual-match, distinguishing alias, alert, stay/retry

## Decisions Made

- Qualified notebook wiki-link fixture for live partner dual-match (same title across notebooks)
- Explicit Try again button + remount nonce (not resume-only)
- D-04 durable threshold DB exclusion deferred to plan 06-02; frontend skips `getThresholdExceeded` here

## Deviations from Plan

None - plan executed as written. Task 2 distinguishing-alias proof shipped inside the Task 1 OverlapTryAgain nested class rather than a second commit.

## Threat Flags

None — reused `resolveWikiLinkToken` readability filter; matchedNotes left empty for OVERLAP (T-06-01).

## Self-Check: PASSED

- Found: SUMMARY path, commits `36c7c4335d` / `83dad557e0`, key source files present
- Tracer verify re-run green after GREEN commit (auto-chain)
