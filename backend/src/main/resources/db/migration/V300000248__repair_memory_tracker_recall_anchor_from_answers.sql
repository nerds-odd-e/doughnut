-- Advances stale recall anchors to the latest linked normal Answer, identified
-- by its null outcome.
--
-- No-op everywhere by default (`spring.flyway.placeholders.recall_anchor_repair=1=0`).
-- Enabled (`1=1`) only for the deliberate production repair deployment.
UPDATE memory_tracker mt
JOIN (
  SELECT rp.memory_tracker_id, MAX(qa.created_at) AS latest_answer_at
  FROM recall_prompt rp
  JOIN quiz_answer qa ON qa.id = rp.quiz_answer_id
  WHERE qa.outcome IS NULL
  GROUP BY rp.memory_tracker_id
) normal_answers ON normal_answers.memory_tracker_id = mt.id
SET mt.last_recalled_at = normal_answers.latest_answer_at
WHERE ${recall_anchor_repair}
  AND normal_answers.latest_answer_at > mt.last_recalled_at;
