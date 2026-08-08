# Phase 6: record-report-and-schedule - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults; audit trail in `06-DISCUSSION-LOG.md`)

<domain>
## Phase Boundary

The learner **pastes a Learning Session Report** into an **awaiting-report**
Learning Session; Doughnut **records Feedback** on matched Session Items,
**reschedules** each commissioned tracker per ADR 0003, and marks the session
**recorded**.

One observable behavior: record report → schedule + feedback log (REC-01–REC-05).

Does **not** amend a prior recorded session (Phase 7 / AMD-01). Does not add
descriptive Feedback, machine transport, or new commission flows (Phase 5 /
PROT-*).

</domain>

<decisions>
## Implementation Decisions

### Report entry surface
- **D-01:** Extend **`CommissionLearningSessionDialog.vue`** in place — add a
  **post-commission / record** state with an editable **report textarea** and
  primary **Record report** CTA below the readonly Request. Same `Modal` +
  DaisyUI patterns as Phase 5; do not introduce a separate dialog component. —
  **Reversibility:** reversible — UI refactor only
- **D-02:** Report paste is **plain markdown text** in a `daisy-textarea`
  (`font-mono text-xs`, same family as Request display). No file upload, no
  rich editor, no `v-html`. — **Reversibility:** reversible

### Awaiting-session discovery (re-open after commission)
- **D-03:** After Phase 5, due commissioned trackers in **AWAITING_REPORT**
  sessions are excluded from `dueCommissioned`, so the potential-session row
  disappears. Add a sibling **awaiting-report strip** on `RecallProgressBar`
  (same column as potential sessions): one row per notebook with an
  `AWAITING_REPORT` session, glossary copy
  `1 learning session awaiting the tutor's report for notebook "{name}"`, and
  a **`Record report`** `daisy-btn-primary` that opens the dialog in **record
  mode** for that notebook. — **Reversibility:** reversible — additive UI +
  API field
- **D-04:** Expose **`awaitingReportSessions`** on the existing recalling /
  `DueMemoryTrackers` load path (notebook id, name, `learningSessionId`,
  optional `requestMarkdown` for dialog prefill). One round-trip with ordinary
  recall data; no separate sessions list page. — **Reversibility:** reversible
  — additive DTO field

### Record API contract
- **D-05:** Add **`POST /api/learning-sessions/record`** (name at planner
  discretion) accepting `{ notebookId, reportMarkdown }` + `timezone` query.
  Resolve the user's single **AWAITING_REPORT** session for that notebook;
  return structured result `{ status, recordedAt, recordedItems, rejectedEntries }`.
  Notebook-scoped, symmetric with commission — **Reversibility:** costly —
  published OpenAPI contract
- **D-06:** **Partial success** per ADR 0005: matched 0–5 integer scores are
  recorded and trackers rescheduled; unmatched titles and out-of-range scores are
  **rejected** and returned in `rejectedEntries` without rolling back matched
  items. Session moves to **RECORDED** when at least one item received Feedback;
  if zero matches, session stays **AWAITING_REPORT** (planner may refine edge
  copy). — **Reversibility:** one-way — persistence + schedule side effects

### Report parsing (backend)
- **D-07:** Implement **`LearningSessionReportParser`** (or equivalent pure
  helper) following ADR 0005 Report shape: `# Learning Session Report` header
  optional; lines `Note title: score` (tolerate trailing prose after score per
  ADR). Match Session Items by **note title** within the session's notebook;
  duplicate titles in one notebook → reject as unmatched (never guess). —
  **Reversibility:** reversible — internal module
- **D-08:** **REC-05 / unit-test primary:** reject unknown titles, non-integer
  scores, scores outside 0–5, and duplicate-title ambiguity. Do **not** add
  E2E scenarios for parse edge cases (REQUIREMENTS out-of-scope; `@wip` cap).

