# Phase 7: amend-recorded-session - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults; audit trail in `07-DISCUSSION-LOG.md`)

<domain>
## Phase Boundary

The learner **pastes a later Learning Session Report** into an already **recorded**
Learning Session; Doughnut **updates Feedback** on matched Session Items,
**reschedules** commissioned trackers from the amended scores, and keeps the session
**visibly recorded**.

One observable behavior: amend recorded session → updated feedback + schedule
(AMD-01).

Does **not** add descriptive Feedback storage, machine transport, new commission
flows, or an open-sessions list beyond the progress-bar strip pattern from Phases
5–6.
</domain>

<decisions>
## Implementation Decisions

### Amend recomputation (Jidoka — ROADMAP)
- **D-01:** **Re-grade from pre-session snapshot**, not compound on the
  post-record tracker state. A later Report **replaces** the scheduling effect of
  the prior score for matched items; one commissioned Learning Session counts as
  **one** graded recall event (`recallCount` must not increment again on amend). —
  **Reversibility:** one-way — requires persisting or deriving pre-session tracker
  fields and amending `recordCommissionedFeedback` usage for amend paths
- **D-02:** On **first** record of a Session Item, persist a **pre-session snapshot**
  on `session_item` (at minimum `preSessionForgettingCurveIndex` and
  `preSessionRecallCount` before applying Feedback). On amend for a matched item:
  restore snapshot fields on the tracker, then apply `recordCommissionedFeedback`
  once with the new score. Items with no matching amend line keep their current
  tracker state. — **Reversibility:** one-way — Flyway columns on `session_item`
- **D-03:** `feedbackRecordedAt` on amended items updates to the amend instant;
  `learning_session.recordedAt` may update to the amend instant (planner picks one
  consistent rule; E2E does not assert timestamp). Status stays **RECORDED**. —
  **Reversibility:** reversible

### Record API contract (extend Phase 6)
- **D-04:** Extend the existing **`POST /api/learning-sessions/record`** notebook-scoped
  endpoint: when no `AWAITING_REPORT` session exists, resolve the user's latest
  **`RECORDED`** session for that notebook and treat the paste as an **amend**.
  Return the same `RecordLearningSessionResponse` shape (`status`, `recordedAt`,
  `recordedItems`, `rejectedEntries`). — **Reversibility:** costly — published
  OpenAPI behavior change
- **D-05:** **Partial amend** uses the same rules as Phase 6 / ADR 0005: matched
  0–5 integer lines update Feedback and reschedule; unmatched titles and
  out-of-range scores are rejected without rolling back other matched amendments
  in the same request. Session stays **RECORDED** when ≥1 item amended
  successfully; zero matches leaves prior Feedback unchanged. — **Reversibility:**
  one-way — schedule side effects

### Amend entry surface (UI)
- **D-06:** Mirror Phase 6 **awaiting-report strip** with a sibling **recorded-session
  strip** on `RecallProgressBar`: one row per notebook with a **RECORDED**
  session (glossary copy e.g. `1 recorded learning session for notebook "{name}"`),
  primary **`Amend report`** CTA opening `CommissionLearningSessionDialog` in
  **amend mode** (`mode` analogous to existing `record` mode). — **Reversibility:**
  reversible — additive UI
- **D-07:** Expose **`recordedSessions`** on the existing recalling /
  `DueMemoryTrackers` load path (notebook id, name, `learningSessionId`, optional
  `requestMarkdown` for dialog display). One round-trip with recall data; no new
  sessions page. — **Reversibility:** reversible — additive DTO field
- **D-08:** In amend mode, dialog shows readonly Request, **recorded** banner
  (`data-test="learning-session-recorded"`), editable report textarea, and
  **`Record report`** button (reuse `data-test="record-learning-session-report"`
  so existing page-object `recordLearningSessionReport` works). Hide textarea for
  commission-only flows. — **Reversibility:** reversible

### Feedback visibility (REC-03 carry-over)
- **D-09:** After amend, **latest tutor feedback score** on the commissioned tracker
  reflects the **amended** score (existing `latestTutorFeedbackScore` / assimilation
  settings row and E2E step
  `I should see tutor feedback score {n} from a learning session for the memory
  tracker of note "{title}"`). — **Reversibility:** reversible

### Potential-session membership (AMD-01 / success criterion 2)
- **D-10:** Amended scores drive **subsequent** `dueCommissioned` / potential-session
  membership the same way first-record scores do. E2E proof: after amending Gracias
  from 1 → 4 on day 2, day 3 shows **0** potential learning sessions for that
  notebook (both Hola and Gracias not due). — **Reversibility:** one-way — policy
  tests must lock snapshot re-grade math

