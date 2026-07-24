-- Repairs note.created_at for the same confirmed 8h-ahead skew window as
-- V300000233/V300000234 (see .planning/quick/260724-db-timezone-fix/PLAN.md).
-- created_at is set once at note creation and never rewritten, so no
-- self-healing join is needed (unlike memory_tracker's scheduling columns).
--
-- Scoped to created_at only: note.updated_at showed a much less consistent
-- hourly pattern in prod (large spikes not matching the personal-review-time
-- signature seen in quiz_answer/memory_tracker), suggesting it is touched by
-- background/batch processes as well as user edits, so a blanket -8h shift
-- would risk mis-correcting rows whose skew source is different. Left alone.
--
-- No-op everywhere by default (`spring.flyway.placeholders.tz_repair=1=0` in
-- application.yml). Enabled (`1=1`) only via a system property on the
-- production deploy that ships this migration.
UPDATE note
SET created_at = created_at - INTERVAL 8 HOUR
WHERE ${tz_repair}
  AND id BETWEEN 26925 AND 29354
  AND created_at >= '2025-07-01 00:00:00'
  AND created_at < '2026-06-01 00:00:00';
