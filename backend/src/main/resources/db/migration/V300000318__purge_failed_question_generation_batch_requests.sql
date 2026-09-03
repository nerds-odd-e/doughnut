-- One-time purge of FAILED question-generation batch request rows so twice-failed
-- trackers (two FAILED requests, no later IMPORTED) can be queued again without
-- waiting for the parent batch's 30-day retention to remove them.
-- Parent question_generation_batch rows and non-FAILED request rows
-- (PENDING, OUTPUT_READY, IMPORTED) are never touched.
DELETE FROM `question_generation_batch_request`
WHERE `status` = 'FAILED';
