# RecallLog (E6) — close the FSRS history gap

**Status:** in progress (slice 9 done; slice 10 next)  
**Seed:** [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)  
**Policy:** Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (lock E6 in Decision in slice 1)

## Goal

Persist an FSRS-shaped **RecallLog** (Doughnut name; review (FSRS) = recall) so each memory-state transition is replayable. Keep `answer` as the recall-prompt payload. Drop the leftover `learning_session` / `session_item` bag once tutor grades live on the log.

## Locked shape (end state)

`recall_log`: `memory_tracker_id`, `recorded_at`, `elapsed_hours` (nullable on backfill), `product_outcome`, optional `answer_id`.

Sources: prompt grade / confusion → `answer_id`; just review and Tutor Feedback → no payload FK. No `recall_prompt_id` (redundant with `answer → recall_prompt`).

Do not store FSRS G, Retrievability, `I`, or pre/post S/D. Current S/D stay on `memory_tracker`. `next_recall_at` stays as the due-work index.

`product_outcome`: `GOOD` | `EASY` | `HARD` | `SHRINK` | `AGAIN` | `AGAIN_ZERO` | `CONFUSION`.

Out of this plan: B4 lapses, E3 fuzz/max interval, E4 fitting, accepting ADR 0003 (human).
