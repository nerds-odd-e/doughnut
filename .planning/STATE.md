---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-18T15:30:00Z"
last_activity: 2026-08-18
last_activity_desc: "Closed first-rating leftover cohesion (Hard on outcome map; isNew)"
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Difficulty is shown on the Memory Tracker Information card (API number, or **N/A** when unset). After a mapped grade, Difficulty follows published FSRS-6 next Difficulty (ADR 0003 Decision). Scheduling policy lives in Proposed [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision.

**Ops leftover:** gated Flyway conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
- `still_new_again_first_rating_backfill` (`V300000271`)
- `still_new_hard_first_rating_backfill` (`V300000272`)

**Recently shipped:** First-rating leftover cohesion (duplicate first-Again pins dropped; Hard backfill skips Again-only New; Tutor **2** on New first-rates at the outcome map; `ForgettingCurve.isNew()` is ADR 0001 **New**). First-rating leftover E2E now goes through real New `S0`/`D0` (same-hour Good stays **55**; incorrect just-review after first Good is Again **15** / **7.3945026**; graded-tracker seed gone; commissioned first 3/4/5 is one outline). First mapped success on New uses FSRS-6 `S0`/`D0` (Good **55h** / **2.118104**, Hard **31h** / **5.1121707**, Easy **199h** / **1**; 24h due fallback is not first-success Stability). No lapse count in Proposed ADR 0003 memory state (B4 closed; Again history is RecallLog; frequent-failure warning unchanged). Stored note markdown carries `type: Note` / `type: Relationship` (persist + production backfill `V300000270`; OKF **C1**/**D2** closed; Wikidata prepend and extract remainder keep the leading fence; leftover persist-test overlap dropped). RecallLog leftover cohesion (reloaded prompt-history Correct/Incorrect pinned; one canonical pin per RecallLog writer; stats tests on `RecallAnswerRow`). Same-hour FSRS-6 short-term success next Stability (elapsed 0, S > 0; Good 24→25, Easy 24→43, Hard 24 stays 24; Again stays post-lapse). Published FSRS-6 next Difficulty after a mapped grade (linear damping + mean reversion toward unclamped D0(Easy); existing D not backfilled). Difficulty on the Memory Tracker page (Information card; N/A when unset). FSRS-6 Good next Stability and Difficulty on ordinary correct recall (E2E day lists follow FSRS). Ordinary incorrect uses FSRS-6 post-lapse Stability and Again Difficulty (due from `I(0.9, S)`; New Again is first-rating **5h**; 24h is only non-positive `I`). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md). Requested-retention leftover cohesion (redundant fail-due pins dropped; grade due and confusion projection live on `MemoryTracker`).

**Remaining FSRS gap:** First-rating (all four G, Tutor **2** on New as Hard) is closed; `w[0]` is used. Remaining deferred knobs (**E3** / **E4**) plus **accept ADR 0003** (human). Lapses are not memory state. Just review is locked two buttons (Tutor **4** / **1**). Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`), `V300000258` (table `answer`), `V300000259` (rename `stability`), `V300000260` (hours conversion + drop `space_intervals`), `V300000261` (difficulty column + graded backfill), `V300000262` (leftover graded-row difficulty backfill), `V300000263` (`recall_log`), `V300000264` (drop confusion FK), `V300000265`–`V300000266` (RecallLog backfills), `V300000267` (drop `session_item` / `learning_session`), and `V300000268` (drop `answer.correct` / `recall_count`)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Enable `still_new_again_first_rating_backfill=1=1` on the deploy that first applies `V300000271`, then revert to `1=0`
- Enable `still_new_hard_first_rating_backfill=1=1` on the deploy that first applies `V300000272`, then revert to `1=0`
- Next FSRS: humans accept Proposed ADR 0003, or pick a remaining deferred knob (**E3** / **E4**) ([FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md)).
