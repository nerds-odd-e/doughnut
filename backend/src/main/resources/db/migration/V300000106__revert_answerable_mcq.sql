-- Add predefined_question_id and quiz_answer_id columns back to recall_prompt (nullable first)
ALTER TABLE `recall_prompt` ADD COLUMN `predefined_question_id` int unsigned DEFAULT NULL;
ALTER TABLE `recall_prompt` ADD COLUMN `quiz_answer_id` int unsigned DEFAULT NULL;
ALTER TABLE `recall_prompt` ADD KEY `fk_recall_prompt_predefined_question` (`predefined_question_id`);
ALTER TABLE `recall_prompt` ADD CONSTRAINT `fk_recall_prompt_predefined_question` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`);
ALTER TABLE `recall_prompt` ADD KEY `fk_recall_prompt_quiz_answer` (`quiz_answer_id`);
ALTER TABLE `recall_prompt` ADD CONSTRAINT `fk_recall_prompt_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`);

-- Add predefined_question_id and quiz_answer_id columns back to assessment_question_instance (nullable first)
ALTER TABLE `assessment_question_instance` ADD COLUMN `predefined_question_id` int unsigned DEFAULT NULL;
ALTER TABLE `assessment_question_instance` ADD COLUMN `quiz_answer_id` int unsigned DEFAULT NULL;
ALTER TABLE `assessment_question_instance` ADD KEY `fk_assessment_question_predefined` (`predefined_question_id`);
ALTER TABLE `assessment_question_instance` ADD CONSTRAINT `fk_assessment_question_predefined` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`);
ALTER TABLE `assessment_question_instance` ADD KEY `fk_assessment_question_quiz_answer` (`quiz_answer_id`);
ALTER TABLE `assessment_question_instance` ADD CONSTRAINT `fk_assessment_question_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`);

-- Migrate data from answerable_mcq to recall_prompt
UPDATE `recall_prompt` rp
INNER JOIN `answerable_mcq` amcq ON rp.`answerable_mcq_id` = amcq.`id`
SET rp.`predefined_question_id` = amcq.`predefined_question_id`,
    rp.`quiz_answer_id` = amcq.`quiz_answer_id`;

-- Migrate data from answerable_mcq to assessment_question_instance
UPDATE `assessment_question_instance` aqi
INNER JOIN `answerable_mcq` amcq ON aqi.`answerable_mcq_id` = amcq.`id`
SET aqi.`predefined_question_id` = amcq.`predefined_question_id`,
    aqi.`quiz_answer_id` = amcq.`quiz_answer_id`;

-- Make predefined_question_id NOT NULL in assessment_question_instance (after data migration)
ALTER TABLE `assessment_question_instance` MODIFY COLUMN `predefined_question_id` int unsigned NOT NULL;

-- Drop foreign keys and columns related to answerable_mcq
ALTER TABLE `recall_prompt` DROP FOREIGN KEY `fk_recall_prompt_answerable_mcq`;
ALTER TABLE `recall_prompt` DROP KEY `fk_recall_prompt_answerable_mcq`;
ALTER TABLE `recall_prompt` DROP COLUMN `answerable_mcq_id`;

ALTER TABLE `assessment_question_instance` DROP FOREIGN KEY `fk_assessment_question_answerable_mcq`;
ALTER TABLE `assessment_question_instance` DROP KEY `fk_assessment_question_answerable_mcq`;
ALTER TABLE `assessment_question_instance` DROP COLUMN `answerable_mcq_id`;

-- Drop the answerable_mcq table
DROP TABLE `answerable_mcq`;
