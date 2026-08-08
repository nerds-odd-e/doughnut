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

---
