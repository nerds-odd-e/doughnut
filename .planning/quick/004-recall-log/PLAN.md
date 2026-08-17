# Plan: RecallLog

**Status:** in progress (slice 3 next)

**Goal:** Memory-state transitions are a RecallLog. Prompt submissions stay `answer`. Tutor Feedback is a log row, not a session bag.

## Design

- Vertical slice: lock E6 in ADR 0003 Decision, then one observable log behavior at a time. No unused columns for fitting.
- Write logs at the **grade caller** (just review, prompt grading, `recordFeedback`, confusion), not only inside `recalledAgain` — Tutor **0** and **1** both call `recalledAgain` but must log `AGAIN_ZERO` vs `AGAIN`.
- Hooking `markAsRecalled` will also log prompt Yes/No before `answer_id` is set. That is allowed interim; the next prompt slice sets `answer_id`. Overlap must still write **no** log (it does not call `markAsRecalled`).
- `GET /api/memory-trackers/{id}/recall-logs` is the Memory Tracker surface. After a DTO change, `pnpm generateTypeScript`.
- Next Flyway: `V300000264`. `recall_log.memory_tracker_id` ON DELETE CASCADE. Include logs in hard-delete fixtures (`unit-testing.mdc`).
- Jidoka before dropping `learning_session`: [ADR 0005](../../../docs/adrs/0005-commissioned-learning-session-protocol.md) still describes paste-into-session and amend-in-place; code already does not. Slice 12 drafts that ADR to match “session = Request/Report activity, not a table.”

## Slices

### 1. Lock RecallLog in ADR 0003 — Structure — done

E6 is in Proposed ADR 0003 Decision (shape + `product_outcome` + `answer_id` xor none). **RecallLog** is in ADR 0001 glossary. Status stays Proposed (human). GAP/SEED still list E6 until slice 14.

### 2. Just-review Yes leaves a GOOD RecallLog — Behavior — done

Table `recall_log` (`V300000263`), GET `/api/memory-trackers/{id}/recall-logs`, Memory Tracker section. Successful `markAsRecalled` writes `GOOD`. Unsuccessful already writes `AGAIN` (same writer; slice 3 asserts it). Controller test pins the GOOD shape once.

### 3. Just-review No leaves an AGAIN RecallLog — Behavior — planned

**Pre:** Tracker already has a GOOD log (slice 2).  
**Trigger:** Just review **No, I need more recall**.  
**Post:** Memory Tracker shows a second log `AGAIN`. Do not re-assert the GOOD row shape.

### 4. Prompt grade links the log to the answer — Behavior — planned

**Pre:** Recall prompt (MCQ or spelling) answered correctly or incorrectly.  
**Trigger:** Submit the answer.  
**Post:** The new RecallLog is `GOOD` or `AGAIN` and `answer_id` is that prompt’s answer. Unanswered prompts still have no log.

### 5. Overlap does not write a RecallLog — Behavior — planned

**Pre:** Spelling overlap with a declared overlap note.  
**Trigger:** Submit the overlapping answer.  
**Post:** Answer exists with outcome `OVERLAP`. Prompted tracker has no new RecallLog. Schedule fields unchanged (existing overlap tests stay the canonical schedule assertion).

### 6. Accidental-match primary logs AGAIN on the answer — Behavior — planned

**Pre:** Spelling accidental match (no overlap).  
**Trigger:** Submit the match.  
**Post:** Prompted tracker has an `AGAIN` log whose `answer_id` is the accidental-match answer.

### 7. Confusion logs CONFUSION on the matched tracker — Behavior — planned

**Pre:** Slice 6; matched note has an eligible tracker.  
**Trigger:** Same accidental-match submit.  
**Post:** Matched tracker has a `CONFUSION` log with the **same** `answer_id`. That tracker’s `recallCount` / `lastRecalledAt` still unchanged. Drop `answer.confusion_adjusted_memory_tracker_id` once this log is the attribution.

### 8. Tutor Feedback writes a RecallLog per score — Behavior — planned

**Pre:** Due commissioned trackers; learner records a Report.  
**Trigger:** Record scores.  
**Post:** Each matched score is a RecallLog on that tracker (`GOOD`/`EASY`/`HARD`/`SHRINK`/`AGAIN`/`AGAIN_ZERO` for 4/5/3/2/1/0), no `answer_id`. Request still creates no session. Keep `session_item` until slice 12 (interim).

E2E: extend `commissioned_learning_session.feature` (one scenario asserts a log; other scores as controller deltas). Tutoring status may still count `session_item` until slice 11.

### 9. Backfill prompt answers into RecallLog — Behavior — planned

**Pre:** Existing `answer` rows from before slice 4.  
**Trigger:** Flyway backfill.  
**Post:** Each ordinary prompt grade has a log (`GOOD`/`AGAIN`, `answer_id` set, `elapsed_hours` null if unknown). Overlap answers still have no log. Memory Tracker shows those logs.

### 10. Backfill Tutor scores into RecallLog — Behavior — planned

**Pre:** Existing `session_item` scores.  
**Trigger:** Flyway backfill.  
**Post:** Each scored item has the matching `product_outcome` log. No `answer_id`. `elapsed_hours` null if unknown.

### 11. Tutoring status and frequent-failure read the log — Behavior — planned

**Pre:** Logs from slices 8–10 (and live grades).  
**Trigger:** Open a Learning Session Request, or hit the frequent-failure threshold.  
**Post:** “N previous sessions, last on …” counts tutor logs (`GOOD`/`EASY`/`HARD`/`SHRINK`/`AGAIN`/`AGAIN_ZERO`) for that tracker. Frequent-failure counts `AGAIN`/`AGAIN_ZERO` in 14 days from the log (just-review No and Tutor 0/1 count; overlap and `CONFUSION` do not).

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
