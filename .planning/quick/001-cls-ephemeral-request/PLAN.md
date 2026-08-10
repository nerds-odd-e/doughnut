# CLS: Ephemeral Request, Session at Record Time

## Context

v1.3 CLS MVP creates `LearningSession` + `SessionItem`s at **commission** time
(`POST /commission`), then records/amends feedback on persisted items. Request
markdown is derived from the persisted session.

**Problem:** Sessions created too early (user might never report → stale
AWAITING_REPORT sessions). Amend flow adds complexity (record target resolver,
pre-session snapshots, status enum). Partial reports leave null-feedback items.

**Goal:** Request is **ephemeral** (recalculated from due trackers, never
persisted). Session created **only when user records a report**. Every session
item has feedback. No amend, no view — list shows only potential sessions.

## Design Decisions

1. **Request ephemeral.** `GET /api/learning-sessions/request?notebookId=…`
   returns markdown from due commissioned note-level trackers. No persistence.
   400 if no due trackers.
2. **Session at record time.** `POST /record` creates new `LearningSession` +
   `SessionItem`s from parsed report. Every item gets feedback. No `learningSessionId`.
3. **Validation simplified.** Record checks only: (a) note title in notebook,
   (b) not ambiguous, (c) commissioned tracker exists. No due-ness or request-membership check.
4. **No amend, no view.** Feedback is final. List shows only potential sessions.
   Unreported items stay due; user generates new request next time.
5. **Schema full cleanup.** Drop `status`, `commissioned_at`, snapshot columns.
   NOT NULL on `feedback_score` + `feedback_recorded_at`. Migration deletes
   null-feedback items + orphaned sessions first.
6. **No re-grading.** Once recorded, feedback is final. Tracker due again
   naturally = new review cycle, not re-grade.

## Phases

### Phase 1 — Structure: Refactor request markdown builder to work from trackers

**Status:** done

Refactor `LearningSessionRequestMarkdownBuilder` to build from `List<MemoryTracker>`
+ `Notebook` + `User` + `ZoneId` instead of persisted `SessionItem`s. Old
`commission()` adapts to use new builder. Markdown output unchanged.

- New method `build(user, notebook, trackers, zoneId)`; old `build(session, zoneId)` delegates
- Update `LearningSessionService.commission()` to use new builder path
- Existing backend tests pass unchanged

**Learnings:** Session `build` kept as thin delegate for `RecallService` until later phases drop commission path.

### Phase 2 — Behavior: User views request directly without commission button

**Status:** done

- Backend: `GET /api/learning-sessions/request?notebookId=…&timezone=…` →
  `{ requestMarkdown }` from due trackers (no session). 400 if none due.
- Frontend: Dialog opens in request mode (spinner → markdown + report UI). No commission CTA.
- POST /commission kept (unused by frontend) until Phase 6.
- E2E: open request → see markdown → no session in DB.

**Learnings:** List/entry still says “Commission” (Phase 4 cleanup). Record still uses old commission-based API until Phase 3. ADR 0001/0003/0005 (Proposed) still describe commission-time sessions — human-owned update.

### Phase 3 — Behavior: User records a report → session created with feedback

**Status:** planned

- Backend: Rewrite `LearningSessionService.record()` — create new `LearningSession`
  + `SessionItem`s from parsed report. Each item: `feedbackScore` +
  `feedbackRecordedAt` + `noteTitle` + `memoryTracker`. Reschedule trackers.
  No `learningSessionId`, no amend, no due check, no request-membership check.
- Frontend: Report textarea + "Record report" button → new POST /record (no
  `learningSessionId`). Show recorded/rejected items.
- E2E: user pastes report → session created → trackers rescheduled.

**Remove this phase:**
- Backend `LearningSessionService`: amend branch (`isAmend`, snapshot restore,
  AWAITING_REPORT transitions), initial-record snapshot capture,
  `LearningSessionRecordTargetResolver` dependency + usage, `abandonUnfinishedSessions()`
