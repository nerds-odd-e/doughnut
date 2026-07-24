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

Pins the JDBC session tz explicitly going forward (defensive fix — see Status
log 2026-07-24: turned out to be a no-op for current prod behavior, since JVM
and DB were already both UTC, but still worth keeping as a safety net).

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

### Phase 2 — Behavior: repair skewed `quiz_answer.created_at` window — in-progress (migration written, not yet deployed)

Stats become fully correct for the confirmed ~11-month skewed core window.
Vehicle: **placeholder-gated Flyway migration** deployed via CD (decision
2026-07-24, replaces manual runbook).

**Revised window and direction** (superseding the original guess — see Status
log 2026-07-24 root-cause revision): the skew is **not** a 1-month window
around the 2026-06-20 JVM change. It's the opposite direction and a different,
much larger window, confirmed via read-only prod queries (`user_id=1`,
`quiz_answer` joined to `recall_prompt`/`memory_tracker`):

- **Conservative core window** (safe, unambiguous — both boundary months
  excluded): `created_at >= '2025-07-01 00:00:00' AND created_at < '2026-06-01 00:00:00'`.
  Confirmed id range for this window: `quiz_answer.id BETWEEN 180685 AND 225258`
  (39,257 rows for user_id=1; use the id band as the safety gate, same rationale
  as before — a fresh local/e2e DB's `quiz_answer` ids start low (baseline
  `AUTO_INCREMENT`), so this id band cannot match freshly-seeded local/e2e data).
- **Direction:** `created_at = created_at - INTERVAL 8 HOUR` (not `+`). Currently
  (post Phase 1, no JDBC conversion) the app displays `HOUR(created_at) + 8`
  (Shanghai offset) directly. Rows in this window are stored 8h **ahead** of
  their true instant (raw stored hour clusters at 6-9 UTC, displaying as a
  spurious 14:00-17:00 peak); subtracting 8h moves them back to the same raw
  22-23-0 UTC band that correctly displays ~06:00-08:00, matching the user's
  confirmed real habit.
- **Excluded (deliberately, for now):** 2025-06 and 2026-06 — transition months
  where both the "true" and "spurious" patterns are mixed at low enough
  resolution (month-level bins) that a per-row-safe cutoff isn't available from
  the evidence gathered so far. Left as known residual imprecision (~1-2 months
  out of ~3 years); can be revisited later if worth the extra forensics.
- **Root cause of the 2025-06 flip itself is still unknown** (predates the
  2026-06-20 JVM commit by a year; ruled out the Jan-2024 driver-family switch).
  Decision 2026-07-24: proceed on the empirical boundary without chasing the
  exact trigger — the data evidence (clean monthly flip, corroborated by the
  user's own stated habit) is strong enough to act on.

**Steps:**

1. Migration `V3000002XX__repair_tz_skewed_quiz_answer_created_at.sql`:
   `UPDATE quiz_answer SET created_at = created_at - INTERVAL 8 HOUR
    WHERE ${tz_repair} AND id BETWEEN 180685 AND 225258
    AND created_at >= '2025-07-01 00:00:00' AND created_at < '2026-06-01 00:00:00';`
   - `tz_repair` Flyway placeholder defaults to `1=0` (no-op everywhere);
     enabled (`1=1`) only via the prod startup script system property.
   - `updated_at` is `ON UPDATE CURRENT_TIMESTAMP` (server-side, already
     correct) — do not touch.
2. Verify in the stats UI: activity peak at ~07:00 dominant across the repaired
   window; "reviews today"/recent stats unaffected (window ends well before
   today). Regenerate ERD not needed (no schema change).

### Phase 3 — Behavior: repair `memory_tracker` scheduling columns — planned

Recalls due at correct times for trackers touched in the window
(`next_recall_at`, `last_recalled_at`, `assimilated_at` stored 8h **ahead** of
true instant, same direction/window as Phase 2 → `- INTERVAL 8 HOUR`). Same
vehicle: placeholder-gated migration in the same or a follow-up deploy.

1. Id-bounding doesn't apply to UPDATEs; use **join-based bounds** (same
   conservative date band as Phase 2: `>= '2025-07-01' AND < '2026-06-01'`):
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
  Deployed to prod via normal CI rolling replace; confirmed live via SSH
  (`-Duser.timezone=UTC`, datasource URL carries the new params).