### Scheduling from score
- **D-09:** On each matched Session Item, apply ADR 0003 **commissioned
  learning session feedback** shifted-band mapping to the commissioned
  tracker's memory state: increment `recallCount`, set `lastRecalledAt`, schedule
  `nextRecallAt` via the normal interval path (no incorrect-recall relearning
  override). Score **5** must schedule **longer** than score **1** on the same
  starting state — E2E day-3 recommission lists only **Gracias**. —
  **Reversibility:** one-way — schedule mutations
- **D-10:** Persist Feedback on **`session_item`** (`feedbackScore`,
  `feedbackRecordedAt` — columns already exist). Set `learning_session.status =
  RECORDED` and `recordedAt` when recording succeeds with ≥1 match. —
  **Reversibility:** one-way

### Feedback visibility (REC-03)
- **D-11:** Show the **latest tutor feedback score** on the **commissioned**
  memory tracker in **assimilation settings** (`NoteInfoMemoryTracker` or
  sibling row): copy pattern
  `tutor feedback score {n} from a learning session` with
  `data-test="tutor-feedback-score-{n}"` or equivalent page-object-friendly
  marker so E2E step
  `I should see tutor feedback score 5 from a learning session for the memory
  tracker of note "Hola"` passes. Expose score via **MemoryTracker API shape**
  (new field or embedded lite) populated from latest recorded Session Item for
  that tracker. — **Reversibility:** costly — API + UI contract

### Recorded session marking (REC-04)
- **D-12:** After successful record, dialog shows informational banner
  `This learning session is recorded.` with
  `data-test="learning-session-recorded"`; hide awaiting banner. Awaiting-report
  strip row for that notebook disappears on `requestDueRecallsRefresh()`. No
  separate open-sessions list UI in MVP — **Reversibility:** reversible
- **D-13:** E2E assertion
  `the learning session for notebook "{title}" should be marked as recorded`
  checks dialog recorded banner and/or API status after record step (planner
  picks one stable observable). — **Reversibility:** reversible

### E2E scope
- **D-14:** Graduate **only** scenario
  `"Recording the tutor's report schedules each tracker from its score"` from
  `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
  into `e2e_test/features/learning_session/commissioned_learning_session.feature`
  (`@wip` until green). **Do not** graduate amend scenario (Phase 7). —
  **Reversibility:** reversible
- **D-15:** Add step definitions + page-object methods for: Given commissioned
  session (testability API or UI commission), When paste report + record,
  Then recorded status, recall counts on commissioned trackers, tutor feedback
  score in assimilation settings, and day-3 recommission asserting **only Gracias**
  in Request. — **Reversibility:** reversible

### Claude's Discretion
- Exact parser regex / line-splitting and whether to strip markdown headings
- Service method names and response DTO field names (must regenerate OpenAPI)
- Whether `requestMarkdown` is re-fetched on record-mode dialog open vs carried
  from `awaitingReportSessions` payload
- Tracer vs expansion plan split (single tracer covering record API + dialog +
  one E2E is viable)
- Unit-test file placement for parser vs controller vs scheduling policy

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase / requirements
- `.planning/ROADMAP.md` — Phase 6 goal, success criteria, E2E scenario name
- `.planning/REQUIREMENTS.md` — REC-01 through REC-05
- `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
  — draft recording scenario (lines 48–63) and amend scenario (deferred Phase 7)
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — session lifecycle,
  partial rejection, score→schedule summary, UI = progress bar dialog
- `.planning/phases/05-commission-learning-session/05-UI-SPEC.md` — DaisyUI
  dialog patterns, test ids, `apiCallWithLoading` contract (extend, do not fork)
- `.planning/phases/05-commission-learning-session/05-PATTERNS.md` — commission
  dialog + progress bar wiring

### Glossary / ADRs
- `docs/adrs/0001-ubiquitous-language.md` §3 — Learning Session Report,
  Feedback, Session Item, Tutor
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Report format,
  title matching, partial record/reject rules, amend deferred to same session
  (Phase 7 behavior)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — § Commissioned
  learning session feedback (0–5 shifted-band table, no relearning override)