- Backend `LearningSessionController`: `learningSessionId` wiring to record
- Frontend `CommissionLearningSessionDialog.vue`: `mode: "amend"`,
  `learningSessionId` in record body, status alerts, `LearningSessionCommissionResponse`
  import, `data.status === "RECORDED"` branch
- Delete `LearningSessionAmendTests.java` entirely
- `LearningSessionRecordTests.java`: remove `allLinesRejectedStaysAwaitingReport`,
  `notFoundWhenNoSessionToRecordOrAmend`, `initialRecordCapturesPreSessionSnapshot`;
  rewrite remaining for record-creates-session
- `LearningSessionControllerTestBase.java`: remove `commissionRequest()`,
  `recordRequest(..., learningSessionId)` overload, `recordedLearningSession()`
  with status+commissionedAt, `commissionAndRecordSpanishNotebook()` commission step,
  `RecordedAndAwaitingSessions`, `commissionRecordAndRecommission()`
- `LearningSessionBuilder.java`: remove `commissionedAt()`
- `SessionItemBuilder.java`: remove `preSessionForgettingCurveIndex()`,
  `preSessionRecallCount()`
- `CommissionLearningSessionDialog.spec.ts`: amend + `learningSessionId` tests,
  record-after-commission tests (rewrite for direct record)
- `RecallProgressBar.spec.ts`: amend flow describe, "Record report" entry flow
- E2E `commissioned_learning_session.feature`: remove amend scenario; rewrite
  record scenario without commission; remove "awaiting the tutor's report"
- E2E `learning_session.ts`: remove amend steps; rewrite recorded setup without
  commission
- E2E `recallLearningSessionMethods.ts`: remove `selectRecordOrAmendAction`,
  `openAmendLearningSessionReport`, `expectLearningSessionAwaitingReport`,
  `expectLearningSessionRecorded`; remove 'Record report'/'Amend report' labels

### Phase 4 — Behavior: List shows only potential sessions

**Status:** planned

- Frontend: `LearningSessionListDialog` shows only potential sessions.
- Backend: Remove `awaitingReportSessions` + `recordedSessions` from recall feed.
  Remove `findMemoryTrackerIdsInAwaitingReportSessions` exclusion logic.
- E2E: after recording, list only shows potential sessions.

**Remove this phase:**
- Backend `DueMemoryTrackers.java`: `awaitingReportSessions`, `recordedSessions` fields
- Backend `RecallService.java`: `awaitingReportTrackerIds` exclusion filter,
  `setAwaitingReportSessions` / `setRecordedSessions`, `learningSessionLitesFor` by status
- Backend `SessionItemRepository.java`: `findMemoryTrackerIdsInAwaitingReportSessions`
- Backend `LearningSessionRepository.java`: `findByUser_IdAndNotebook_IdAndStatus`,
  `findByUser_IdAndStatus`
- Backend `LearningSessionLite.java`: delete if no longer used by recall feed
- Frontend `RecallLearningSessionActions.vue`: `awaitingReportSessions` /
  `recordedSessions` props, "Record report" / "Amend report" entry loops
- Frontend `LearningSessionListDialog.vue`: `"record" | "amend"` from action mode
- Frontend `RecallProgressBar.vue`: awaiting/recorded props, amend wiring
  (`learningSessionId`, `initialRequestMarkdown`), `@commissioned` event
- Frontend `useRecallData.ts`: `awaitingReportSessions`, `recordedSessions`, setters
- Frontend `useRecallPageLoading.ts`: awaiting/recorded in `applySessionStrips`
- Frontend `RecallPage.vue`: awaiting/recorded strip props
- `RecallsCommissionedLearningSessionTests.java`: remove
  `excludesDueCommissionedTrackersAwaitingReportAfterCommission`,
  `returnsAwaitingReportSessionsAfterCommission`,
  `returnsRecordedSessionsAfterRecord`, `recordedSessionsDoesNotLeakAcrossUsers`,
  `awaitingReportExclusionDoesNotLeakAcrossUsers`; remove commission setup from survivors
