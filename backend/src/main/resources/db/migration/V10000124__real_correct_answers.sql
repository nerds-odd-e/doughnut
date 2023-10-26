ALTER TABLE `suggested_question_for_fine_tuning` ADD `real_correct_answers` varchar(100) NOT NULL DEFAULT "";
ALTER TABLE `suggested_question_for_fine_tuning` DROP COLUMN `is_duplicated`;
ALTER TABLE `suggested_question_for_fine_tuning` DROP FOREIGN KEY `fk_quiz_answer_id_2`;
ALTER TABLE `suggested_question_for_fine_tuning` DROP COLUMN `quiz_question_id`;
