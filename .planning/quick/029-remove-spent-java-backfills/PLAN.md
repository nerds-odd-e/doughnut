# Remove spent Java Flyway backfills

**Status:** in-progress  
**Goal:** Delete Java migrations that are DML-only (or empty-DB-safe), their gates, and code/tests/docs that existed only to run them. Empty migrate still reaches the current schema. Product behavior unchanged.

## Assumptions

- Every long-lived database is already at the current Flyway tip **or has no rows**, including **`V300000283` applied successfully**. Startup still `repair()` then `migrate()` (prod and test).
- **Keep `V300000260`** (drops `user.space_intervals`, sets `stability` default `0`). Baseline still has the old column/default; no later SQL repeats this.
- **Keep `V300000282`** (`elapsed_hours NOT NULL`). Empty `recall_log` still accepts that `ALTER`.
- Do **not** edit committed SQL (including `V300000266` comments that still mention `SHRINK` / `AGAIN_ZERO`).
- Do **not** squash the SQL chain or rewrite `V100000000__baseline.sql`.

`V300000283` is DML-only (`UPDATE memory_tracker` from `recall_log`). Nothing later depends on it. Live grading still writes the snapshot. Same removal rule as the other spent Java backfills.

## Keep (live, not migration-only)

- `NoteConceptType.ensureStoredType` — note create/update
- `DisplayName` / `DisplayNamePathSeparators` / `WikiLinkMarkdownRewrite` OS-invalid helpers — live save and AI paths
- `Fsrs` / `MemoryTracker` live DSR updates
- `StabilityIndexToHoursBackfill` — only for `V300000260`

## Out of scope

- Squash / baseline dump
- Removing SQL DML (`V233`–`V235` tz, dummy-skip convert, etc.)
- Changing live display-name, concept-type, or recall-scheduling writers

## Slices

Each slice is **Structure**: delete one backfill family; remaining tests pass; empty Flyway still migrates. No E2E (no user-visible change). Verify with targeted backend tests that boot the test DB (`StabilityIndexToHoursBackfillTest` plus any remaining `*BackfillTest`).

### 1. Drop display-name whitespace backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000244` gone. `DisplayNameSurroundingWhitespaceBackfill` + its test gone. `DisplayNameUniqueKeyJdbcConversion` still used by V280.
- **Learnings:** No stray product imports; conversion helper correctly deferred to slice 9.

### 2. Drop note concept-type backfill and gate

- **Type:** Structure
- **Status:** done
- **Done:** `V300000269` and `V300000270` gone (same runner). `NoteConceptTypeBackfill` gone. `note_concept_type_backfill` removed from all `application.yml` documents and `application-prod.yml`. `NoteConceptType.ensureStoredType` unchanged.
- **Learnings:** Dual gate migrations share one runner; clear placeholder from every Flyway map (yml ×3 + prod).

### 3. Drop gated still-New first-rating backfills

- **Type:** Structure
- **Status:** done
- **Done:** `V300000271` / `V300000272` gone. `StillNewAgainFirstRatingBackfill`, `StillNewFirstRatingBackfill`, and `StillNewFirstRatingBackfillTest` gone. Placeholders `still_new_again_first_rating_backfill` and `still_new_hard_first_rating_backfill` removed from all Flyway placeholder maps.
- **Learnings:** `StillNewMappedFirstRatingBackfill` correctly deferred to slice 6.

### 4. Drop over-cap Stability backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000274`, `OverCapStabilityBackfill`, and `OverCapStabilityBackfillTest` gone. Live over-cap clamp on `Fsrs` / recall scheduling tests unchanged. ADR 0003 no longer names those deleted classes (keep the one-time Flyway *policy* wording until slice 11).
- **Learnings:** Ungated DML-only family — no yml placeholder cleanup. STATE.md V274 mention deferred to slice 12.

### 5. Drop ungraded-New last-recall backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000276`, `UngradedNewLastRecallBackfill`, and its test gone.

### 6. Drop still-New mapped first-rating backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000277`, `StillNewMappedFirstRatingBackfill`, and its test gone. `SHRINK` / `AGAIN_ZERO` literals in Java drop with this class (remaining Java alias rewrite is slice 8).
- **Learnings:** Remaining Java `SHRINK`/`AGAIN_ZERO` on RemovedGraded (slice 7) and Alias (slice 8). ADR class cite deferred to slice 11.

### 7. Drop removed-graded last-recall backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000278`, `RemovedGradedLastRecallBackfill`, and its test gone.

### 8. Drop alias RecallLog grade rewrite

- **Type:** Structure
- **Status:** done
- **Done:** `V300000279`, `AliasRecallLogGradeBackfill`, and its test gone. No remaining Java production code mentions `SHRINK` / `AGAIN_ZERO`. `ProductOutcome` enum unchanged (`GOOD`/`EASY`/`HARD`/`AGAIN`/`CONFUSION`).

### 9. Drop OS-invalid display-name backfill

- **Type:** Structure
- **Status:** done
- **Done:** `V300000280`, `DisplayNameOsInvalidCharsBackfill`, its test, and `DisplayNameUniqueKeyJdbcConversion` gone (last caller). Live `normalizeDisplayName` / wiki-token rewrite unchanged.

### 10. Drop null elapsed-hours reconstruction

- **Type:** Structure
- **Status:** done
- **Done:** `V300000281`, `RecallLogElapsedHoursBackfill`, and its test gone. `V300000282` stays.

### 11. Drop RecallLog DSR rebuild backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000283`, `RecallLogDsrBackfill`, and `RecallLogDsrBackfillTest` gone. `ProductOutcome.mappedGradeSqlInList` / `isMappedGrade` gone if no remaining callers; `ProductOutcomeTest` gone if it only pinned that SQL list. Live `Fsrs` grading unchanged.
- **Docs in this slice** (the named runner is gone): ADR 0003 **DSR snapshot** keeps the cache-of-fold / live-update / no-query-time-fold policy; one-time Flyway is **past** (applied, not in the chain) and does not cite deleted classes. [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md) and [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md) drop `RecallLogDsrBackfill` / `V300000283` code pointers.

### 12. Drop spent Flyway version mentions in project state

- **Type:** Structure
- **Status:** planned
- **Done:** `.planning/STATE.md` Operator Next Steps no longer asks to confirm versions whose Java files were deleted (including `V283`). Recently-shipped copy no longer treats those Java backfills as living chain steps. Remaining confirms: SQL through `V282` plus **keep `V260`**. If `.planning/quick/028-rebuild-dsr-from-recall-log/` is still on disk, prune it as spent (outcomes live in ADR 0003). No product-doc rewrite of immutable SQL comments.

## Jidoka

- Stop after any slice; each family gone is the value.
- If a remaining test still imports a deleted runner, that slice is not done — fix in the same slice, do not leave a compile hole.
- If empty-DB migrate fails after a delete, revert that slice; the usual cause is having deleted **V260** or **V282** by mistake.
- Do not re-introduce a DSR Java migration after slice 11; leftover rows are assumed already folded.
