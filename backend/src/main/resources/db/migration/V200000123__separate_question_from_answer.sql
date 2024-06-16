-- Step 1: Rename quiz_question to quiz_question_and_answer
RENAME TABLE `quiz_question` TO `quiz_question_and_answer`;

-- Step 2: Create the new quiz_question table
CREATE TABLE `quiz_question` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `raw_json_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `check_spell` tinyint(1) DEFAULT '0',
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_mask` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: Add foreign key to quiz_question_and_answer
ALTER TABLE `quiz_question_and_answer`
ADD COLUMN `quiz_question_id` int unsigned,
ADD UNIQUE KEY `uq_quiz_question_id` (`quiz_question_id`),
ADD CONSTRAINT `fk_quiz_question_and_answer_quiz_question_id` FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question` (`id`);

INSERT INTO `quiz_question` (`id`, `raw_json_question`, `check_spell`, `image_url`, `image_mask`)
SELECT `id`, `raw_json_question`, `check_spell`, `image_url`, `image_mask` FROM `quiz_question_and_answer`;

UPDATE `quiz_question_and_answer`
SET `quiz_question_id` = `id`;

-- Step 5: Remove the fields from quiz_question_and_answer
ALTER TABLE `quiz_question_and_answer`
DROP COLUMN `raw_json_question`,
DROP COLUMN `check_spell`,
DROP COLUMN `image_url`,
DROP COLUMN `image_mask`;
