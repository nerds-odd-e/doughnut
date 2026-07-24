-- Repairs quiz_answer.created_at for a confirmed ~11-month window where rows were
-- stored 8h ahead of their true instant (see .planning/quick/260724-db-timezone-fix/PLAN.md
-- for the read-only prod forensics that established this window and direction).
--
-- No-op everywhere by default (`spring.flyway.placeholders.tz_repair=1=0` in
-- application.yml). Enabled (`1=1`) only via a system property on the production
-- deploy that ships this migration; local/e2e/test/dev never override it.
UPDATE quiz_answer
SET created_at = created_at - INTERVAL 8 HOUR
WHERE ${tz_repair}
  AND id BETWEEN 180685 AND 225258
  AND created_at >= '2025-07-01 00:00:00'
  AND created_at < '2026-06-01 00:00:00';
