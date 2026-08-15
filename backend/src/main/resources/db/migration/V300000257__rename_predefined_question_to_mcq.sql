-- Rename table predefined_question to mcq and recall_prompt.predefined_question_id to mcq_id.

ALTER TABLE `recall_prompt`
  DROP FOREIGN KEY `fk_recall_prompt_predefined_question`;

ALTER TABLE `predefined_question`
  DROP FOREIGN KEY `quiz_question_fk_note_id`;

RENAME TABLE `predefined_question` TO `mcq`;

ALTER TABLE `mcq`
  RENAME INDEX `quiz_question_fk_note_id` TO `fk_mcq_note`;

ALTER TABLE `mcq`
  ADD CONSTRAINT `fk_mcq_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE;

ALTER TABLE `recall_prompt`
  CHANGE COLUMN `predefined_question_id` `mcq_id` int unsigned DEFAULT NULL,
  RENAME INDEX `fk_recall_prompt_predefined_question` TO `fk_recall_prompt_mcq`;

ALTER TABLE `recall_prompt`
  ADD CONSTRAINT `fk_recall_prompt_mcq` FOREIGN KEY (`mcq_id`) REFERENCES `mcq` (`id`);