### Phase 4–5 foundation (do not regress)
- `backend/.../services/LearningSessionService.java` — commission + abandon
- `backend/.../controllers/LearningSessionController.java` — commission endpoint
- `backend/.../services/RecallService.java` — awaiting-report exclusion from
  `dueCommissioned`
- `frontend/src/components/recall/CommissionLearningSessionDialog.vue` —
  commission + awaiting banner (extend for record)
- `e2e_test/features/learning_session/commissioned_learning_session.feature` —
  existing green commission scenarios

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CommissionLearningSessionDialog.vue` + `RecallProgressBar.vue` — extend for
  report textarea, record CTA, recorded/awaiting banners
- `LearningSessionService` / `LearningSessionController` — add `record` beside
  `commission`
- `SessionItem` entity — `feedbackScore`, `feedbackRecordedAt` already on table
- `LearningSession` — `status`, `recordedAt`; `LearningSessionStatus.RECORDED`
- `LearningSessionRequestMarkdownBuilder` — reuse for awaiting dialog prefill
- `SessionItemRepository.summarizeRecordedFeedbackByMemoryTrackerId` — pattern for
  feedback history queries
- `RecallService.getDueMemoryTrackers` + `DueMemoryTrackers` DTO — extend with
  `awaitingReportSessions`
- `NoteInfoMemoryTracker.vue` — add tutor feedback score column/row for
  COMMISSIONED type
- `makeMe.aLearningSession()` / `SessionItemBuilder.feedbackScore()` — test fixtures
- `e2e_test/step_definitions/learning_session.ts` + `recallPage.ts` — extend for
  record flow
- `e2e_test/step_definitions/assimilation_settings.ts` — `I open assimilation
  settings` for REC-03 assertion

### Established Patterns
- `apiCallWithLoading` + `timezoneParam()` + generated SDK for mutations
- Progress-bar **strip rows** for notebook-scoped learning-session affordances
  (Phase 3 potential / Phase 5 commission)
- Partial protocol acceptance — ADR 0005; unit tests for edge cases, one happy-path
  E2E per behavior phase
- Controller-level JUnit for API contracts; policy tests assert schedule movement
  not internal indexes (ADR 0003)
- `@wip` until green; targeted Cypress `--spec` for touched feature

### Integration Points
- `RecallsController.recalling` — add `awaitingReportSessions` without changing
  ordinary `toRepeat` / `dueCommissioned` semantics
- OpenAPI regenerate after new record endpoint + MemoryTracker feedback field
- `useRecallData` / `requestDueRecallsRefresh()` after record success
- `SpacedRepetitionAlgorithm` or dedicated commissioned-feedback scheduler in
  `algorithms/` — planner chooses smallest cohesive extension

</code_context>

<specifics>
## Specific Ideas

- E2E recording scenario uses report markdown verbatim from ADR 0005 example
  (`Hola: 5`, `Gracias: 1`) and asserts Hola not due on day 3 while Gracias is
- Success criterion 2: high vs low scores **diverge on later due work** — proven
  by recommission Request listing only Gracias
- Commission flow may already show awaiting banner with dialog open; record
  textarea can appear in that same view without closing dialog
- Re-open path required for Given steps that commission via testability without
  leaving dialog open

</specifics>

<deferred>
## Deferred Ideas

- Amend recorded session (re-paste Report updates Feedback) — Phase 7 (AMD-01);
  amend recomputation policy still Jidoka in plan-phase 7
- Open-sessions list showing all recorded sessions across notebooks — out of MVP
  (REC-04 satisfied by dialog + status enum)
- Descriptive Feedback prose stored or displayed — v2 (PROT-01)
- GET learning session by id as a standalone product surface — only if record
  flow needs it; prefer notebook-scoped record POST + recalling payload

None beyond roadmap — discussion stayed within phase scope.

</deferred>

---

*Phase: 06-record-report-and-schedule*
*Context gathered: 2026-08-08*