- `RecallProgressBar.spec.ts`: badge count with awaiting+recorded, awaiting list flow
- `recallPageTestSupport.ts`, `mainMenuMocks.ts`, `assimilationPanelTestSupport.ts`:
  remove awaiting/recorded mocks
- E2E `commissioned_learning_session.feature`: remove commission flow scenario
- E2E `recall.ts`: update "potential learning session to commission" wording
- E2E `recallLearningSessionMethods.ts`: update `expectPotentialLearningSession`

### Phase 5 — Structure: Data migration — clean up old data and constrain

**Status:** planned

Flyway migration:
- Delete `session_item` rows where `feedback_score IS NULL`
- Delete orphaned `learning_session` rows (no remaining items)
- Add NOT NULL on `session_item.feedback_score` + `feedback_recorded_at`
- Existing tests pass (updated for constraint where needed)

### Phase 6 — Structure: Schema + dead code cleanup

**Status:** planned

Drop obsolete columns and remove all remaining dead code.

**Flyway migration:**
- Drop `learning_session.status`, `learning_session.commissioned_at`
- Drop `session_item.pre_session_forgetting_curve_index`,
  `session_item.pre_session_recall_count`
- Drop index `idx_learning_session_user_notebook_status`

**Delete entire files:**
- `LearningSessionStatus.java`
- `LearningSessionRecordTargetResolver.java`
- `LearningSessionCommissionResponse.java`
- `CommissionLearningSessionRequest.java`
- `LearningSessionCommissionTests.java` (if not deleted earlier)

**Remove from entities:**
- `LearningSession.java`: `status` field + getter/setter, `commissionedAt` field +
  getter/setter
- `SessionItem.java`: `preSessionForgettingCurveIndex`, `preSessionRecallCount`
  fields + getters/setters

**Remove from algorithms:**
- `CommissionedLearningSessionFeedbackScheduling.java`:
  `restorePreSessionSnapshot()` (keep `recordFeedback()`)

**Remove from DTOs:**
- `RecordLearningSessionRequest.java`: `learningSessionId` field
- `RecordLearningSessionResponse.java`: `status` (`LearningSessionStatus`) field

**Remove from repositories:**
- `SessionItemRepository.java`: status-based predicates in
  `summarizeRecordedFeedbackByMemoryTrackerId` + `findLatestFeedbackScoreByMemoryTrackerId`
  (methods survive, status filter removed)

**Remove old commission endpoint:**
- `LearningSessionController.java`: `POST /commission` mapping + `commission()` method
- `LearningSessionService.java`: `commission()`, `toCommissionResponse()`,
  `createSessionItem()` without feedback

**Regenerate:**
- `docs/database-erd.md` (via `database-erd` skill)
- API client (via `generate-api-client` skill — OpenAPI, `types.gen.ts`, `sdk.gen.ts`,
  `api-summary.md`)

## Docs to Update (human-owned ADR process)

These ADRs reference commission/amend/awaiting and need human review to update.
Agents flag the conflict; humans own the ADR advice process.

- `docs/adrs/0005-commissioned-learning-session-protocol.md`: remove amend semantics
  (lines 169–174), partial-report-with-null-feedback (lines 167–168),
  "between commissioning and recording" (line 191), learner picks target session
  (lines 48–49)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`: update "commissions another
  Learning Session" (lines 153–154), abandoned session items (lines 158–159)
- `docs/adrs/0001-ubiquitous-language.md`: review "Potential learning session" /
  "commissioned" orchestrator verb (lines 132, 157–159) — commission step removed

## Notes

- Phases 1→4: behavior change (ephemeral request + session at record + list cleanup).
- Phases 5→6: cleanup (data migration + schema/dead code removal).
- Each phase is stop-safe: app works after each phase.
- Phase 2 and 3 share the same dialog component — sequential is safer than parallel.
- ADR updates are human-owned; agent flags conflicts only.
