# Plan: RecallLog

**Status:** in progress (slice 12 next)

**Goal:** Memory-state transitions are a RecallLog. Prompt submissions stay `answer`. Tutor Feedback is a log row, not a session bag.

## Design

- Vertical slice: lock E6 in ADR 0003 Decision, then one observable log behavior at a time. No unused columns for fitting.
- Write logs at the **grade caller** (just review, prompt grading, `recordFeedback`, confusion), not only inside `recalledAgain` — Tutor **0** and **1** both call `recalledAgain` but must log `AGAIN_ZERO` vs `AGAIN`.
- Hooking `markAsRecalled` will also log prompt Yes/No before `answer_id` is set. That is allowed interim; the next prompt slice sets `answer_id`. Overlap must still write **no** log (it does not call `markAsRecalled`).
- `GET /api/memory-trackers/{id}/recall-logs` is the Memory Tracker surface. After a DTO change, `pnpm generateTypeScript`.
- Next Flyway: `V300000267`. `recall_log.memory_tracker_id` ON DELETE CASCADE. Include logs in hard-delete fixtures (`unit-testing.mdc`).
- Jidoka before dropping `learning_session`: [ADR 0005](../../../docs/adrs/0005-commissioned-learning-session-protocol.md) still describes paste-into-session and amend-in-place; code already does not. Slice 12 drafts that ADR to match “session = Request/Report activity, not a table.”

## Slices

### 1. Lock RecallLog in ADR 0003 — Structure — done

E6 is in Proposed ADR 0003 Decision (shape + `product_outcome` + `answer_id` xor none). **RecallLog** is in ADR 0001 glossary. Status stays Proposed (human). GAP/SEED still list E6 until slice 14.

### 2. Just-review Yes leaves a GOOD RecallLog — Behavior — done

Table `recall_log` (`V300000263`), GET `/api/memory-trackers/{id}/recall-logs`, Memory Tracker section. Successful `markAsRecalled` writes `GOOD`. Unsuccessful already writes `AGAIN` (same writer; slice 3 asserts it). Controller test pins the GOOD shape once.

### 3. Just-review No leaves an AGAIN RecallLog — Behavior — done

Unsuccessful `markAsRecalled` already wrote `AGAIN`. Tests assert the second log only (controller, page, E2E). Do not re-assert the GOOD shape.

### 4. Prompt grade links the log to the answer — Behavior — done

Prompt grade callers pass the persisted `Answer` into `markAsRecalled`. MCQ pins GOOD/`AGAIN` + `answer_id`; spelling correct asserts the link. Just-review still null. Accidental-match may already get `answer_id` (slice 6). Overlap still does not call `markAsRecalled`.

### 5. Overlap does not write a RecallLog — Behavior — done

Overlap still does not call `markAsRecalled`. Controller test pins `OVERLAP` + no log. Schedule stays in the canonical overlap test.

### 6. Accidental-match primary logs AGAIN on the answer — Behavior — done

Spelling accidental match already wrote `AGAIN` + `answer_id` (slice 4 path). Controller test pins that delta.

### 7. Confusion logs CONFUSION on the matched tracker — Behavior — done

Matched tracker gets `CONFUSION` with the same `answer_id`. Stability adjustment unchanged. Dropped `answer.confusion_adjusted_memory_tracker_id` (`V300000264`). Tutoring status still reads `session_item` until slice 11.

### 8. Tutor Feedback writes a RecallLog per score — Behavior — done

Grade caller maps 4/5/3/2/1/0 → GOOD/EASY/HARD/SHRINK/AGAIN/AGAIN_ZERO and persists before `recalledAgain` (0 vs 1). No `answer_id`. `session_item` still written. E2E: one GOOD log after tutor 4.

### 9. Backfill prompt answers into RecallLog — Behavior — done

`V300000265` INSERT…SELECT: ordinary answers → GOOD/AGAIN with `answer_id`, `elapsed_hours` null. OVERLAP skipped. Idempotent on existing `recall_log.answer_id`.

### 10. Backfill Tutor scores into RecallLog — Behavior — done

`V300000266` INSERT…SELECT from scored `session_item` (same 0–5 map as live). No `answer_id`. `elapsed_hours` null. Idempotent against live tutor logs.

### 11. Tutoring status and frequent-failure read the log — Behavior — done

Tutoring status counts `answer_id` null tutor outcomes **on the commissioned tracker** (sibling understanding just-review does not count). Frequent-failure counts `AGAIN`/`AGAIN_ZERO` in 14 days. No source column. `session_item` still written.

### 12. Drop learning_session and session_item — Structure — planned

**Jidoka:** Draft ADR 0005 so Learning Session is the Request/Report activity, not a persisted aggregate (no paste-into-session, no amend-in-place). Human owns that Decision edit.

Remove tables, entities, and E2E “creates a session” assertions. Latest tutor score on the Memory Tracker comes from the latest tutor log. Existing tests still pass aside from those replaced assertions. `note_title` snapshot is not kept.

### 13. Drop redundant answer.correct and recall_count — Structure — planned

Prompt history “Correct/Incorrect” follows the linked log (or `OVERLAP`). `recall_count` is not stored; if the API still exposes a count, it is `count` of non-`CONFUSION` logs. Existing tests pass with the new source.

### 14. Point the gap tracker at remaining knobs only — Structure — planned

E6 off Deferred in ADR 0003. [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md) and SEED-004 list only B4 / E3 / E4 plus human accept. No spent slice diary left in this plan directory beyond resume status.

## Out of scope

- B4, E3, E4; accepting ADR 0003
- Relearning steps, first-rating `S0(G)`/`D0(G)`, fuzz
- Deleting `hoursFromLegacyIndex` (still required for `V300000260` replay)
