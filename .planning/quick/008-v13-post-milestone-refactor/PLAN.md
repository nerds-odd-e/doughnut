# v1.3 post-milestone refactor

**Status:** shipped (2026-08-08)

**Type:** ad-hoc quick plan (Structure only)

**Goal:** Post-change-refactor and unit-testing.mdc hygiene for milestone
**v1.3 Commissioned Learning Session MVP** — no observable behavior change.

## Outcome

All 11 Structure phases executed. CLS E2E (`commissioned_learning_session.feature`)
green throughout.

**Highlights:**
- Dead code removed from record path; `LearningSessionService` 192 lines
- Controller/recall tests split by capability (all under 250 lines)
- `LearningSessionLite` unified DTO; frontend aligned
- `LearningSessionRecordTargetResolver`, `CommissionedLearningSessionFeedbackScheduling`
- `LearningSessionStrip` + single dialog in `RecallProgressBar`
- Distinct strip vs dialog `data-test` ids; trimmed overlapping unit tests

**Deferred (optional polish):** `authorizedNotebook` helper; domain exceptions
instead of `ResponseStatusException`; shared Spanish-notebook fixtures across
test bases; unused `timezone` param on record endpoint.
