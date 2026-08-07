# Phase 3: potential-learning-sessions - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults; audit trail in `03-DISCUSSION-LOG.md`)

<domain>
## Phase Boundary

Due **commissioned memory trackers** surface as **potential learning sessions**
grouped by notebook on the recall page progress bar, and do **not** appear as
ordinary recall work.

One observable behavior: potential sessions visible / ordinary recall empty of
commissioned work (TRK-03, POT-01, POT-02).

Does **not** create Learning Sessions, emit Requests, or open the commission
dialog (Phases 4–5).

</domain>

<decisions>
## Implementation Decisions

### Due-commissioned data feed
- **D-01:** Ordinary `toRepeat` remains COMMISSIONED-free (Phase 1). Expose
  **due commissioned trackers** (with enough notebook identity for grouping and
  E2E notebook-title assertions) on the existing recalling / `DueMemoryTrackers`
  load path so the recall page gets them in one round-trip. — **Reversibility:**
  reversible — additive DTO field; no persistence of Potential Learning Session
  entities
- **D-02:** A **potential learning session** is **derived in the frontend** by
  grouping those due commissioned trackers **by notebook** (Phase 1 lifecycle
  lock). Do **not** persist Potential Learning Session rows in this phase. A
  Learning Session entity exists only once commissioned (Phase 4–5)

### Progress-bar presentation
- **D-03:** On the recall page **top progress bar** area, show potential
  learning session(s) **by notebook name** so E2E can assert
  “1 potential learning session to commission for notebook {title}”. Use
  glossary wording (**potential learning session**). — **Reversibility:**
  reversible — UI presentation only
- **D-04:** Phase 3 is **display-only**. Do **not** open the commission dialog
  or create a Learning Session when the learner interacts with a potential
  session. Commission UI + Request belong to Phases 4–5 (milestone CONTEXT:
  dialog from progress bar)

### Ordinary recall separation
- **D-05:** Ordinary recall count, progress finished/`toRepeatCount`, and nav
  recall badge stay **ordinary-only**. Potential sessions are a **separate**
  affordance and must not inflate ordinary recall numbers (TRK-03 /
  success criterion 1)

### E2E scope
- **D-06:** Graduate into `e2e_test/features/learning_session/` (tag `@wip`
  until green) only the two Phase 3 scenarios from the draft feature:
  “Due commissioned trackers await a Tutor rather than ordinary recall” and
  “Notes from different notebooks are commissioned as separate learning
  sessions”. Do not graduate commission / record / amend scenarios in this phase

### Claude's Discretion
- Exact DTO field shape (flat due-commissioned lites vs pre-grouped notebook
  summaries) as long as D-01/D-02 hold and notebook title is assertable
- Exact DaisyUI / progress-bar slot markup and `data-testid` / `data-test` ids
  (page-object friendly; match existing recall progress patterns)
- Whether due-commissioned query reuses a sibling of `byUserIdFrom` that
  **selects only** COMMISSIONED vs a dedicated repository method — smallest
  coherent extension of `RecallService.getDueMemoryTrackers`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase / requirements
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, E2E scenario names
- `.planning/REQUIREMENTS.md` — TRK-03, POT-01, POT-02
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — potential
  session derived in frontend; UI surface = progress bar; Learning Session only
  once commissioned
- `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
  — draft scenarios for Phase 3 (lines covering potential sessions / multi-notebook)
- `.planning/phases/02-assimilate-as-commissioned/02-CONTEXT.md` — create path
  for COMMISSIONED; coexistence; do not regress

### Glossary / ADRs
- `docs/adrs/0001-ubiquitous-language.md` §3 — **Potential learning session**,
  Learning Session, Tutor, Commissioned memory tracker
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — protocol context
  (not implemented in Phase 3; Learning Sessions span one notebook)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — score→schedule (not
  Phase 3)

### Phase 1–2 foundation (do not regress)
- `.planning/phases/01-commissioned-tracker-model/01-VERIFICATION.md` —
  COMMISSIONED excluded from ordinary due-recall via `byUserIdFrom`
- `.planning/phases/02-assimilate-as-commissioned/02-VERIFICATION.md` —
  assimilate-as-commissioned create path green

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `backend/.../controllers/dto/DueMemoryTrackers.java` +
  `RecallService.getDueMemoryTrackers` — natural extension point for due
  commissioned payload alongside ordinary `toRepeat`
- `frontend/src/components/recall/RecallProgressBar.vue` +
  `frontend/src/components/commons/ProgressBar.vue` — recall top bar; add
  potential-session UI in/near `#buttons` or sibling slot
- `frontend/src/composables/useRecallData.ts` /
  `useRecallPageLoading.ts` — load recalling response into page state
- `packages/doughnut-test-fixtures` `DueMemoryTrackersBuilder` — extend for
  due-commissioned fixtures
- `makeMe.aMemoryTrackerFor(...).commissioned()` — backend/E2E fixtures ready
- Draft + graduated feature under
  `.planning/phases/01-.../commissioned_learning_session.feature` and
  `e2e_test/features/learning_session/commissioned_learning_session.feature`

### Established Patterns
- Ordinary due-recall already filters `type <> 'COMMISSIONED'` in
  `byUserIdFrom` (Phase 1) — potential sessions need a **positive** COMMISSIONED
  due query, not removal of that filter
- E2E page objects under `e2e_test/start/pageObjects/`; recall steps in
  `e2e_test/step_definitions/recall.ts` (“I should see that I have {int} notes
  to recall”)
- Capability-named E2E; `@wip` until green; CI `@wip` cap 5

### Integration Points
- `RecallsController.recalling` / menu `recallStatus` — both use
  `getDueMemoryTrackers`; keep ordinary fields unchanged when adding
  commissioned data
- `MemoryTrackerLite` today lacks notebook identity — either enrich lite for
  commissioned rows or introduce a small companion DTO
- OpenAPI regenerate after DTO change (`generate-api-client` skill)
- Nav badge via `useNavigationItems` / `toRepeatCount` must stay ordinary-only
  (D-05)

</code_context>

<specifics>
## Specific Ideas

- Success criteria map 1:1 to E2E: ordinary recall 0 with only due commissioned;
  one notebook → one potential session; two notebooks → two potential sessions
- Feature step wording to support:
  `I should see {n} potential learning session to commission for notebook "{title}"`
- Multi-notebook scenario title says “commissioned as separate learning
  sessions” but Phase 3 asserts **potential** sessions only (no commission
  action yet)

</specifics>

<deferred>
## Deferred Ideas

- Commission dialog from progress bar + Learning Session create — Phase 4–5
  (COM-*)
- Learning Session Request markdown — Phase 5 (ADR 0005)
- Record / amend report — Phases 6–7 (REC-*, AMD-01)
- Feedback score on tracker — REC-03 (Phase 6)

None beyond roadmap — discussion stayed within phase scope.

</deferred>

---

*Phase: 3-potential-learning-sessions*
*Context gathered: 2026-08-08*
