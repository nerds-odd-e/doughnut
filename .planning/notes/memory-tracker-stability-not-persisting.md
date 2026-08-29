---
title: memory_tracker.stability may not be persisting across grades
date: 2026-08-29
context: .planning/quick/001-morning-cognitive-index/PLAN.md, slice 21.9
---

# `memory_tracker.stability` may not be persisting across grades

## The finding

While building the morning-cognitive-index plan's split-half reliability
diagnostic (slice 21.4, `GET /api/user/recall-split-half-reliability`),
querying it against real production data (via three rounds of temporary,
response-embedded debug counters, each deployed then reverted — production is
back to its pre-investigation state) showed that almost every review has a
null `retrievability`. On one account: `AccuracyStats.sampleSize = 4` against
`totals.reviewsToday = 61` — roughly 5% of today's reviews had a usable
retrievability. Across a 90-day window, 180 of 182 candidate half-day
scorings failed on zero *sample* rows (not a degenerate/zero-variance fit).

`MemoryTracker.retrievabilityAt(now)` returns null for exactly one reason:
`isNew()`, i.e. `stability <= Fsrs.NEW_STABILITY_HOURS` (`0.0f`, the field's
default). So the finding is really: as far as `memory_tracker.stability` is
concerned, most trackers on that account are perpetually "New" — never
observed to have received a prior grade — despite the account having 126,440
total reviews and a 200-day streak.

## What git history establishes

`RecallLog` and its memory-state snapshots were introduced in two separate,
recent steps:

| Date (Singapore time) | Commit | Change |
|---|---|---|
| 2026-08-17 14:23 | `0843cbd3ee` | Created `recall_log`, its entity/repository, and the first live writer. |
| 2026-08-17 15:48–16:14 | `6ddf35275e`, `59e3e4a282` | Backfilled ordinary answers and Tutor session history into `recall_log`. |
| 2026-08-19 | `d18c692a0e` | Reconstructed `elapsed_hours` and made it non-null. |
| 2026-08-20 | `9da86f5e29` | Canonicalized the stored outcome as an FSRS `Grade` or `CONFUSION`. |
| 2026-08-22 | `fc3f7d865f`, `724685c882` | Added `tutor_feedback`. |
| 2026-08-27 13:58 | `e24006ae39` | Added nullable `stability_before`, `difficulty_before`, and `retrievability`. |
| 2026-08-27 14:35 | `078e4895fa` | Started writing those fields on live reviews and fixed the Confusion path to snapshot state before mutation. |
| 2026-08-27 17:57 | `7a9929abe9` | Added the historical memory-state replay/backfill implementation. |
| 2026-08-28 21:01 | `e7a7879fc4` | Recorded the production finding that nearly every review's retrievability was null. |

The original log was only eleven days old, and the memory-state columns had
existed for only about one day, when the production gap was recorded. Git
history shows that the **schema** has been unchanged since 2026-08-27, but it
does not show that the **data** has stabilized: the historical replay is not
wired to run automatically, and the presence of its implementation does not
prove that it ran successfully in production.

## Why this might be bigger than a stats readout

`RecallLogMemoryStateBackfill` (built for a different purpose — backfilling
`recall_log.stability_before`/`difficulty_before`/`retrievability` on old
rows) replays history onto a **scratch, never-persisted** `MemoryTracker` and
never writes back to the live `memory_tracker.stability`/`difficulty`
columns. A fully complete `RecallLog` backfill therefore would not seed the
live tracker state. This *can* explain a live row having null retrievability
when it is that item's first genuine grade since the new writer was deployed:
the row snapshots the still-New state, then `applyGrade` should establish the
tracker. A later live grade for the same tracker should no longer be null.

The more consequential hypothesis is therefore narrower and still unproven:
**`memory_tracker.stability` remains New after at least one genuine live
grade**. Possible causes include an unseeded migration followed by too little
post-rollout history, an `applyGrade` persistence/transaction problem, a stale
read, or a later reset to `NEW_STABILITY_HOURS`. If a tracker remains New
after a live grade, this is not just starving a stats readout — it would feed
the live FSRS *scheduler* the wrong stability and mis-schedule reviews.

## Suggested first step

A plain count of trackers with `stability <= 0` and multiple `recall_log` rows
is not sufficient: those rows may all have been backfilled from old answers.
Use the production deployment/cutover time for commit `078e4895fa` and run a
direct read-only query (DB console, not the stats API) for trackers that:

