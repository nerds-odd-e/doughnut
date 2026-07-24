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

### Phase 1 — Behavior: pin DB session time zone to UTC — done

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

Stats become fully correct (recent month included). Vehicle: **placeholder-gated
Flyway migration** deployed via CD (decision 2026-07-24, replaces manual runbook).

1. Prereq (read-only prod query, Jidoka): find the boundary ids — the cutover
   shows as an ~8h **backward** jump in `created_at` between consecutive ids
   (fuzzy over the few-minute rolling replace; ±8h on a handful of boundary rows
   is acceptable). Window end = last row before the Phase 1 rollout, same
   signature in reverse. Also confirm
   `SELECT @@session.time_zone, @@system_time_zone, NOW(), UTC_TIMESTAMP();`.
2. Migration `V3000002XX__repair_tz_skewed_quiz_answer_created_at.sql`:
   `UPDATE quiz_answer SET created_at = created_at + INTERVAL 8 HOUR
    WHERE ${tz_repair} AND id BETWEEN <literal bounds> AND created_at BETWEEN <band>;`
   - `tz_repair` Flyway placeholder defaults to `1=0` (no-op everywhere);
     enabled (`1=1`) only via the prod startup script system property.
   - `updated_at` is `ON UPDATE CURRENT_TIMESTAMP` (server-side, already
     correct) — do not touch.
3. Deploy **after** Phase 1 is live (window closed; rolling replace cannot
   append skewed rows post-repair).
4. Verify in the stats UI: activity peak at ~07:00 across the whole year;
   "reviews today" consistent. Regenerate ERD not needed (no schema change).

### Phase 3 — Behavior: repair `memory_tracker` scheduling columns — planned

Recalls due at correct times for trackers touched in the window
(`next_recall_at`, `last_recalled_at`, `assimilated_at` stored 8h early →
recalls surface 8h early). Same vehicle: placeholder-gated migration in the
same or a follow-up deploy.

1. Id-bounding doesn't apply to UPDATEs; use **join-based bounds**:
   `last_recalled_at`/`next_recall_at` for trackers whose latest
   `recall_prompt`→`quiz_answer` id falls in the Phase 2 window;
   `assimilated_at` for trackers whose own id is in the window (creation-time).
2. Skip trackers already re-answered after the Phase 1 rollout (self-healed —
   the join on latest answer id handles this naturally).
3. Escalate to a Java migration (`backend/src/main/java/db/migration/`) only if
   the SQL gets contorted.
4. Verify: due list shows sensible next-recall times; one targeted recall E2E
   locally stays green.

### Phase 4 — Behavior (optional): audit remaining app-written timestamps — planned

Enumerate other app-written `TIMESTAMP` columns (notes, conversations, tokens…)
and repair the same window. Mostly cosmetic (created/updated display). Stop-safe
to skip entirely.

## Key decisions

- **Pin via `forceConnectionTimeZoneToSession=true` + `connectionTimeZone=UTC`**
  (Connector/J "Solution 2b"): session is forced to UTC on connect, so no
  conversion can ever be wrong, independent of JVM/host/server settings.
- **Repair via placeholder-gated Flyway migration** (revised 2026-07-24;
  originally a manual runbook). Rationale: ships through CD/review instead of a
  hand-typed session on the private-IP DB VM. Safety: `tz_repair` placeholder
  defaults to no-op — a date- or id-only bound would corrupt local/e2e rows
  (fresh DBs inherit `AUTO_INCREMENT=5038` from the baseline, so ids collide
  with prod's range). Manual prod work shrinks to one read-only boundary query.
- **Window bounded by literal ids + date band + placeholder gate** —
  `created_at` alone is ambiguous in an 8h band around the cutover; ids alone
  are not env-safe.
- Phase 1 before repairs: it stops ongoing corruption and fixes the bulk of the
  display immediately; repairs then operate on a closed window.

## Status log

- 2026-07-24: plan written. Nothing executed yet.
- 2026-07-24: Phase 1 done. Added `backend/src/test/java/com/odde/doughnut/configs/DatabaseTimeZoneTest.java`
  (asserts `SELECT @@session.time_zone` is `+00:00` and a `FROM_UNIXTIME` round-trip through JDBC
  `Timestamp` matches the known epoch millis exactly). Confirmed red first (`SYSTEM`), then green after
  the config change. Appended `?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` (or
  `&...` where a `?` already existed) to every real runtime JDBC URL:
  `backend/src/main/resources/application.yml` (e2e fallback + prod), `db-dev.properties`,
  `db-test.properties`, `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` (the actual
  prod-controlling `-Dspring.datasource.url` override), and `scripts/cloud_agent_setup.sh`
  (`SPRING_DATASOURCE_URL`/`INPUT_DB_URL` for the no-Nix Cloud VM path).
  Surprises: `db-e2e.properties` defines no `db.url` (e2e always resolves via the `INPUT_DB_URL`
  env var or the `application.yml` fallback, so nothing to change there); `.github/workflows/ci.yml`
  never sets `INPUT_DB_URL`, so CI e2e always uses the `application.yml` fallback — updating that one
  line covers CI; `.github/starting_backend_actions/action.yml` also references `INPUT_DB_URL` but is
  not invoked from any workflow (dead/orphaned composite action) — left untouched.
  Full backend suite (`pnpm backend:test_only`) green after the change; `pnpm lint:all` /
  `pnpm format:all` clean.
  No scope change to Phases 2–4.
