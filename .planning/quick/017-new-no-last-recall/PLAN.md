# Plan: New has no last recall

**Status:** in progress

**Goal:** `lastRecalledAt` is the last mapped grade (FSRS `last_review`). A New tracker has no last recall; due now comes from `assimilatedAt`. Migrate existing rows. Leave **E4** fitting, `ForgettingCurve` rename, `DEFAULT_SPACES`, and the Difficulty-5 fallback out.

Accepted recommendations (2026-08-19): nullable `last_recalled_at`; New due = `assimilatedAt`; first-grade `RecallLog.elapsed_hours` = 0; `remove` does not write last recall; ungraded New / still-New-with-logs / remove-bumped repair as specified; lock no Learning/Relearning card states in ADR 0003.

## Locked for this plan

- ADR 0003 stays **Proposed**. Do not accept it here.
- First-rating `S0`/`D0` math is unchanged. Elapsed time still does not change first-rating Stability/Difficulty.
- Still-New outcome precedence stays: `AGAIN`/`AGAIN_ZERO` → Again first-rating; `SHRINK` → Hard first-rating; Hard must not overwrite Again-only New; first-success `S > 0` unrestored. **CONFUSION** is not a mapped grade.
- Do not edit committed `V300000271` / `V300000272` (remain `1=0`). New ungated migrations do the real work.
- Do not add FSRS Learning / Review / Relearning columns or step lists.
- Do not squash Flyway. Do not rename `ForgettingCurve` / `ASSIMILATE_STABILITY_HOURS`.

## Out of this plan

- **E4** fitting / per-user weights.
- `DEFAULT_SPACES` / `hoursFromLegacyIndex` (needed so `V300000260` can replay).
- Difficulty **5** fallback on old `S > 0` rows with null D.
- Calendar same-day short-term (elapsed whole hours **0** stays).
- `NotePropertyTrackingBackfill` (already-applied insert).

## Discoveries

- `findLast100RecalledByUser` already filters `last_recalled_at IS NOT NULL`. Memory Tracker already shows **N/A** when last recall is missing.
- `last_recalled_at` is DATETIME NULL (`V300000275`). Assimilate leaves it unset; due = `assimilatedAt`.
- `remove` still writes `lastRecalledAt = now` (slice 4).
- Graded builder helpers (`stabilityAndNextRecallAt`, `afterNthStrictRecall`) still set last recall when the fixture is not New.

## Slices

### 1. Lock New last-recall in ADR 0003

- **Type:** Structure
- **Status:** done

Locked in Proposed ADR 0003 Decision (status still Proposed). Gap tracker / SEED-004 point at this plan as live **New last recall**; **E4** deferred.

Learning: pointers use Doughnut **recall**, not FSRS “review”.

### 2. last_recalled_at can be unset

- **Type:** Structure
- **Status:** done

Flyway `V300000275` makes `last_recalled_at` DATETIME NULL. Unset last recall: elapsed hours **0**, due = `assimilatedAt`. Assimilate still writes last recall. Due math lives in package-private `MemoryTrackerRecallDue` so `MemoryTracker` stays under 250 lines.

### 3. Assimilating leaves Last Recall Time N/A

- **Type:** Behavior
- **Status:** done

Assimilate leaves last recall unset; due via `calculateNextRecallAt()` (= assimilated). First just-review Yes a day later: `RecallLog.elapsed_hours` **0**. E2E: Last Recall Time **N/A**, Next = Assimilated, S 0, D **N/A**. Builder `assimilatedAt` does not copy last recall; graded helpers still set it. Dropped unused `updateStability`.

### 4. Remove from recall does not change Last Recall Time

- **Type:** Behavior
- **Status:** planned

**Pre:** a graded tracker with last recall T (just-review Yes, then open Memory Tracker). **Trigger:** Remove from recall. **Post:** tracker is skipped; Last Recall Time still T.

Replace `MemoryTrackerTrackingControllerTest.removeAndUpdateLastRecalledAt`. Extend `spaced_repetition.feature` (or the existing visit-tracker path) so Remove on the Memory Tracker page leaves Last Recall Time unchanged. Revive still does not write last recall.

### 5. Existing ungraded New have no last recall

- **Type:** Behavior
- **Status:** planned

**Pre:** persisted New (`S = 0`, Difficulty null, no mapped RecallLog: not `GOOD`/`EASY`/`HARD`/`SHRINK`/`AGAIN`/`AGAIN_ZERO`). **Trigger:** apply the new ungated Flyway. **Post:** `last_recalled_at` is null; `next_recall_at` stays `assimilated_at`. Includes removed New with no mapped logs.

Still-New rows **with** mapped logs stay for the next slice (do not null them). CONFUSION-only stays New (null last recall).

Test the Java/SQL backfill like `StillNewFirstRatingBackfillTest` / `OverCapStabilityBackfillTest`. No E2E.

### 6. Still-New mapped logs first-rate from the grade time

- **Type:** Behavior
- **Status:** planned

**Pre:** `S = 0`, Difficulty null, at least one mapped RecallLog. **Trigger:** apply the new ungated Flyway (do not edit 271/272). **Post:** first-rating S/D per locked outcome precedence; `lastRecalledAt` = latest mapped `recall_log.recorded_at`; due = that time + `I`. Snapshot, not a history replay. Already `S > 0` unchanged (including if 271/272 ever ran).

Canonical Again-only and Shrink-only pins; Hard still skips Again-only. If both Again and Shrink logs exist, keep today’s order (Again first-rating wins because Hard skips already-migrated).

After this slice, STATE must not ask operators to enable 271/272. Keep those files for Flyway replay.

### 7. Removed graded last recall is the last mapped grade

- **Type:** Behavior
- **Status:** planned

**Pre:** `removed_from_tracking` is true, at least one mapped RecallLog, `last_recalled_at` is not that log’s latest `recorded_at` (remove-bump). **Trigger:** apply the new ungated Flyway. **Post:** `lastRecalledAt` = latest mapped log time; **due unchanged**.

Test the backfill at the same JDBC boundary as slice 5–6. Wrap-up: update FSRS gap tracker / SEED-004 (New last recall closed; **E4** still deferred); drop spent 271/272 operator steps from STATE.
