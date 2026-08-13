-- Advances stale recall anchors to the latest recorded Tutor Feedback.
--
-- No-op everywhere by default (`spring.flyway.placeholders.recall_anchor_repair=1=0`).
-- Enabled (`1=1`) only for the deliberate production repair deployment.
UPDATE memory_tracker mt
JOIN (
  SELECT memory_tracker_id, MAX(feedback_recorded_at) AS latest_feedback_at
  FROM session_item
  GROUP BY memory_tracker_id
) tutor_feedback ON tutor_feedback.memory_tracker_id = mt.id
SET mt.last_recalled_at = tutor_feedback.latest_feedback_at
WHERE ${recall_anchor_repair}
  AND tutor_feedback.latest_feedback_at > mt.last_recalled_at;
