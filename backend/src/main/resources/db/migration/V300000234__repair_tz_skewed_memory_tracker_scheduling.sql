-- Repairs memory_tracker scheduling columns for the same confirmed 8h-ahead
-- skew as V300000233 (see .planning/quick/260724-db-timezone-fix/PLAN.md for
-- the read-only prod forensics that established the window and direction).
--
-- No-op everywhere by default (`spring.flyway.placeholders.tz_repair=1=0` in
-- application.yml, same placeholder V300000233 uses). Enabled (`1=1`) only via
-- a system property on the production deploy that ships this migration;
-- local/e2e/test/dev never override it.

-- last_recalled_at / next_recall_at: both are written together at review time,
-- so gate on the tracker's TRUE latest linked quiz_answer id (across all its
-- recall_prompt rows, not just ones inside the band) falling in the confirmed
-- skewed window. This naturally skips trackers already re-answered after the
-- Phase 1 rollout (self-healed with a correct, un-skewed value).
UPDATE memory_tracker mt
JOIN (
  SELECT rp.memory_tracker_id, MAX(rp.quiz_answer_id) AS true_latest_qa_id
  FROM recall_prompt rp
  WHERE rp.quiz_answer_id IS NOT NULL
  GROUP BY rp.memory_tracker_id
  HAVING true_latest_qa_id BETWEEN 180685 AND 225258
) latest ON latest.memory_tracker_id = mt.id
SET mt.last_recalled_at = mt.last_recalled_at - INTERVAL 8 HOUR,
    mt.next_recall_at = mt.next_recall_at - INTERVAL 8 HOUR
WHERE ${tz_repair};

-- assimilated_at: creation-time column, no join available, so gate by the
-- tracker id band confirmed to fall inside the Phase 2 date window plus the
-- date band itself as a second safety bound.
UPDATE memory_tracker
SET assimilated_at = assimilated_at - INTERVAL 8 HOUR
WHERE ${tz_repair}
  AND id BETWEEN 24179 AND 26039
  AND assimilated_at >= '2025-07-01 00:00:00'
  AND assimilated_at < '2026-06-01 00:00:00';
