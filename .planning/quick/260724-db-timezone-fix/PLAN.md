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
  - **Decision needed from developer** before designing the real Phase 2: (a) worth spending more
    effort to find the exact 2025-06 trigger (for confidence in the correction direction/scope), or
    (b) proceed straight to a repair plan bounded by the empirically-observed monthly cluster
    flip (2025-06-01 ↔ 2026-06-20ish), accepting some fuzziness at the two transition months.
  - Phases 2–4 as originally written are **superseded** — do not execute them as-is. A new repair
    plan must be designed against the real (~13-month) window once the developer decides how much
    further to investigate the 2025-06 trigger.
