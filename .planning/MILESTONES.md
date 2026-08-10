# Milestones

## v1.3 Commissioned Learning Session MVP (Shipped: 2026-08-08)

**Phases:** 7 | **Plans:** 14 | **Closeout:** verified (E2E `commissioned_learning_session.feature` green)

**Delivered:** Full offline commissioned learning loop — assimilate as commissioned (TRK), potential sessions on recall progress bar (POT), commission dialog with ADR 0005 Request (COM), record/amend Report with ADR 0003 scheduling and tutor feedback visibility (REC, AMD).

**Key accomplishments:**

- `type=COMMISSIONED` memory trackers excluded from ordinary due-recall; coexist with ordinary trackers on the same note
- Caret assimilate-as-commissioned; potential learning session rows per notebook on recall progress bar
- Learning Session / Session Item persistence; commission API + copyable Request markdown
- Record and amend flows with snapshot-based re-grade; awaiting/recorded session strips on recall page
- E2E: assimilate, potential sessions, commission, record, amend scenarios without `@wip`

**Stats:** ~179 files, +21.9k / −636 LOC since milestone start (2026-08-07)

**Post-milestone refactor (2026-08-08):** Structure cleanup across CLS backend,
frontend, and tests (split oversized test classes, unified `LearningSessionLite`,
extracted record targeting and feedback scheduling, `LearningSessionStrip`) — no
observable behavior change.

**Post-v1.3 polish (2026-08-08):** Request brief (tutor instructions, XML
sections, notebook QGI, report example); learning session hub (always-visible
bar icon, list modal → detail); tagged report scores
`<session_item_scores>`. E2E `commissioned_learning_session.feature` green.

**Post-v1.3 CLS refactor (2026-08-10):** Ephemeral request from due trackers;
session at record time; potential-only recall list; schema cleanup (no status,
commission, amend, or pre-session snapshots). E2E `commissioned_learning_session.feature`
green.

---
