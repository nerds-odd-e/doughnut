-- Advances stale recall anchors to the latest linked accidental-match Answer.
-- Overlap Answers remain excluded by the exact outcome match.
--
-- No-op everywhere by default (`spring.flyway.placeholders.recall_anchor_repair=1=0`).
-- Enabled (`1=1`) only for the deliberate production repair deployment.
UPDATE memory_tracker mt
JOIN (
  SELECT rp.memory_tracker_id, MAX(qa.created_at) AS latest_answer_at
  FROM recall_prompt rp
  JOIN quiz_answer qa ON qa.id = rp.quiz_answer_id
  WHERE qa.outcome = 'ACCIDENTAL_MATCH'
  GROUP BY rp.memory_tracker_id
) accidental_matches ON accidental_matches.memory_tracker_id = mt.id
SET mt.last_recalled_at = accidental_matches.latest_answer_at
WHERE ${recall_anchor_repair}
  AND accidental_matches.latest_answer_at > mt.last_recalled_at;
