---
phase: 06-overlap-try-again-no-credit
plan: 02
subsystem: recall
tags: [overlap, flyway, quiz_answer, threshold, srs]

requires:
  - phase: 06-overlap-try-again-no-credit
    provides: Dual-match OVERLAP grading with correct=false + outcome=OVERLAP (06-01)
provides:
  - Nullable persisted quiz_answer.outcome (Flyway V300000236)
  - countWrongAnswersSinceForMemoryTracker excludes OVERLAP (D-04 durable)
affects:
  - 06-03 (edges)
  - 06-04 (E2E)
  - re-assimilation threshold gating

tech-stack:
  added: []
  patterns:
    - AnswerOutcome as nullable VARCHAR via @Enumerated(EnumType.STRING)
    - Wrong-count native SQL: correct=false AND (outcome IS NULL OR outcome <> 'OVERLAP')

key-files:
  created:
    - backend/src/main/resources/db/migration/V300000236__add_quiz_answer_outcome.sql
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/Answer.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/RecallPromptRepository.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java

key-decisions:
  - "Auto-selected option-flyway (yolo): persist outcome + exclude OVERLAP; did not soften D-03"
  - "matchedNoteId stays @Transient; only outcome promoted to column"
  - "ERD exporter documents PK/UK/FK only — outcome not shown; no ERD commit"

patterns-established:
  - "Durable grade metadata: nullable quiz_answer.outcome stores AnswerOutcome enum names"
  - "D-04 exclusion: OVERLAP rows are correct=false but omitted from wrong-count"

requirements-completed: []  # OVL-01 spans 06-01..06-04; leave open until phase complete

coverage:
  - id: D1
    description: Five OVERLAP try-agains leave isThresholdExceeded false while correct=false + outcome=OVERLAP
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests$OverlapTryAgain#shouldNotCountOverlapTowardWrongAnswerThreshold
        status: pass
    human_judgment: false
  - id: D2
    description: Five ACCIDENTAL_MATCH answers still exceed wrong-answer threshold (regression)
    requirement: OVL-01
    verification:
      - kind: integration
        ref: RecallPromptControllerTests (accidental-match)#shouldStillCountAccidentalMatchTowardWrongAnswerThreshold
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-07-24
status: complete
---

# Phase 6 Plan 02: Persist outcome / exclude OVERLAP from wrong-count Summary

**Flyway `quiz_answer.outcome` + wrong-count exclusion so repeated OVERLAP try-agains cannot trip re-assimilation while D-03 `correct=false` stays locked.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-07-24T12:16:10Z
- **Completed:** 2026-07-24T12:28:00Z
- **Tasks:** 2/2 (checkpoint auto-selected + TDD implement)
- **Files modified:** 4

## Accomplishments

- Auto-selected **option-flyway** (yolo / `--auto`); did not soften D-03
- Migration `V300000236__add_quiz_answer_outcome.sql` — nullable VARCHAR outcome
- `Answer.outcome` promoted from `@Transient` to `@Column` + `@Enumerated(STRING)`; `matchedNoteId` stays transient
- `countWrongAnswersSinceForMemoryTracker` excludes `outcome = 'OVERLAP'` (NULL = today)
- Controller proof: five OVERLAPs → `isThresholdExceeded` false; AM five-wrong regression still green

## Task Commits

1. **Checkpoint: Confirm durable D-04** — auto-selected `option-flyway` (no commit)
2. **Task (RED): Persist outcome / exclude OVERLAP** - `64b4208cf5` (test)
3. **Task (GREEN): Flyway + query exclusion** - `38555c0e48` (feat)

## Files Created/Modified

- `V300000236__add_quiz_answer_outcome.sql` — nullable `quiz_answer.outcome`
- `Answer.java` — persisted outcome column mapping
- `RecallPromptRepository.java` — wrong-count excludes OVERLAP
- `RecallPromptControllerTests.java` — OverlapTryAgain threshold proof

## Decisions Made

- Yolo default **option-flyway** (persist + exclude); escalate path unused
- Keep locked D-03 (`correct=false` + zero mark path) — no `correct=true` softener
- Skip ERD commit: exporter only lists PK/UK/FK columns; `outcome` is neither

## Deviations from Plan

None - plan executed as written (Flyway path).

### Auto-fixed Issues

None.

## Threat Flags

None beyond plan mitigations T-06-02 / T-06-04 (outcome writers remain service paths).

## Self-Check: PASSED

- Found: migration file, Answer.outcome column, OVERLAP exclusion in repository, SUMMARY path
- Found commits: `64b4208cf5`, `38555c0e48`
- `backend:test_only` green after GREEN commit
