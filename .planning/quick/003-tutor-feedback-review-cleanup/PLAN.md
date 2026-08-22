# Cleanup: tutor descriptive Feedback commit review

**Status:** in progress (slice 1 done)

Source: retrospective review of commit `724685c` ("Tutor descriptive feedback in
Learning Session reports", #1584) — no functional bugs found. Two real quality
issues found (redundant E2E coverage, duplicate repository query). Both are
**Structure** slices: internal-only changes, verified by existing/adjusted
tests, no product behavior change.

## Findings not turned into slices (noted, not actionable now)

- `MemoryTrackerService.markAsRecalled` / `persistRecallLog` now take two
  trailing nullable params (`Answer answer, String tutorFeedback`); several
  call sites pass `null, null` positionally. Mildly less readable but matches
  existing codebase patterns — not worth a change without a second concrete
  pain point.
- `LearningSessionRequestMarkdownBuilder` loads the **full** tutor-log history
  per Session Item (via `findTutorLogsByMemoryTrackerId`) just to report a
  count and the last two dated Feedbacks, replacing a previous lean
  `COUNT`/`MAX` aggregate. No observed performance problem yet — do not add a
  bounding query speculatively (`general.mdc`: no defensive layers without
  observed need). Revisit only if a notebook with many tutoring sessions shows
  up as slow.
- Minor cross-layer test overlap (e.g. `LearningSessionRequestTests` unit
  assertions on `how_to_report` text vs. the equivalent E2E step) — normal
  E2E+unit pairing in this codebase, not a duplicate worth removing.

## Slices

### 1. Trim redundant scheduling assertions from the new session-item-feedback E2E scenario (Structure)

**Status:** done

Trimmed `commissioned_learning_session.feature` so the new-format scenario only
asserts recording + descriptive Feedback text on the tracker. Duplicate recall
count / grade / potential-session steps remain on the legacy
`<session_item_grades>` scenario.

**Verify:** Cypress spec 14/14 green. First run failed because the live backend
still had pre-`724685c` classes; compiling `classes` triggered reload. Re-run
green. Not a product issue.

### 2. Consolidate duplicate "tutor log" query in RecallLogRepository (Structure)

**Problem:**
`backend/src/main/java/com/odde/doughnut/entities/repositories/RecallLogRepository.java`
now defines the "tutor log" predicate (`answer IS NULL AND grade IS NOT
NULL`, ordered by `recordedAt DESC, id DESC`) twice:

- `findTutorLogsByMemoryTrackerId` (added this commit; returns the full
  ordered list)
- `findLatestTutorGradeByMemoryTrackerId` (pre-existing; returns just the
  latest `Grade`, used by `NoteController.getNoteInfo`)

One concept, two JPQL statements — a cohesion smell per `general.mdc` ("one
representation for each concept").

**Change:**

- Remove `findLatestTutorGradeByMemoryTrackerId` from `RecallLogRepository`.
- In `NoteController.getNoteInfo`, derive the latest tutor grade from the
  already-consolidated query:

  ```java
  recallLogRepository.findTutorLogsByMemoryTrackerId(tracker.getId()).stream()
      .findFirst()
      .map(RecallLog::getGrade)
      .ifPresent(tracker::setLatestTutorFeedbackGrade);
  ```

**Verify:** existing `NoteController` tests covering
`latestTutorFeedbackGrade` must pass unchanged (no new test needed — this is
a pure refactor, not a behavior change).
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`

**Stop-safe:** yes — independent of Slice 1; can ship alone.

## Order

Slice 1 done. Next: slice 2 (independent).
