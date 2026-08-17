-- Backfill RecallLog for tutor scores that predate live logging.
-- Map session_item.feedback_score 4/5/3/2/1/0 → GOOD/EASY/HARD/SHRINK/AGAIN/AGAIN_ZERO
-- (same as live productOutcomeForScore). Skip null/unscored items. No answer_id.
-- elapsed_hours unknown → NULL. recorded_at = feedback_recorded_at (live writer source).
-- Idempotent: skip when a matching tutor log already exists.
INSERT INTO recall_log (
  memory_tracker_id,
  recorded_at,
  elapsed_hours,
  product_outcome,
  answer_id
)
SELECT
  scored.memory_tracker_id,
  scored.feedback_recorded_at,
  NULL,
  scored.product_outcome,
  NULL
FROM (
  SELECT
    si.memory_tracker_id,
    si.feedback_recorded_at,
    CASE si.feedback_score
      WHEN 5 THEN 'EASY'
      WHEN 4 THEN 'GOOD'
      WHEN 3 THEN 'HARD'
      WHEN 2 THEN 'SHRINK'
      WHEN 1 THEN 'AGAIN'
      WHEN 0 THEN 'AGAIN_ZERO'
    END AS product_outcome
  FROM session_item si
  WHERE si.feedback_score IN (0, 1, 2, 3, 4, 5)
) scored
WHERE NOT EXISTS (
  SELECT 1
  FROM recall_log rl
  WHERE rl.memory_tracker_id = scored.memory_tracker_id
    AND rl.recorded_at = scored.feedback_recorded_at
    AND rl.product_outcome = scored.product_outcome
    AND rl.answer_id IS NULL
);
