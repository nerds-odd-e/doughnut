# DB timezone fix — pin JDBC session to UTC + repair skewed window

## Root cause (investigated 2026-07-24)

- Production JVM moved from `TZ=Asia/Singapore` to `TZ=UTC` on 2026-06-20
  (`a3777337da`), but the production MySQL server session still runs at +8.
- The datasource uses Connector/J 9.7 defaults (`connectionTimeZone=LOCAL`,
  `preserveInstants=true`), which **assume** session tz == JVM tz without checking.
- Consequences:
  - **Reads:** every `TIMESTAMP` is parsed +8h ahead → stats show 7:00 activity
    as 15:00.
  - **Writes since 2026-06-20:** epochs stored **8h too early** (display correct
    today because the error cancels on read; wrong at rest).
  - Native-SQL time comparisons and recall scheduling (`next_recall_at`) are
    skewed in the same window.
- All time columns are MySQL `TIMESTAMP` (no `DATETIME`), so pinning the session
  tz has no wall-clock storage side effects.
- Repair must be **production-only** (manual runbook, NOT Flyway): local/e2e rows
  in the same date window are correctly stored and would be corrupted by a
  blanket migration.

## Phases

### Phase 1 — Behavior: pin DB session time zone to UTC — planned

Fixes hour-of-day display for all correctly-stored data (~11 months of history)
and stops further skewed writes. Safe in every environment (no-op where tz
already consistent).

- Test first (red): backend DB test (capability-named, e.g.
  `DatabaseTimeZoneTest`) asserting via the app datasource:
  1. `SELECT @@session.time_zone` returns `'+00:00'` (currently `SYSTEM` → red
     everywhere), and
  2. instant round-trip: `SELECT FROM_UNIXTIME(1750000000)` read as
     `java.sql.Timestamp` has millis `1750000000000`.
- Change: append `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`
  to every JDBC URL:
  - `backend/src/main/resources/application.yml` (all profiles with a datasource)
  - `backend/src/main/resources/db-dev.properties`, `db-e2e.properties`,
    `db-test.properties`
  - `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh`
    (`-Dspring.datasource.url=...` overrides application.yml in prod)
  - check `scripts/` for any other constructed JDBC URLs
- Note: after prod rollout, rows written 2026-06-20 → rollout display −8h
  (23:00) until Phase 2 repairs them. Documented trade-off; the year of history
  becomes correct immediately.
- Wrap-up: targeted backend test + one recall-related E2E spec; commit + push;
  prod rollout is via MIG startup-script update + rolling replace (runbook note).

### Phase 2 — Behavior: repair skewed `quiz_answer.created_at` window — planned

Stats become fully correct (recent month included). Manual prod runbook with
Jidoka stops (credentials + approval before UPDATE):

1. Verify: `SELECT @@session.time_zone, @@system_time_zone, NOW(), UTC_TIMESTAMP();`
   — confirm effective +8 pre-fix assumption held.
2. Bound the window by **id**, not by the (skewed) `created_at`: find the first
   `quiz_answer.id` after the 2026-06-20 rollout and the last id before the
   Phase 1 rollout.
3. Count + snapshot affected rows; then
   `UPDATE quiz_answer SET created_at = created_at + INTERVAL 8 HOUR WHERE id BETWEEN ... ;`
   (same for `updated_at` only if app-written; `ON UPDATE CURRENT_TIMESTAMP` is
   server-side and already correct).
4. Verify in the stats UI: activity peak at ~07:00 across the whole year;
   "reviews today" consistent.

### Phase 3 — Behavior: repair `memory_tracker` scheduling columns — planned

Recalls due at correct times for trackers touched in the window
(`next_recall_at`, `last_recalled_at`, `assimilated_at` stored 8h early →
recalls surface 8h early). Runbook:

1. Identify rows whose scheduling columns were written in the window (bound via
   correlated `quiz_answer`/`recall_prompt` ids or an `updated_at` that is
   server-maintained — inspect schema on prod first).
2. Shift affected columns +8h; skip trackers already re-answered after the
   Phase 1 rollout (self-healed).
3. Verify: due list shows sensible next-recall times; one targeted recall E2E
   locally stays green.

### Phase 4 — Behavior (optional): audit remaining app-written timestamps — planned

Enumerate other app-written `TIMESTAMP` columns (notes, conversations, tokens…)
and repair the same window. Mostly cosmetic (created/updated display). Stop-safe
to skip entirely.

## Key decisions

- **Pin via `forceConnectionTimeZoneToSession=true` + `connectionTimeZone=UTC`**
  (Connector/J "Solution 2b"): session is forced to UTC on connect, so no
  conversion can ever be wrong, independent of JVM/host/server settings.
- **Repair is a manual prod runbook, not a Flyway migration** — a migration
  bounded by dates would corrupt correct local/e2e rows created in the same
  window.
- **Window bounded by id, not created_at** — ids are monotonic; created_at in
  the window is exactly the thing that's wrong.
- Phase 1 before repairs: it stops ongoing corruption and fixes the bulk of the
  display immediately; repairs then operate on a closed window.

## Status log

- 2026-07-24: plan written. Nothing executed yet.
