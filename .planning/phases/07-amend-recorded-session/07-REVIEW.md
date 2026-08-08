---
phase: 07-amend-recorded-session
reviewed: 2026-08-08T01:56:00Z
depth: standard
files_reviewed: 27
files_reviewed_list:
  - backend/src/main/java/com/odde/doughnut/controllers/dto/DueMemoryTrackers.java
  - backend/src/main/java/com/odde/doughnut/controllers/dto/RecordedLearningSessionLite.java
  - backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  - backend/src/main/java/com/odde/doughnut/entities/SessionItem.java
  - backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java
  - backend/src/main/java/com/odde/doughnut/services/RecallService.java
  - backend/src/main/resources/db/migration/V300000241__session_item_pre_session_snapshot.sql
  - backend/src/test/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicyTest.java
  - backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java
  - backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java
  - backend/src/test/java/com/odde/doughnut/testability/builders/SessionItemBuilder.java
  - e2e_test/features/learning_session/commissioned_learning_session.feature
  - e2e_test/start/pageObjects/recallPage.ts
  - e2e_test/step_definitions/learning_session.ts
  - frontend/src/components/recall/CommissionLearningSessionDialog.vue
  - frontend/src/components/recall/RecallProgressBar.vue
  - frontend/src/composables/useRecallData.ts
  - frontend/src/composables/useRecallPageLoading.ts
  - frontend/src/pages/RecallPage.vue
  - frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts
  - frontend/tests/components/recall/RecallProgressBar.spec.ts
  - frontend/tests/components/recall/assimilationPanelTestSupport.ts
  - frontend/tests/pages/recallPageTestSupport.ts
  - frontend/tests/toolbars/mainMenuMocks.ts
  - open_api_docs.yaml
  - packages/generated/doughnut-backend-api/index.ts
  - packages/generated/doughnut-backend-api/types.gen.ts
findings:
  critical: 1
  warning: 3
  info: 1
  total: 5
status: issues_found
---

# Phase 7: Code Review Report

**Reviewed:** 2026-08-08T01:56:00Z
**Depth:** standard
**Files Reviewed:** 27
**Status:** issues_found

## Summary

Phase 7 adds pre-session snapshot columns, an amend branch on `LearningSessionService.record`, `recordedSessions` on the recalling payload, recorded-session strip UI, and amend dialog mode. Snapshot re-grade math and the primary E2E amend path (single recorded session, no concurrent awaiting session) are well covered by unit and integration tests.

One **blocker** remains: when a notebook has both an **AWAITING_REPORT** session and a **RECORDED** session, the amend UI still calls the same `record` endpoint with only `notebookId`, and the service always prefers the awaiting session — so an “Amend report” action can record against the wrong session. Additional warnings cover legacy rows without snapshots, multi-recorded-session UX/API mismatch, and uncovered concurrent-session scenarios.

## Critical Issues

### CR-01: Amend dialog records awaiting session when both awaiting and recorded exist

**File:** `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java:82-105`
**Issue:** `record()` resolves `AWAITING_REPORT` before `RECORDED`. If the learner recorded session A, then commissioned session B (awaiting report), both strips appear on the recall page. Clicking **Amend report** on the recorded strip opens amend mode but `CommissionLearningSessionDialog` posts only `{ notebookId, reportMarkdown }`. The backend attaches the paste to session B (`isAmend = false`), not session A. Scores land on the wrong session; session A feedback and schedules stay stale while the user believes they amended the recorded session.

**Fix:** Add optional `learningSessionId` to `RecordLearningSessionRequest` and OpenAPI. When present and the session is `RECORDED`, target that session for amend regardless of awaiting sessions. Pass `learningSessionId` from `RecallProgressBar` / `CommissionLearningSessionDialog` in amend mode:

```java
// RecordLearningSessionRequest
public Integer learningSessionId; // optional; when set, amend that RECORDED session

// LearningSessionService.record — before awaiting branch
if (body.learningSessionId != null) {
  session = learningSessionRepository.findById(body.learningSessionId)
      .filter(s -> s.getUser().getId().equals(user.getId()))
      .filter(s -> s.getNotebook().getId().equals(notebook.getId()))
      .filter(s -> s.getStatus() == LearningSessionStatus.RECORDED)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "..."));
  isAmend = true;
} else if (!awaitingSessions.isEmpty()) {
  // existing first-record path
}
```

```typescript
// CommissionLearningSessionDialog.vue — add prop learningSessionId, include in record body when mode === 'amend'
body: {
  notebookId: props.notebookId,
  learningSessionId: props.mode === "amend" ? props.learningSessionId : undefined,
  reportMarkdown: reportMarkdown.value,
}
```

Add a controller test: recorded session + awaiting session for same notebook → amend request with recorded `learningSessionId` updates the recorded session, not awaiting.

## Warnings

### WR-01: Amend without snapshot compounds instead of re-grading

**File:** `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java:141-144`
**Issue:** On amend, snapshot restore runs only when `preSessionRecallCount != null`. Session items recorded before Phase 7 migration (or any row missing snapshots) skip restore and call `recordCommissionedFeedback` on the post-record tracker state — compound grading, violating D-01/D-02 and `07-RESEARCH.md` Pitfall 6.

**Fix:** When `isAmend` and snapshot is missing, reject the matched line with a clear reason (e.g. “Cannot amend: no pre-session snapshot for this item”) instead of applying feedback. Optionally backfill snapshots in a one-off migration for existing `session_item` rows where `feedback_score IS NOT NULL`.

### WR-02: Multiple RECORDED sessions per notebook — UI implies per-session amend, API amends latest only

**File:** `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java:95-103`, `frontend/src/components/recall/RecallProgressBar.vue:116-136`
**Issue:** After recommission and re-record, a notebook can hold multiple `RECORDED` sessions. The UI renders one amend row per session (keyed by `learningSessionId`), but the API selects only the latest by `recordedAt`. Clicking **Amend report** on an older row still amends the newest session (documented as A1 in research, but misleading and wrong if the user targets a specific past session).

**Fix:** Same as CR-01 — pass `learningSessionId` from the strip row into the amend dialog and API. Alternatively, filter `recordedSessions` to only the latest per notebook until session-targeted amend ships.

### WR-03: No test coverage for awaiting + recorded coexistence on amend

**File:** `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java`
**Issue:** Controller tests cover amend with a single recorded session and isolation across users, but not the realistic flow: record → commission again → attempt amend on recorded strip while awaiting exists. CR-01 would not be caught by current tests.

**Fix:** Add `Record` nested test: after record + recommission, call `record` with only `notebookId` and a report line matching the **recorded** session’s items; assert awaiting session unchanged and recorded session not amended (or, after CR-01 fix, assert amend with `learningSessionId` hits the correct session).

## Info

### IN-01: `learningSessionId` on lite DTOs is unused by amend client

**File:** `frontend/src/components/recall/CommissionLearningSessionDialog.vue:156-167`, `frontend/src/components/recall/RecallProgressBar.vue:156-165`
**Issue:** `RecordedLearningSessionLite` and strip rows carry `learningSessionId`, but amend mode never sends it to the API. The field is wired for display/keying only; session targeting cannot work until the client passes it (see CR-01).

**Fix:** Add `learningSessionId` prop to `CommissionLearningSessionDialog` in amend mode and include it in the record request body once the API accepts it.

---

_Reviewed: 2026-08-08T01:56:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
