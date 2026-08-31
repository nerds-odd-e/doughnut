-- Gated one-time purge of incomplete question-generation batches accumulated
-- while the OpenAI Batch API failed to accept valid input files.
-- Default placeholder is 1=0 (no-op). Production enables 1=1 for the deploy
-- that ships this version so leftover PENDING rows are not resubmitted.
-- Imported COMPLETED batches are kept. After Flyway has applied this version
-- in production, revert the production placeholder to 1=0.
DELETE FROM `question_generation_batch`
WHERE ${question_generation_batch_incomplete_purge}
  AND (`status` <> 'COMPLETED' OR `imported_at` IS NULL);
