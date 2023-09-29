RENAME TABLE `marked_questions` TO `suggested_question_for_fine_tuning`;
ALTER TABLE `suggested_question_for_fine_tuning` DROP COLUMN `is_good`;
ALTER TABLE `suggested_question_for_fine_tuning` ADD COLUMN `preserved_question` TEXT;
ALTER TABLE `suggested_question_for_fine_tuning` ADD COLUMN `preserved_note_content` TEXT;
ALTER TABLE `suggested_question_for_fine_tuning` ADD COLUMN `approved` BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE `suggested_question_for_fine_tuning` DROP FOREIGN KEY `fk_note_id`;
ALTER TABLE `suggested_question_for_fine_tuning` DROP COLUMN `note_id`;

