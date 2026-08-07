# Roadmap: Doughnut

## Milestones

- ✅ **v1.0 Notebook Lint & Auto-Fix** — shipped 2026-07-23
- ✅ **v1.1 Spelling Answer Match & Link** — shipped 2026-07-25
- ✅ **v1.2 Accidental Match Resolve UX** — shipped 2026-08-06
- 🔄 **v1.3 Commissioned Learning Session MVP** — in progress

## Phases

### Phase 1: Commissioned tracker model (Structure)

**Goal:** Persist a commissioned memory tracker variant and keep it out of ordinary due-recall selection, without changing any user-visible path yet.

**Type:** Structure — enables Phase 2 (assimilate as commissioned).

**Requirements:** (none user-facing; unlocks TRK-*)

**Success criteria:**

1. Existing assimilation and recall **backend unit** suites still pass unchanged (`pnpm backend:verify`). E2E is N/A for this Structure phase unless a product path is accidentally touched — then run targeted E2E only; do not add or graduate Phase 1 E2E scenarios.
2. Domain model can represent a commissioned memory tracker (coexisting with ordinary trackers on the same note)
3. Due-recall selection never returns commissioned trackers

**Context:** `.planning/phases/01-commissioned-tracker-model/`

**Plans:** 2 plans

Plans:
**Wave 1**

- [ ] 01-01-PLAN.md — Tracer: `type=COMMISSIONED` due-recall / count exclusion via `byUserIdFrom` + SC3 (SC2 stay green)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 01-02-PLAN.md — Expansion: assimilation join + property target gate + batch candidate exclusion (SC1)

---

### Phase 2: Assimilate as commissioned (Behavior)

**Goal:** User can create a commissioned memory tracker from the assimilation caret dropdown.

**Type:** Behavior — one observable: assimilate note as commissioned.

**Requirements:** TRK-01, TRK-02

**Success criteria:**

1. Caret next to Assimilate opens a dropdown to assimilate as commissioned (not offered for properties)
2. Assimilation settings show a commissioned memory tracker for that note
3. Ordinary trackers for the same note still exist when present (coexistence)

**E2E:** Scenario "Assimilating a note with a tutor creates a commissioned memory tracker"

**Context:** `.planning/phases/02-assimilate-as-commissioned/`

---

### Phase 3: Potential learning sessions (Behavior)

**Goal:** Due commissioned trackers surface as potential learning sessions by notebook on the recall page, not as ordinary recall.

**Type:** Behavior — one observable: potential sessions visible / ordinary recall empty of commissioned work.

**Requirements:** TRK-03, POT-01, POT-02

**Success criteria:**

1. With only due commissioned trackers, ordinary recall count is 0
2. Recall progress bar offers potential learning session(s) grouped by notebook
3. Two notebooks with due commissioned trackers yield two potential sessions

**E2E:** Scenarios "Due commissioned trackers await a Tutor…" and "Notes from different notebooks…"

**Context:** `.planning/phases/03-potential-learning-sessions/`

---

### Phase 4: Learning Session and Request builder (Structure)

**Goal:** Persist Learning Session / Session Item and build Learning Session Request markdown (ADR 0005), without the commission UI yet.

**Type:** Structure — enables Phase 5 (commission + request).

**Requirements:** (unlocks COM-*)

**Success criteria:**

1. Existing product behavior unchanged (prior E2E still green)
2. A Learning Session can be created in domain/API with Session Items for due commissioned trackers of one notebook
3. Request markdown includes session items, expected learning content, learning status, and the 0–5 rubric

**Context:** `.planning/phases/04-learning-session-request-builder/`

---

### Phase 5: Commission Learning Session (Behavior)

**Goal:** User commissions a Learning Session from the recall progress bar dialog and receives a copyable Request while the session awaits the report.

**Type:** Behavior — one observable: commission → Request + awaiting report.

**Requirements:** COM-01, COM-02, COM-03

**Success criteria:**

1. User opens the dialog from the recall progress bar and commissions a notebook’s potential session
2. Request lists the due Session Items with content, status, and scoring instruction
3. Session is in awaiting-report state after commission
4. Commissioning abandons prior unfinished sessions / items without Feedback for that notebook (per CONTEXT lifecycle)

**E2E:** Scenario "Commissioning a learning session produces a request for the tutor"

**Context:** `.planning/phases/05-commission-learning-session/`

---

### Phase 6: Record report and schedule (Behavior)

**Goal:** User records a Learning Session Report; matched scores update trackers and Feedback; session is marked recorded.

**Type:** Behavior — one observable: record report → schedule + feedback log.

**Requirements:** REC-01, REC-02, REC-03, REC-04, REC-05

**Success criteria:**

1. Pasting a valid Report records Feedback scores on matched Session Items
2. Trackers reschedule per ADR 0003 shifted-band mapping; high vs low scores diverge on later due work
3. Feedback score is visible on the commissioned tracker; session is marked recorded
4. Unmatched / out-of-range entries are rejected and reported (unit tests; do not grow E2E `@wip`)

**E2E:** Scenario "Recording the tutor's report schedules each tracker from its score"

**Context:** `.planning/phases/06-record-report-and-schedule/`

---

### Phase 7: Amend recorded session (Behavior)

**Goal:** A later Report amends Feedback on a recorded session and reschedules.

**Type:** Behavior — one observable: amend recorded session.

**Requirements:** AMD-01

**Success criteria:**

1. Re-pasting a Report into a recorded session updates Feedback for matched items
2. Amended scores drive subsequent potential-session membership
3. Recorded marking remains visible

**E2E:** Scenario "A later report amends the feedback of a recorded learning session"

**Jidoka:** Decide amend recomputation (re-grade from pre-session snapshot vs compound) during `/gsd-plan-phase 7` — not yet in ADRs.

**Context:** `.planning/phases/07-amend-recorded-session/`

---

## Progress

| Phase | Name | Type | Req | Status |
|-------|------|------|-----|--------|
| 1 | Commissioned tracker model | Structure | — | Planned |
| 2 | Assimilate as commissioned | Behavior | TRK-01, TRK-02 | Pending |
| 3 | Potential learning sessions | Behavior | TRK-03, POT-01, POT-02 | Pending |
| 4 | Learning Session and Request builder | Structure | — | Pending |
| 5 | Commission Learning Session | Behavior | COM-01–03 | Pending |
| 6 | Record report and schedule | Behavior | REC-01–05 | Pending |
| 7 | Amend recorded session | Behavior | AMD-01 | Pending |

**Coverage:** 14/14 v1 requirements mapped ✓

## Backlog

(none for this capability — 999.1 promoted into v1.3)

Parked elsewhere: SEED-001, ADR 0002 Level 1.

---
*Last updated: 2026-08-07 — Phase 1 replanned for type=COMMISSIONED filters*