1. currently have `stability <= 0`; and
2. have at least one graded `recall_log` backed by an answer genuinely created
   after that cutover.

Two or more post-cutover live grades for the same still-New tracker would be
stronger evidence of a persistence/reset bug. Inspect a small sample in
chronological order to distinguish three cases:

- only historical/backfilled rows lack memory state: accept limited history
  and let post-rollout data accumulate;
- the first live grade establishes stability: the live writer works, but
  existing trackers were not seeded;
- stability remains zero after live grades: investigate `applyGrade`
  persistence or a subsequent reset as a scheduler defect.

Only after identifying the case should a repair be designed. In particular,
do not assume that the scratch `RecallLogMemoryStateBackfill` was intended to
update live tracker state.

## Verdict (production query, 2026-08-28)

Read-only against Cloud SQL `doughnut-db-instance` / database `doughnut`.
Cutover used: first successful MIG boot that actually ran the writer jar,
not the git timestamp of `078e4895fa`.

**Live `applyGrade` persistence is fine.** Zero not-deleted trackers with
`stability <= 0` have a graded `recall_log` whose answer (or, if no answer,
`recorded_at`) is after cutover. Recent rows show a non-zero
`stability_before` and a higher `memory_tracker.stability` after the grade.

**The stats-null gap was missing columns, not perpetually-New trackers.**

| Fact | Value |
|---|---|
| `V300000303` (add memory-state columns) applied | 2026-08-28 09:56:43 UTC |
| Last `recall_log` with all three memory-state columns NULL | 2026-08-28 08:43:50 UTC |
| First row with `stability_before` set | 2026-08-28 10:06:35 UTC |
| Rows with memory state set vs NULL (before backfill) | 43 vs 126,997 |
| `RecallLogMemoryStateBackfill` | ran 2026-08-29 00:36–00:49 UTC from `doughnut-app-group-x250`: 14,041 trackers with gaps, **125,570 rows** filled. Remaining NULL rows are the elapsed-hours checksum fail-safe (plus New-tracker first grades, which correctly have null retrievability). |

Writer code (`078e4895fa` / `c5cd9af8`) merged 2026-08-27 ~07:09 UTC, but
production kept booting the old `doughnut-*.jar` until
`d24eff0871` / deploy of `581530c485` fixed the startup-script ARTIFACT
name to `donut` and rolled the MIG (~09:50–09:57 UTC on 2026-08-28).
Flyway then applied `V300000303`; live snapshots start in the next reviews.

That matches the original readout (`sampleSize = 4` vs `reviewsToday = 61`)
taken later the same Singapore day: almost every “today” review was still
a pre-column row.

**Still-New live trackers are a small leftover, not the review stream.**

| Slice | Count |
|---|---|
| All trackers | 111,439 |
| `stability <= 0` | 86,548 |
| of those, not deleted, `removed_from_tracking=1`, never recalled | 84,323 |
| Active (`deleted_at` null, not removed) and still New | **107** |
| of those 107 with any `recall_log` | **0** |
| of those 107 with `last_recalled_at` set | 54, every one `2026-08-16 00:29:52` (same instant as `V300000261` adding `difficulty` — not an organic review) |

The scheduler is not being fed New stability for items that have been
graded since the writer jar actually booted. No `applyGrade` code fix.

**Backfill run (2026-08-29):** one-off operator invocation of
`RecallLogMemoryStateBackfill` from `doughnut-app-group-x250` filled 125,570
historical `recall_log` rows (14,041 trackers). It does not write live
`memory_tracker.stability`. Temporary fat jar copies at
`gs://dough-01/tmp/donut-backfill.jar` and `/tmp/donut-backfill.jar` on that
VM were deleted after the run. The class is unchanged and still has no
wired runner.

## Why this is not being resolved inside `quick/001`

Confirming the hypothesis needs a production read-only query only the
developer can run, and — if confirmed — the fix is a live FSRS scheduling
bug affecting real spaced-repetition scheduling for most users, not a
sub-slice of a stats-display plan. `quick/001` closed out slice 21.9 with
this write-up and treated its reliability gate as failed (composite morning
index dropped, component readouts ship on their own) rather than waiting on
this. This note is the handoff point for a follow-up investigation, sized
and scoped on its own.