- 2026-07-24: **Root-cause model revised after production read-only forensics (Phase 2 prereq).**
  Original theory (single skew window ≈ the 2026-06-20 JVM tz cutover) is **wrong**. Real picture,
  found via read-only `SELECT`s against prod through `gcloud compute ssh` (no writes):
  - Prod DB session/system time zone has **always been UTC** (`@@system_time_zone=UTC`,
    `@@session.time_zone=SYSTEM`) — confirmed directly. The 2026-06-20 JVM change (Singapore→UTC)
    made the **JVM** match the **DB**, which had already been UTC all along. So Phase 1's config
    pin, while still a good defensive fix, was a **no-op for current prod behavior** — there was no
    live JDBC mismatch today.
  - The real bug is historical: binning `quiz_answer` (joined to `recall_prompt`/`memory_tracker`,
    `user_id=1`) by month into "true ~7am cluster" (raw stored hour 22,23,0 UTC → displays 06:00–08:00
    Shanghai) vs "spurious ~15:00 cluster" (raw stored hour 6,7,8,9 UTC → displays 14:00–17:00
    Shanghai) shows a **clean flip**:
    - 2023-07 → 2025-05: "true" cluster dominant (matches user's confirmed real habit: ~7am + a
      smaller 19:00–22:00 evening scatter).
    - **2025-06 → 2026-06 (~13 months)**: flips hard — "spurious" cluster dominant (e.g. 2025-11:
      true=10, spurious=2442). This is the actual skewed window, ~13x bigger than originally planned.
    - 2026-06: transitional month (the 2026-06-20 JVM fix rolled out mid-month).
    - 2026-07 (post-fix): back to "true" dominant (true=1686, spurious=253) — matches user's
      self-reported real habit almost exactly. Confirms **today's live behavior is correct**.
  - User confirmed (2026-07-24) their real habit is "mostly ~7am, small scatter 19:00–22:00" — this
    matches the pre-2025-06 and post-2026-07 eras, and contradicts the 2025-06→2026-06 era, which is
    now confirmed to be **display-bug artifacts**, not real usage.
  - **Unknown**: what caused the 2025-06 flip. It predates the known 2026-06-20 JVM change by a full
    year, so that commit is not the trigger — it just happens to have corrected the symptom as a side
    effect (the commit's stated purpose was `fix(batch-question-generation)`, unrelated to stats).
    Candidate causes not yet checked: MySQL Connector/J version bumps around 2025-04/05
    (9.2.0 @ 2025-04-04, 9.3.0 @ 2025-05-07 — default `connectionTimeZone` behavior may differ across
    minor versions); a DB server VM change/recreation around mid-2025 (not visible in this repo's git
    history — would need GCP audit logs or `db-server` instance metadata); some other prod config
    change. Driver history in this data's lifetime: `mysql:mysql-connector-java` (legacy, unpinned
    version) from before 2023-06-29 (earliest `quiz_answer` row) until 2024-01-27, then
    `com.mysql:mysql-connector-j` 8.1.0 → 9.7.0 (today) — the Jan 2024 driver-family switch does NOT
    line up with the 2025-06 flip, so it's a separate, ruled-out candidate.
  - **Impact**: ~13 months of `quiz_answer.created_at` (and likely `memory_tracker` scheduling
    columns touched in the same window) are stored/interpreted 8h off from their true instant. This
    is a much bigger repair than the original "1 month around 6/20" scope, but also much better
    evidenced (clean monthly boundary, corroborated by the user's own habit description).
  - **Decision (developer, 2026-07-24):** proceed on the empirical boundary; do not chase the exact
    2025-06 trigger further. Phase 2 rewritten below with the confirmed conservative window
    (`2025-07-01` to `2026-06-01`, id band `180685`-`225258`, `- INTERVAL 8 HOUR`), excluding the two
    fuzzy transition months (2025-06, 2026-06) as accepted residual imprecision.

- 2026-07-24: Phase 2 implemented. Added `backend/src/main/resources/db/migration/
  V300000233__repair_tz_skewed_quiz_answer_created_at.sql` (placeholder-gated
  `UPDATE ... WHERE ${tz_repair} AND id BETWEEN 180685 AND 225258 AND created_at
  BETWEEN 2025-07-01 AND 2026-06-01`, `- INTERVAL 8 HOUR`). Learned Flyway does **not**
  support inline `${name:default}` syntax — default value must come from
  `spring.flyway.placeholders.tz_repair` config instead. Added `tz_repair: "1=0"` to
  every profile's `flyway.placeholders` map in `application.yml` (default/test, e2e,
  prod, dev); added `-Dspring.flyway.placeholders.tz_repair=1=1` to
  `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` (prod-only enable —
  safe to leave permanently since Flyway never re-applies a versioned migration once
  recorded in `flyway_schema_history`). Added
  `RecallStatsTimeZoneRepairMigrationTest` (loads the actual migration SQL from the
  classpath, substitutes the real default placeholder, asserts 0 rows updated and an
  inserted row's timestamp is unchanged — proves the no-op safety net). Full backend
  suite green; `pnpm lint:all` / `pnpm format:all` clean. Not yet deployed to prod —
  next step is push + let CD roll out, then confirm via the stats UI and
  `flyway_schema_history`.
