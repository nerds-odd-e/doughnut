# Batch Question Generation — Production Reliability

Make the hourly question-generation batch job verifiably run in production, observable, timezone-correct, and safe under more than one instance.

## Context / discoveries

### 2026-06-19/20 (initial investigation)

- Prod runs **1** backend instance; autoscaler `min=1, max=2`.
- Java stdout/stderr wired to an **unread pipe** — can block the single scheduler thread.
- Scheduler is **single-threaded**, shared with `EmbeddingMaintenanceJob` (every 5 min, ~591 notebooks).
- Eligibility catch-up fix (`e6de80b147`, `9c63e77ba5`) was on `main` before first deploy; migration `V300000221` drops `question_generation_batch_user_state`.
- `QuestionGenerationBatchMaintenanceRunState` is in-memory only.

### 2026-06-20 (follow-up — investigation complete, no separate phase needed)

New instance `doughnut-app-group-8vm5`, jar deployed **2026-06-20 02:41 UTC** (includes catch-up eligibility).

| Observation | Meaning |
|-------------|---------|
| Last maintenance **08:38:04Z** (not `:00`) | Scheduler ran **late** once (~38 min after the hour), not a manual resume-only action. Submission timestamp matches exactly. |
| Batch **#2 SUBMITTED** since 08:38 UTC, 150 `PENDING` rows | Automatic submission **did work** on that delayed run. |
| OpenAI batch `batch_6a365197442c8190a62f856fe8b9ddb2` status **`completed`** (150/150) | OpenAI finished in ~5 min. Local row still `SUBMITTED` because **polling never ran again**. |
| No maintenance updates for hours after 08:38 | Hourly job **not firing reliably** — primary root cause for stuck batches. |

**Conclusion:** Stuck `SUBMITTED` is a symptom of unreliable scheduling/resume, not slow OpenAI. Phase 1 (scheduler + logging) is the correct first fix. **Immediate ops:** admin → "Resume existing batches" will poll, collect output, and import the completed batch while code fix deploys.

### Timezone

- Cron and silent-window triggers are **timezone-invariant** for whole-hour offsets.
- DB stores naive datetimes in **SGT**; API/admin displays **UTC** (`08:38Z` = `16:38` in DB). Phase 3 pins JVM to UTC for consistency.

## Immediate ops unblock (no code)

Admin → **"Resume existing batches"** — OpenAI batch is already `completed`; this should advance local batch #2 through poll → collect → import.

---

## Phases

### Phase 1 (behavior) — Hourly maintenance runs reliably and is visible — DONE

Outcome: scheduled maintenance runs every hour without hanging on logging, is not starved by the embedding job, and stuck `SUBMITTED` batches get polled on schedule.

- [backend/src/main/resources/logback-spring.xml](../backend/src/main/resources/logback-spring.xml) `prod`: `AsyncAppender` with `neverBlock=true` + rolling FILE appender.
- [backend/src/main/resources/application.yml](../backend/src/main/resources/application.yml) `prod`: `spring.task.scheduling.pool.size: 3`.

Verification: after deploy, prod log shows maintenance at `:00:00` UTC each hour; batch #2 (or next stuck batch) advances from `SUBMITTED` to `COMPLETED`/`IMPORTED` without manual resume.

### Phase 2 (behavior) — Durable, accurate maintenance run-state — DONE

Outcome: admin shows last scheduled vs manual run from DB, surviving restart.

- Flyway table `question_generation_batch_maintenance_run`.
- Replace in-memory `QuestionGenerationBatchMaintenanceRunState`.
- Admin DTO + Vue: distinguish scheduled vs manual.
- Tests: `QuestionGenerationBatchMaintenanceRunRepositoryTest`, extend admin status tests.

### Phase 3 (behavior) — Production runs in UTC — DONE

Outcome: JVM and logs use UTC; remove misleading `TZ=Asia/Singapore` from startup script.

- [infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh](../infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh): `-Duser.timezone=UTC` and `TZ=UTC`.
- Update [docs/question-generation-batch-operations.md](../docs/question-generation-batch-operations.md).

### Phase 4 (behavior) — Safe under two instances — DONE

Outcome: ShedLock on `runHourlyMaintenance` so autoscaler max=2 cannot double-submit.

- ShedLock 7.7.0 (`shedlock-spring`, `shedlock-provider-jdbc-template`), Flyway `shedlock` table, `ShedLockConfig` (`@Profile("prod")`), `@SchedulerLock` on hourly job.
- `QuestionGenerationBatchMaintenanceConcurrencyTest` verifies annotation, lock table, duplicate-lock behavior, prod `LockProvider` bean.

**Plan complete** — all four phases delivered.

---

## Per-phase discipline

- `CURSOR_DEV=true nix develop -c pnpm backend:verify` (or `backend:test_only` when no migration).
- Commit, push, CD deploy before next phase.
