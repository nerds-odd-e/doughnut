# Remove spent Java Flyway backfills

**Status:** planned  
**Goal:** Delete Java migrations that are DML-only (or empty-DB-safe), their gates, and code/tests/docs that existed only to run them. Empty migrate still reaches the current schema. Product behavior unchanged.

## Assumptions

- Every long-lived database is already at the current Flyway tip **or has no rows**. Startup still `repair()` then `migrate()` (prod and test).
- **Keep `V300000260`** (drops `user.space_intervals`, sets `stability` default `0`). Baseline still has the old column/default; no later SQL repeats this.
- **Keep `V300000283` / `RecallLogDsrBackfill`** (in-tree DSR rebuild; `.planning/quick/028-rebuild-dsr-from-recall-log/`).
- Do **not** edit committed SQL (including `V300000266` comments that still mention `SHRINK` / `AGAIN_ZERO`).
- Do **not** squash the SQL chain or rewrite `V100000000__baseline.sql`.

## Keep (live, not migration-only)

- `NoteConceptType.ensureStoredType` — note create/update
- `DisplayName` / `DisplayNamePathSeparators` / `WikiLinkMarkdownRewrite` OS-invalid helpers — live save and AI paths
- `ProductOutcome.mappedGradeSqlInList` — `RecallLogDsrBackfill` + `ProductOutcomeTest`
- `StabilityIndexToHoursBackfill` — only for `V300000260`

## Out of scope

- Squash / baseline dump
- Removing SQL DML (`V233`–`V235` tz, dummy-skip convert, etc.)
- Changing live display-name or concept-type writers

## Slices

Each slice is **Structure**: delete one backfill family; remaining tests pass; empty Flyway still migrates. No E2E (no user-visible change). Verify with targeted backend tests that boot the test DB (the remaining `*BackfillTest` plus `RecallLogDsrBackfillTest` / `StabilityIndexToHoursBackfillTest` as appropriate).

### 1. Drop display-name whitespace backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000244` gone. `DisplayNameSurroundingWhitespaceBackfill` + its test gone. `DisplayNameUniqueKeyJdbcConversion` still used by V280.

### 2. Drop note concept-type backfill and gate

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000269` and `V300000270` gone (same runner). `NoteConceptTypeBackfill` gone. `note_concept_type_backfill` removed from all `application.yml` documents and `application-prod.yml`. `NoteConceptType.ensureStoredType` unchanged.

### 3. Drop gated still-New first-rating backfills

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000271` / `V300000272` gone. `StillNewAgainFirstRatingBackfill`, `StillNewFirstRatingBackfill`, and `StillNewFirstRatingBackfillTest` gone. Placeholders `still_new_again_first_rating_backfill` and `still_new_hard_first_rating_backfill` removed from all Flyway placeholder maps.

### 4. Drop over-cap Stability backfill and retarget citations

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000274`, `OverCapStabilityBackfill`, and `OverCapStabilityBackfillTest` gone. Live over-cap clamp on `Fsrs` / recall scheduling tests unchanged. ADR 0003 DSR snapshot cites `RecallLogDsrBackfill` (not the deleted classes). `.planning/quick/028-rebuild-dsr-from-recall-log/PLAN.md` locked decision matches.

### 5. Drop ungraded-New last-recall backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000276`, `UngradedNewLastRecallBackfill`, and its test gone.

### 6. Drop still-New mapped first-rating backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000277`, `StillNewMappedFirstRatingBackfill`, and its test gone. `SHRINK` / `AGAIN_ZERO` literals in Java drop with this class (remaining Java alias rewrite is slice 8).

### 7. Drop removed-graded last-recall backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000278`, `RemovedGradedLastRecallBackfill`, and its test gone.

### 8. Drop alias RecallLog grade rewrite

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000279`, `AliasRecallLogGradeBackfill`, and its test gone. No remaining Java production code mentions `SHRINK` / `AGAIN_ZERO`. `ProductOutcome` enum unchanged (`GOOD`/`EASY`/`HARD`/`AGAIN`/`CONFUSION`).

### 9. Drop OS-invalid display-name backfill

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000280`, `DisplayNameOsInvalidCharsBackfill`, its test, and `DisplayNameUniqueKeyJdbcConversion` gone (last caller). Live `normalizeDisplayName` / wiki-token rewrite unchanged.

### 10. Drop null elapsed-hours reconstruction

- **Type:** Structure
- **Status:** planned
- **Done:** `V300000281`, `RecallLogElapsedHoursBackfill`, and its test gone. `V300000282` (`elapsed_hours NOT NULL`) stays. Empty `recall_log` still accepts that `ALTER`.

### 11. Drop spent Flyway version mentions in project state

- **Type:** Structure
- **Status:** planned
- **Done:** `.planning/STATE.md` Operator Next Steps no longer asks to confirm versions whose Java files were deleted. Recently-shipped / leftover copy no longer treats those Java backfills as living chain steps. Remaining confirms: SQL through `V282` plus **keep** `V260` / `V283` as applicable. No product-doc rewrite of immutable SQL comments.

## Jidoka

- Stop after any slice; each family gone is the value.
- If a remaining test still imports a deleted runner, that slice is not done — fix in the same slice, do not leave a compile hole.
- If empty-DB migrate fails after a delete, revert that slice; the usual cause is having deleted **V260** or **V282** by mistake.
- Do not run in parallel with 028 on ADR 0003 except slice 4’s citation retarget.
