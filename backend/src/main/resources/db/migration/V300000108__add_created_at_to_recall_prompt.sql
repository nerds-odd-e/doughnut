ALTER TABLE `recall_prompt` ADD COLUMN `created_at` timestamp(3) NULL DEFAULT NULL;

-- Set created_at for existing records based on predefined_question's created_at if available
UPDATE `recall_prompt` rp
INNER JOIN `predefined_question` pq ON rp.`predefined_question_id` = pq.`id`
SET rp.`created_at` = pq.`created_at`
WHERE rp.`created_at` IS NULL;

-- For records without predefined_question, set to current timestamp
UPDATE `recall_prompt`
SET `created_at` = CURRENT_TIMESTAMP(3)
WHERE `created_at` IS NULL;
