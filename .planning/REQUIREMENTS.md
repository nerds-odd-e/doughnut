# Requirements: Doughnut

**Defined:** 2026-08-07
**Core Value:** Healthy mainline for learning and knowledge work
**Milestone:** v1.3 Commissioned Learning Session MVP

## v1 Requirements

Behavioral scope is locked by
`.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`
and CONTEXT.md. Glossary: ADR 0001 §3. Protocol: ADR 0005. Score→schedule: ADR 0003.

### Commissioned tracker

- [x] **TRK-01**: User can assimilate a note as a commissioned memory tracker via a caret dropdown next to Assimilate (not offered for properties in the UI)
- [x] **TRK-02**: A commissioned memory tracker coexists with ordinary trackers for the same note
- [x] **TRK-03**: Due commissioned memory trackers do not appear as ordinary recall work

### Potential learning session

- [x] **POT-01**: User sees potential learning sessions (due commissioned trackers grouped by notebook) from the recall page progress bar
- [x] **POT-02**: Due commissioned trackers from different notebooks form separate potential learning sessions

### Commission and request

- [ ] **COM-01**: User can commission a Learning Session for a notebook from a dialog opened on the recall page progress bar
- [ ] **COM-02**: Commissioning produces a Learning Session Request (markdown per ADR 0005) listing Session Items with expected learning content, learning status, and the 0–5 scoring rubric
- [ ] **COM-03**: After commissioning, the Learning Session awaits the Tutor's report

### Record and schedule

- [ ] **REC-01**: User can paste a Learning Session Report into a Learning Session and record it
- [ ] **REC-02**: Recording applies a Feedback score to each matched Session Item and reschedules the commissioned tracker per ADR 0003 (shifted-band mapping)
- [ ] **REC-03**: Recorded Feedback score is visible on the commissioned memory tracker
- [ ] **REC-04**: A Learning Session that has recorded Feedback is visibly marked among sessions
- [ ] **REC-05**: Matched report entries are recorded; unmatched or out-of-range entries are rejected and reported to the learner (unit-test primary; not E2E suite growth)

### Amend

- [ ] **AMD-01**: User can paste a later Learning Session Report that amends Feedback on a recorded session and reschedules accordingly

## v2 Requirements

Deferred beyond this MVP.

### Protocol and tutor

- **PROT-01**: Descriptive feedback and recommendations recorded as Feedback (not score-only)
- **PROT-02**: Smart / AI-assisted Learning Session Request shaping
- **PROT-03**: In-app agentic Tutor
- **PROT-04**: Machine transport for Request/Report (HTTP or MCP)

### Tracker

- **TRK-04**: Commissioned trackers for properties (domain allowed; UI deferred)
- **TRK-05**: Commissioned assimilation (first intake via Tutor), not only recall

## Out of Scope

| Feature | Reason |
|---------|--------|
| Session identity codes in the protocol documents | Learner opens the session and loads the report into it (ADR 0005) |
| Notebook-level opt-in for commissioned tracking | Per-note caret dropdown is the agreed surface |
| Replacing ordinary trackers with commissioned | Coexistence is required |
| In-app agent stub for Tutor | Copy-paste; Tutor identity is outside Doughnut |
| Growing E2E for report-parse edge cases | Unit tests only (CI `@wip` cap) |
| SEED-001 spelling follow-ons | Parked seed |
| ADR 0002 Level 1 git-native notebooks | Separate milestone |

## Traceability

Filled by roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| TRK-01 | Phase 2 | Complete |
| TRK-02 | Phase 2 | Complete |
| TRK-03 | Phase 3 | Complete |
| POT-01 | Phase 3 | Complete |
| POT-02 | Phase 3 | Complete |
| COM-01 | Phase 5 | Pending |
| COM-02 | Phase 5 | Pending |
| COM-03 | Phase 5 | Pending |
| REC-01 | Phase 6 | Pending |
| REC-02 | Phase 6 | Pending |
| REC-03 | Phase 6 | Pending |
| REC-04 | Phase 6 | Pending |
| REC-05 | Phase 6 | Pending |
| AMD-01 | Phase 7 | Pending |

**Coverage:**

- v1 requirements: 14 total
- Mapped to phases: 14
- Unmapped: 0 ✓

---
*Requirements defined: 2026-08-07*
*Last updated: 2026-08-07 after roadmap*
