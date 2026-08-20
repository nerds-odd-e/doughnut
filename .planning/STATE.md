---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-20T00:51:00Z"
last_activity: 2026-08-20
last_activity_desc: "Dropped spent Java Flyway confirms from project state"
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

Difficulty is shown on the Memory Tracker Information card (API number, or **N/A** when unset). After a mapped grade, Difficulty follows published FSRS-6 next Difficulty (ADR 0003 Decision). Scheduling policy lives in Proposed [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision. RecallLog `elapsed_hours` is required (ADR 0003 RecallLog). Maximum interval is locked (36500 days / 876000 hours); over-cap Stability is clamped. Thinking time is recorded on answers for display and stats; it is not a memory-state input. `lastRecalledAt` is the last mapped grade (unset on New; New due is `assimilatedAt`). `last_recalled_at` / `next_recall_at` are DATETIME so last+876000 can persist; remaining TIMESTAMP columns are [SEED-006](seeds/SEED-006-remove-mysql-timestamp-2038.md).

**Flyway:** every applied migration is squashed into `V100000000__baseline.sql`; `V300000300__db_migration_placeholder.sql` is the tip. New migrations use a greater version.

**Recently shipped:** RecallLog DSR snapshot rebuild (see ADR 0003 **DSR snapshot**; live grading writes the snapshot). FSRS post-lapse cap (elapsed **≥ 24**, S > 0: `S' = min(current S, max(1, round(Sf)))` in `FsrsAgainRecall.hoursAfterPostLapse`; 5h / D=1 / elapsed 8760 stays **5**; short-term After-Again unchanged). FSRS After-Again leftover cohesion (duplicate same-hour After-Again unit pin dropped; window pins with other G; elapsed **24** on 72h / D=5 is post-lapse **15**, on-time **17**; one `< 24` short-term path for all four G). FSRS short-term leftover cohesion (failure-then-Good elapsed pin is long-term **24 vs 48**; leftover SpacedRepetition scheduling tests are MemoryTracker recall-scheduling; public **New** is `MemoryTracker.isNew()`). FSRS-6 short-term next Stability for elapsed whole hours **< 24** (all four G; **≥ 24** long-term, Again = post-lapse; New → Again **5h** → Good at 5h → **6h**). Live DSR updates live on `Fsrs` (no `ForgettingCurve`). RecallLog `elapsed_hours` is required (NOT NULL). Alias RecallLog grades are normalized at write time. Removed graded last-recall uses latest mapped grade (due / S / D unchanged). Still-New mapped first-rating is closed. Dead wiki-link retarget rewrites every matching path-Markdown (and wiki) token; path-Markdown WikiTitle detection shares the frontend concept-path helper. Frontmatter dual-spelling (YAML `source`/`target`/`overlaps` wiki default; path Markdown accepted as the same link; reduce-on-delete and editor flush; no conversion). FSRS maximum interval (36500 days / 876000 hours; clamp after next-S; due from that S; over-cap Stability clamped live; recall due columns DATETIME in `V300000273`). First-rating leftover cohesion (duplicate first-Again pins dropped; Hard first-rating skips Again-only New; Hard on New first-rates at the Grade map; `MemoryTracker.isNew()` is ADR 0001 **New**). First-rating leftover E2E now goes through real New `S0`/`D0` (same-hour Good stays **55**; incorrect just-review after first Good is Again **15** / **7.3945026**; graded-tracker seed gone; commissioned first-rating is one outline). First mapped success on New uses FSRS-6 `S0`/`D0` (Good **55h** / **2.118104**, Hard **31h** / **5.1121707**, Easy **199h** / **1**; 24h due fallback is not first-success Stability). No lapse count in Proposed ADR 0003 memory state (B4 closed; Again history is RecallLog; frequent-failure warning unchanged). Stored note markdown carries `type: Note` / `type: Relationship` (persist via `NoteConceptType.ensureStoredType`; OKF **C1**/**D2** closed; Wikidata prepend and extract remainder keep the leading fence; leftover persist-test overlap dropped). RecallLog leftover cohesion (reloaded prompt-history Correct/Incorrect pinned; one canonical pin per RecallLog writer; stats tests on `RecallAnswerRow`). Same-hour FSRS-6 short-term next Stability (elapsed 0, S > 0; Good 24→25, Easy 24→43, Hard 24 stays 24; After-Again after first Good **18**; 72h **24**). Published FSRS-6 next Difficulty after a mapped grade (linear damping + mean reversion toward unclamped D0(Easy); existing D not backfilled). Difficulty on the Memory Tracker page (Information card; N/A when unset). FSRS-6 Good next Stability and Difficulty on ordinary correct recall (E2E day lists follow FSRS). Ordinary incorrect at elapsed **≥ 24** uses FSRS-6 post-lapse Stability and Again Difficulty (due from `I(0.9, S)`; New Again is first-rating **5h**; 24h is only non-positive `I`). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md). Requested-retention leftover cohesion (redundant fail-due pins dropped; grade due and confusion projection live on `MemoryTracker`).

**Remaining FSRS gap:** First-rating (all four G, Hard on New) is closed; `w[0]` is used. Maximum interval is closed. Interval fuzz is closed (due follows S). Thinking-time overlay is closed (RT is not a DSR input). New last recall is closed (`lastRecalledAt` is last mapped grade). RecallLog elapsed hours is closed (`elapsed_hours` required; [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy.md) RecallLog). Short-term window is closed (elapsed whole hours **< 24** all four G; **≥ 24** long-term, Again = post-lapse). Short-term leftover cohesion is closed (long-term failure-then-Good pin; conversion not a live service; **New** on the memory tracker). After-Again leftover cohesion is closed (window pins with other G; elapsed **24** → **15**, not short-term **24**). Post-lapse cap is closed (elapsed **≥ 24**, a fail cannot lengthen S). DSR snapshot is closed (see ADR 0003 **DSR snapshot**). Thinking-time leftover cohesion (duplicate on-time first-Good pin dropped; max-interval Good lives with other correct-recall pins; HTTP equality covers S, D, and due). Commissioned Feedback uses Grades **1–4** (= FSRS G). Remaining deferred knob (**E4** fitting) plus **accept ADR 0003** (human). Lapses are not memory state. Just review is locked two buttons (GOOD / AGAIN). Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied
- Next FSRS: humans accept Proposed ADR 0003, or pick remaining deferred **E4** fitting ([FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md)). Thinking-time overlay is closed.
