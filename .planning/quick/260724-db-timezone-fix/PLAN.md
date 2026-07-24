# DB timezone fix — pin JDBC session to UTC + repair skewed window (closed 2026-07-24)

All 4 phases done and verified live in production. Kept as the permanent
forensics record referenced by the migration file comments
(`V300000233`/`234`/`235`).

## Root cause

Production MySQL's session/system time zone has **always been UTC**. The
production JVM ran `TZ=Asia/Singapore` until 2026-06-20, then switched to
`TZ=UTC`. With Connector/J defaults (`connectionTimeZone=LOCAL`,
`preserveInstants=true`), the driver assumes session tz == JVM tz without
checking — so for roughly 13 months (**2025-06 to 2026-06**), every
app-written `TIMESTAMP` was stored **8 hours ahead** of its true instant.
Confirmed via read-only production forensics (binning `quiz_answer` by month
into a "true ~7am-local cluster" — raw UTC hours 22-1 — vs a "spurious
~15:00-local cluster" — raw UTC hours 6-9): a clean flip from true-dominant
(2023-07 → 2025-05) to spurious-dominant (2025-06 → 2026-06) back to
true-dominant (2026-07+, post JVM fix), corroborated by the user's own
confirmed real review habit (~7am + a small 19:00-22:00 evening scatter).
The exact 2025-06 trigger was never identified (predates the known JVM
change by a year; MySQL Connector/J version bumps and driver-family
switches were checked and ruled out) — the empirical boundary was strong
enough to act on without further investigation.

**Conservative repair window** (excludes the two fuzzy transition months as
accepted residual imprecision): `created_at >= '2025-07-01 00:00:00' AND
created_at < '2026-06-01 00:00:00'`, direction `- INTERVAL 8 HOUR`.

## Phases

### Phase 1 — pin DB session time zone to UTC

Appended `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` to
every real runtime JDBC URL (`application.yml`, `db-*.properties`, the prod
MIG startup script, `cloud_agent_setup.sh`). Turned out to be a no-op for
*current* prod behavior (JVM and DB were already both UTC by the time this
shipped) but is a correct defensive fix and stops any future JVM/DB tz
drift from silently corrupting data again. Verified via
`DatabaseTimeZoneTest` (session tz + instant round-trip) and live SSH
confirmation.

### Phase 2 — repair `quiz_answer.created_at`

Placeholder-gated Flyway migration `V300000233`: `id BETWEEN 180685 AND
225258` + the date band above, `tz_repair` placeholder defaults to `1=0`
(no-op) everywhere except a system-property override on the prod deploy.
Verified live: post-repair histogram peaks at raw UTC 22-1 with a secondary
bump at UTC 11-14, matching the user's confirmed habit; the old spurious
cluster is gone.

### Phase 3 — repair `memory_tracker` scheduling columns

Migration `V300000234`, two placeholder-gated `UPDATE`s: (1)
`last_recalled_at`/`next_recall_at` gated by a **self-healing-aware join**
on each tracker's *true latest* linked `quiz_answer` (not just answers
inside the band) — correctly skips trackers already re-answered after
Phase 1 shipped; (2) `assimilated_at` gated by id band `24179`-`26039` +
date band. Verified live: exact count-preserving -8h shift for
`assimilated_at`; `last_recalled_at` cluster moved from raw UTC 6-9 to
22-1.

### Phase 4 — repair `note.created_at` (narrowed scope)

Confirmed the originally-reported recall-stats bug needed no further work:
`recall_prompt.created_at` is only used for a duration subtraction
(`answerCreatedAt - promptCreatedAt`) that cancels an equal skew on both
sides, and all stats bucketing uses `quiz_answer.created_at` (Phase 2).
Audited other app-written timestamp columns; developer chose to repair
only `note.created_at` (user-visible, immutable after creation, clean
skew signature — id band `26925`-`29354`). Explicitly **skipped**
`note.updated_at`: its hourly distribution in the same window was far
larger and less coherent than a personal-review-time signature, most
likely mixed with background/batch writes (AI content generation,
imports) — not safely correctable from the evidence available. Also
skipped: conversations, `predefined_question`, `question_generation_batch`,
`note_embeddings`, `failure_report` (internal/non-user-facing). Verified
live via `V300000235`: exact count-preserving -8h shift.

## Key decisions

- **Pin via `forceConnectionTimeZoneToSession=true` + `connectionTimeZone=UTC`**
  (Connector/J "Solution 2b"): forces the session to UTC on connect so no
  conversion can be wrong, independent of JVM/host/server settings.
- **Repair via placeholder-gated Flyway migration**, not a manual runbook:
  ships through CD/review instead of a hand-typed session on the
  private-IP DB VM. The `tz_repair` placeholder (default `1=0`) is the
  *primary* safety net; literal id bands are a secondary, prod-side
  precision tool (not a portability guarantee — note's band happens to
  sit below fresh-DB `AUTO_INCREMENT`, so the placeholder alone protects
  local/e2e/test there).
- Phase 1 shipped before the repairs so it stops any further corruption
  immediately, letting the repairs target a now-closed historical window.