### E2E scope
- **D-11:** Graduate **only** scenario
  `"A later report amends the feedback of a recorded learning session"` from
  `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
  into `e2e_test/features/learning_session/commissioned_learning_session.feature`
  (`@wip` until green). — **Reversibility:** reversible
- **D-12:** Add Given step `I have recorded a learning session for notebook … on day
  {n} with scores:` (table) plus any amend-specific wiring; reuse When/Then steps
  from Phase 6 where possible. — **Reversibility:** reversible
- **D-13:** **Unit-test primary** for amend recomputation edge cases (snapshot
  restore, no double `recallCount`, compound-vs-snapshot regression). Do not grow
  E2E for parse edge cases. — **Reversibility:** reversible

### Claude's Discretion
- Exact snapshot column names and whether `preSessionLastRecalledAt` is needed
- Service method split (`record` vs `amend`) vs single method with status branch
- Whether `recordedAt` on session updates on amend
- Tracer vs expansion plan split (single tracer covering amend API + strip + E2E
  is viable)
- MakeMe builder helpers for pre-recorded sessions in Given steps

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase / requirements
- `.planning/ROADMAP.md` — Phase 7 goal, success criteria, E2E scenario name, Jidoka
- `.planning/REQUIREMENTS.md` — AMD-01
- `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
  — amend scenario (lines 65–78)
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — re-recording =
  amend; open amend recomputation note
- `.planning/phases/06-record-report-and-schedule/06-CONTEXT.md` — record API,
  dialog, strips, partial success, E2E graduation pattern

### Glossary / ADRs
- `docs/adrs/0001-ubiquitous-language.md` §3 — Feedback, Learning Session Report
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — amend semantics
  (§ Matching and recording items 5–6); snapshot vs compound deferred to ADR 0003
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — § Commissioned learning
  session feedback (shifted-band table; one graded event per recorded score)

### Phase 6 foundation (extend, do not regress)
- `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java` —
  `record` (extend for RECORDED)
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` —
  `recordCommissionedFeedback`
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicy.java`
- `backend/src/main/java/com/odde/doughnut/entities/SessionItem.java` — add snapshot
  columns
- `frontend/src/components/recall/CommissionLearningSessionDialog.vue` — amend UI
- `frontend/src/components/recall/RecallProgressBar.vue` — recorded-session strip
- `e2e_test/features/learning_session/commissioned_learning_session.feature` —
  graduate amend scenario
- `e2e_test/start/pageObjects/recallPage.ts` — `recordLearningSessionReport`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `LearningSessionService.record` — branch to latest RECORDED session when no
  AWAITING_REPORT session; apply snapshot amend path
- `CommissionLearningSessionDialog.vue` — extend `mode` for amend; show textarea
  when `status === 'RECORDED'`
- `RecallProgressBar.vue` + `awaitingReportSessions` pattern — clone for
  `recordedSessions`
- `RecallService.getDueMemoryTrackers` / `DueMemoryTrackers` DTO — add recorded list
- `SessionItem` + Flyway migration — pre-session snapshot columns
- `LearningSessionReportParser` — unchanged parse rules
- `SessionItemRepository.summarizeRecordedFeedbackByMemoryTrackerId` — latest score
  for REC-03 after amend
- `makeMe.aLearningSession()` / controller tests in `LearningSessionControllerTests`
- `e2e_test/step_definitions/learning_session.ts` — extend Given for recorded session

### Established Patterns
- Notebook-scoped symmetric commission/record POST bodies
- Progress-bar strip rows for learning-session affordances (potential / awaiting)
- Partial protocol acceptance — ADR 0005; unit tests for policy math
- `@wip` until green; targeted Cypress `--spec` for touched feature
- Policy tests assert schedule movement, not internal indexes (ADR 0003)

### Integration Points
- `RecallsController.recalling` — add `recordedSessions` payload
- OpenAPI regenerate after DTO + `session_item` migration
- `useRecallData` / `requestDueRecallsRefresh()` after amend success
- `CommissionedLearningSessionFeedbackPolicyTest` — extend for amend snapshot path

</code_context>

<specifics>
## Specific Ideas

- E2E amend scenario: day-2 record Hola 4 / Gracias 1, amend report `Gracias: 4`,
  assert tutor feedback 4 on Gracias, day-3 **0** potential sessions for Spanish
  conversation
- Snapshot re-grade aligns with ADR 0005 "amends" wording and avoids double
  `recallCount` from one Tutor session
- Reuse dialog scoped selectors from Phase 6 (`commission-learning-session-dialog`
  + `learning-session-report`) so amend does not fight awaiting-strip homonyms

</specifics>

<deferred>
## Deferred Ideas

- Human ADR revision explicitly locking snapshot amend in ADR 0003 text — agents
  implement per CONTEXT; human approval of Proposed ADRs remains separate
- Open-sessions list across notebooks — out of MVP (REC-04 satisfied by dialog +
  strip)
- Descriptive Feedback prose stored or displayed — v2 (PROT-01)
- Compound amend semantics — rejected for this phase (see D-01)

None beyond roadmap — discussion stayed within phase scope.

</deferred>

---

*Phase: 07-amend-recorded-session*
*Context gathered: 2026-08-08*
