---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-19T12:00:00Z"
last_activity: 2026-08-19
last_activity_desc: "FSRS-6 short-term success under 24 hours"
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

**Ops leftover:** gated Flyway conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)

**Recently shipped:** FSRS-6 short-term success for elapsed whole hours **< 24** (Hard/Good/Easy; **≥ 24** long-term; New → Again **5h** → Good at 5h → **6h**). Live DSR updates live on `Fsrs` (no `ForgettingCurve`). Legacy index conversion lives only on `db.migration.StabilityIndexToHoursBackfill` for `V300000260` replay. RecallLog `elapsed_hours` is required (ungated `V300000281` reconstruction; ungated `V300000282` NOT NULL). Alias RecallLog grades are ungated `V300000279`. Removed graded last-recall repair is ungated `V300000278` (last recall = latest mapped grade; due / S / D unchanged). Still-New mapped first-rating is ungated `V300000277` (gated `V300000271` / `V300000272` stay `1=0` for Flyway replay). Dead wiki-link retarget rewrites every matching path-Markdown (and wiki) token; path-Markdown WikiTitle detection shares the frontend concept-path helper. Frontmatter dual-spelling (YAML `source`/`target`/`overlaps` wiki default; path Markdown accepted as the same link; reduce-on-delete and editor flush; no conversion). FSRS maximum interval (36500 days / 876000 hours; clamp after next-S; due from that S; over-cap rows clamped by ungated `V300000274`; recall due columns DATETIME in `V300000273`). First-rating leftover cohesion (duplicate first-Again pins dropped; Hard backfill skips Again-only New; Tutor **2** on New first-rates at the outcome map; `Fsrs.isNew()` is ADR 0001 **New**). First-rating leftover E2E now goes through real New `S0`/`D0` (same-hour Good stays **55**; incorrect just-review after first Good is Again **15** / **7.3945026**; graded-tracker seed gone; commissioned first-rating is one outline). First mapped success on New uses FSRS-6 `S0`/`D0` (Good **55h** / **2.118104**, Hard **31h** / **5.1121707**, Easy **199h** / **1**; 24h due fallback is not first-success Stability). No lapse count in Proposed ADR 0003 memory state (B4 closed; Again history is RecallLog; frequent-failure warning unchanged). Stored note markdown carries `type: Note` / `type: Relationship` (persist + production backfill `V300000270`; OKF **C1**/**D2** closed; Wikidata prepend and extract remainder keep the leading fence; leftover persist-test overlap dropped). RecallLog leftover cohesion (reloaded prompt-history Correct/Incorrect pinned; one canonical pin per RecallLog writer; stats tests on `RecallAnswerRow`). Same-hour FSRS-6 short-term success next Stability (elapsed 0, S > 0; Good 24→25, Easy 24→43, Hard 24 stays 24; Again stays post-lapse). Published FSRS-6 next Difficulty after a mapped grade (linear damping + mean reversion toward unclamped D0(Easy); existing D not backfilled). Difficulty on the Memory Tracker page (Information card; N/A when unset). FSRS-6 Good next Stability and Difficulty on ordinary correct recall (E2E day lists follow FSRS). Ordinary incorrect uses FSRS-6 post-lapse Stability and Again Difficulty (due from `I(0.9, S)`; New Again is first-rating **5h**; 24h is only non-positive `I`). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md). Requested-retention leftover cohesion (redundant fail-due pins dropped; grade due and confusion projection live on `MemoryTracker`).

**Remaining FSRS gap:** First-rating (all four G, Tutor **2** on New as Hard) is closed; `w[0]` is used. Maximum interval is closed. Interval fuzz is closed (due follows S). Thinking-time overlay is closed (RT is not a DSR input). New last recall is closed (`lastRecalledAt` is last mapped grade). RecallLog elapsed hours is closed (`elapsed_hours` required; [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy.md) RecallLog). Short-term success window is closed (elapsed whole hours **< 24** Hard/Good/Easy; **≥ 24** long-term). Thinking-time leftover cohesion (duplicate on-time first-Good pin dropped; max-interval Good lives with other correct-recall pins; HTTP equality covers S, D, and due). Commissioned scores are **1–4** (`score = G`). Remaining deferred knob (**E4** fitting) plus **accept ADR 0003** (human). Lapses are not memory state. Just review is locked two buttons (Tutor **3** / **1**). Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`), `V300000258` (table `answer`), `V300000259` (rename `stability`), `V300000260` (hours conversion + drop `space_intervals`), `V300000261` (difficulty column + graded backfill), `V300000262` (leftover graded-row difficulty backfill), `V300000263` (`recall_log`), `V300000264` (drop confusion FK), `V300000265`–`V300000266` (RecallLog backfills), `V300000267` (drop `session_item` / `learning_session`), `V300000268` (drop `answer.correct` / `recall_count`), `V300000273` (recall due DATETIME), `V300000274` (over-cap Stability clamp), `V300000275` (nullable `last_recalled_at`), `V300000276` (ungraded New last recall), `V300000277` (still-New mapped first-rating), `V300000278` (removed graded last recall), `V300000279` (alias RecallLog grades), `V300000281` (elapsed reconstruction), and `V300000282` (`elapsed_hours` NOT NULL)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Next FSRS: humans accept Proposed ADR 0003, or pick remaining deferred **E4** fitting ([FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md)). Thinking-time overlay is closed.
