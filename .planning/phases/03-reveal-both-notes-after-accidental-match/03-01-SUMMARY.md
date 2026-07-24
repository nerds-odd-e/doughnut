---
phase: 03-reveal-both-notes-after-accidental-match
plan: 01
subsystem: api
tags: [accidental-match, matchedNotes, WikiLinkResolver, spelling, IDOR]

requires:
  - phase: 02-accidental-match-grading-penalty
    provides: ACCIDENTAL_MATCH outcome, matchedNoteId, findAccidentalMatch, empty matchedNotes field
provides:
  - findAllAccidentalMatches title∪alias readable union ordered by id
  - AnsweredQuestion.matchedNotes populated on answer-spelling ACCIDENTAL_MATCH
  - matchedNoteId aligned to first matchedNotes entry
  - IDOR proofs that unreadable notes never appear in matchedNotes
affects:
  - 03-02 (UI reveal of matchedNotes)
  - 03-03 (E2E / messaging)
  - Phase 4 link preselection via matchedNoteId

tech-stack:
  added: []
  patterns:
    - findAllAccidentalMatches as source of truth; findAccidentalMatch = first-of-list
    - SpellingAnswerResult carries RecallPrompt + match list for single-lookup DTO assembly

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java
    - backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java
    - backend/src/main/java/com/odde/doughnut/controllers/RecallPromptController.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java

key-decisions:
  - "D-01/D-02: matchedNotes is primary plural surface; matchedNoteId is first-of-list"
  - "SpellingAnswerResult avoids double findAll between service and controller"
  - "Title∪alias union replaces Phase 2 title-prefer short-circuit"

patterns-established:
  - "Pattern: TreeMap id-ordered merge of title + alias candidates with userMayReadNotebook filter"
  - "Pattern: AnsweredQuestion.from(prompt, matches) maps Note::getNoteTopology when non-empty"

requirements-completed: []  # AM-03 spans Phase 3 UI (03-02/03-03); backend list wire alone does not complete reveal

coverage:
  - id: D1
    description: "answer-spelling returns populated matchedNotes on ACCIDENTAL_MATCH with matchedNoteId = first list id"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "RecallPromptControllerTests$AccidentalMatch#shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteTitle"
        status: pass
      - kind: unit
        ref: "CURSOR_DEV=true nix develop -c pnpm backend:test_only"
        status: pass
    human_judgment: false
  - id: D2
    description: "Title∪alias union returns both notes in matchedNotes ordered by id"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "RecallPromptControllerTests$AccidentalMatch#shouldIncludeTitleAndAliasMatchesInMatchedNotesOrderedById"
        status: pass
    human_judgment: false
  - id: D3
    description: "Unreadable notes never appear in matchedNotes (IDOR / T-03-01)"
    requirement: AM-03
    verification:
      - kind: unit
        ref: "RecallPromptControllerTests$AccidentalMatch#shouldOmitUnreadableNotesFromMatchedNotesWhenReadableMatchAlsoExists"
        status: pass
      - kind: unit
        ref: "RecallPromptControllerTests$AccidentalMatch#shouldNotLeakMatchedNoteIdFromUnreadableNotebook"
        status: pass
    human_judgment: false

duration: 18min
completed: 2026-07-24
status: complete
---

# Phase 03 Plan 01: Populate matchedNotes Summary

**answer-spelling now returns all readable accidental matches as `matchedNotes`, with `matchedNoteId` as the lowest-id first entry.**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-07-24T04:13:32Z
- **Completed:** 2026-07-24T04:31:00Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- Added `WikiLinkResolver.findAllAccidentalMatches` (title ∪ alias, readability filter, dedupe, id ASC); `findAccidentalMatch` delegates to first-of-list.
- Wired `MemoryTrackerService.SpellingAnswerResult` + `AnsweredQuestion.from(prompt, matches)` so the controller populates `matchedNotes` without a second lookup.
- Strengthened AccidentalMatch controller tests for union semantics and IDOR list filtering.

## Task Commits

1. **Task 1 (tracer) RED:** `51b36bba64` — test(03-01): assert matchedNotes populated on accidental match
2. **Task 1 (tracer) GREEN:** `a28cd72460` — feat(03-01): populate matchedNotes via findAllAccidentalMatches
3. **Task 2 IDOR:** `9fc150ffc4` — test(03-01): prove matchedNotes omits unreadable notes

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `WikiLinkResolver.java` — `findAllAccidentalMatches` + `findAccidentalMatch` first-of-list
- `MemoryTrackerService.java` — `SpellingAnswerResult`; grades from `findAllAccidentalMatches` once
- `AnsweredQuestion.java` — `from(RecallPrompt, List<Note>)` sets topologies
- `RecallPromptController.java` — returns `from(prompt, matches)`
- `RecallPromptControllerTests.java` — AccidentalMatch assertions + IDOR mixed-readability case

## Decisions Made

- Followed locked D-01/D-02: plural `matchedNotes` is primary; `matchedNoteId` = first list entry.
- Used package-visible `SpellingAnswerResult` so controller does not re-query matches.
- Rewrote former title-prefer test to title∪alias union ordered by id.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Correctness] Did not leave AM-03 marked complete after backend-only plan**
- **Found during:** State/requirements wrap-up
- **Issue:** `requirements.mark-complete AM-03` ran from plan frontmatter, but AM-03 ("revealed together") still needs 03-02 UI / 03-03 E2E.
- **Fix:** Reverted AM-03 checkbox and traceability to In Progress; left `requirements-completed` empty in SUMMARY.
- **Files modified:** `.planning/REQUIREMENTS.md`, `03-01-SUMMARY.md`
- **Commit:** docs metadata commit


## TDD Gate Compliance

- RED commit present (`test(03-01): …`)
- GREEN commit present after RED (`feat(03-01): …`)
- Task 2 was test-only strengthening (production filter already correct from Task 1)

## Threat Flags

None — IDOR mitigated via existing `userMayReadNotebook` filter; Task 2 proves list surface.

## Known Stubs

None.

## Self-Check: PASSED

- `WikiLinkResolver.java` FOUND with `findAllAccidentalMatches`
- Commits `51b36bba64`, `a28cd72460`, `9fc150ffc4` FOUND
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only` exited 0
