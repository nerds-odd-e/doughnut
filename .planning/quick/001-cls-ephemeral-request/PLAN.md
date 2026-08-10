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

**Status:** done

- Backend: `record()` creates new `LearningSession` + `SessionItem`s with feedback;
  reschedules trackers. No `learningSessionId`, amend, due/request-membership checks.
- Frontend: Record from request UI without session id; show recorded/rejected items.
- E2E: paste report → session created → trackers rescheduled. Amend scenario removed.

**Learnings:** `abandonUnfinishedSessions()` + POST `/commission` remain until Phase 6.
`learningSessionId` DTO field / schema status columns deferred to Phases 5–6.
Proposed ADRs 0001/0003/0005 still need human update.

### Phase 4 — Behavior: List shows only potential sessions

**Status:** done

- Frontend: potential-only list labeled “Request”.
- Backend: removed awaiting/recorded from recall feed and awaiting-report exclusion.
- E2E: after recording, list shows only potential sessions.

**Learnings:** `findByUser_IdAndNotebook_IdAndStatus` kept for lingering commission until Phase 6.
Dialog filename still `CommissionLearningSessionDialog` (rename deferred / ADR vocabulary).

### Phase 5 — Structure: Data migration — clean up old data and constrain

**Status:** done

Flyway `V300000242__session_item_feedback_not_null.sql`: delete null-feedback
items + orphaned sessions; NOT NULL on `feedback_score` + `feedback_recorded_at`.
Entity/builders updated; commission tests removed (commission creates null feedback).

**Learnings:** POST `/commission` breaks if called until Phase 6 removes it (UI unused).

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
