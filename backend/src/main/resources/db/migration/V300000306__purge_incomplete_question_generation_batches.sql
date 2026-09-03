-- One-time purge of incomplete question-generation batches accumulated
-- while the OpenAI Batch API failed to accept valid input files.
-- Imported COMPLETED batches are kept.
DELETE FROM `question_generation_batch`
WHERE (`status` <> 'COMPLETED' OR `imported_at` IS NULL);
