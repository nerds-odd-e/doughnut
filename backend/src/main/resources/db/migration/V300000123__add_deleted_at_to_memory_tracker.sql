ALTER TABLE `memory_tracker` ADD COLUMN `deleted_at` timestamp NULL DEFAULT NULL AFTER `spelling`;
ALTER TABLE `memory_tracker` DROP INDEX `user_note_spelling`;
ALTER TABLE `memory_tracker` ADD UNIQUE INDEX `user_note_spelling_active`
  (`user_id`, `note_id`, `spelling`, (IF(`deleted_at` IS NULL, 1, NULL)));
