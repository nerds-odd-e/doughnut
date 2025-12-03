-- Create answerable_mcq table
CREATE TABLE `answerable_mcq` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `predefined_question_id` int unsigned NOT NULL,
  `quiz_answer_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_answerable_mcq_predefined_question` (`predefined_question_id`),
  KEY `fk_answerable_mcq_quiz_answer` (`quiz_answer_id`),
  CONSTRAINT `fk_answerable_mcq_predefined_question` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`),
  CONSTRAINT `fk_answerable_mcq_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add question_type column to recall_prompt (nullable first)
ALTER TABLE `recall_prompt` ADD COLUMN `question_type` varchar(50) DEFAULT NULL;

-- Add answerable_mcq_id column to recall_prompt (nullable first)
ALTER TABLE `recall_prompt` ADD COLUMN `answerable_mcq_id` int unsigned DEFAULT NULL;
ALTER TABLE `recall_prompt` ADD KEY `fk_recall_prompt_answerable_mcq` (`answerable_mcq_id`);
ALTER TABLE `recall_prompt` ADD CONSTRAINT `fk_recall_prompt_answerable_mcq` FOREIGN KEY (`answerable_mcq_id`) REFERENCES `answerable_mcq` (`id`);

-- Add answerable_mcq_id column to assessment_question_instance (nullable first)
ALTER TABLE `assessment_question_instance` ADD COLUMN `answerable_mcq_id` int unsigned DEFAULT NULL;
ALTER TABLE `assessment_question_instance` ADD KEY `fk_assessment_question_answerable_mcq` (`answerable_mcq_id`);
ALTER TABLE `assessment_question_instance` ADD CONSTRAINT `fk_assessment_question_answerable_mcq` FOREIGN KEY (`answerable_mcq_id`) REFERENCES `answerable_mcq` (`id`);

-- Migrate data from recall_prompt to answerable_mcq
INSERT INTO `answerable_mcq` (`predefined_question_id`, `quiz_answer_id`)
SELECT `predefined_question_id`, `quiz_answer_id`
FROM `recall_prompt`
WHERE `predefined_question_id` IS NOT NULL;

-- Update recall_prompt to reference the new answerable_mcq records
UPDATE `recall_prompt` rp
INNER JOIN `answerable_mcq` amcq ON rp.`predefined_question_id` = amcq.`predefined_question_id` 
  AND (rp.`quiz_answer_id` = amcq.`quiz_answer_id` OR (rp.`quiz_answer_id` IS NULL AND amcq.`quiz_answer_id` IS NULL))
SET rp.`answerable_mcq_id` = amcq.`id`, rp.`question_type` = 'MCQ';

-- Migrate data from assessment_question_instance to answerable_mcq
INSERT INTO `answerable_mcq` (`predefined_question_id`, `quiz_answer_id`)
SELECT `predefined_question_id`, `quiz_answer_id`
FROM `assessment_question_instance`
WHERE `predefined_question_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `answerable_mcq` amcq 
    WHERE amcq.`predefined_question_id` = `assessment_question_instance`.`predefined_question_id`
      AND (amcq.`quiz_answer_id` = `assessment_question_instance`.`quiz_answer_id` 
        OR (amcq.`quiz_answer_id` IS NULL AND `assessment_question_instance`.`quiz_answer_id` IS NULL))
  );

-- Update assessment_question_instance to reference the new answerable_mcq records
UPDATE `assessment_question_instance` aqi
INNER JOIN `answerable_mcq` amcq ON aqi.`predefined_question_id` = amcq.`predefined_question_id`
  AND (aqi.`quiz_answer_id` = amcq.`quiz_answer_id` OR (aqi.`quiz_answer_id` IS NULL AND amcq.`quiz_answer_id` IS NULL))
SET aqi.`answerable_mcq_id` = amcq.`id`;

-- Make question_type NOT NULL in recall_prompt
ALTER TABLE `recall_prompt` MODIFY COLUMN `question_type` varchar(50) NOT NULL;

-- Make answerable_mcq_id NOT NULL in assessment_question_instance
ALTER TABLE `assessment_question_instance` MODIFY COLUMN `answerable_mcq_id` int unsigned NOT NULL;

-- Drop old columns from recall_prompt
ALTER TABLE `recall_prompt` DROP FOREIGN KEY `fk_question_and_answer`;
ALTER TABLE `recall_prompt` DROP FOREIGN KEY `fk_quiz_answer`;
ALTER TABLE `recall_prompt` DROP KEY `fk_question_and_answer`;
ALTER TABLE `recall_prompt` DROP KEY `fk_quiz_answer`;
ALTER TABLE `recall_prompt` DROP COLUMN `predefined_question_id`;
ALTER TABLE `recall_prompt` DROP COLUMN `quiz_answer_id`;

-- Drop old columns from assessment_question_instance
ALTER TABLE `assessment_question_instance` DROP FOREIGN KEY `fk_assess_question_predefined`;
ALTER TABLE `assessment_question_instance` DROP FOREIGN KEY `fk_assess_question_quiz_answer`;
ALTER TABLE `assessment_question_instance` DROP KEY `fk_assess_question_predefined`;
ALTER TABLE `assessment_question_instance` DROP KEY `fk_assess_question_quiz_answer`;
ALTER TABLE `assessment_question_instance` DROP COLUMN `predefined_question_id`;
ALTER TABLE `assessment_question_instance` DROP COLUMN `quiz_answer_id`;

