-- Backfill RecallLog for prompt grades that predate live logging.
-- correct true → GOOD, false → AGAIN; skip OVERLAP; skip answers that already have a log.
INSERT INTO recall_log (
  memory_tracker_id,
  recorded_at,
  elapsed_hours,
  product_outcome,
  answer_id
)
SELECT
  rp.memory_tracker_id,
  a.created_at,
  NULL,
  CASE WHEN a.correct = 1 THEN 'GOOD' ELSE 'AGAIN' END,
  a.id
FROM answer a
JOIN recall_prompt rp ON rp.answer_id = a.id
WHERE (a.outcome IS NULL OR a.outcome <> 'OVERLAP')
  AND NOT EXISTS (
    SELECT 1 FROM recall_log rl WHERE rl.answer_id = a.id
  );
