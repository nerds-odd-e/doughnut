-- Rename index and constraint names from "review" to "recall" for consistency

ALTER TABLE `memory_tracker` RENAME INDEX `last_reviewed_at` TO `last_recalled_at`;
ALTER TABLE `memory_tracker` RENAME INDEX `FK_review_point_user_id` TO `FK_memory_tracker_user_id`;
ALTER TABLE `memory_tracker` DROP FOREIGN KEY `review_point_fk_note_id`;
ALTER TABLE `memory_tracker` ADD CONSTRAINT `memory_tracker_fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE;

ALTER TABLE `conversation` RENAME INDEX `review_question_instance_id` TO `recall_prompt_id`;
