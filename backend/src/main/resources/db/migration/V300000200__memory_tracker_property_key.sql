ALTER TABLE `memory_tracker`
  ADD COLUMN `property_key` VARCHAR(255) NOT NULL DEFAULT '',
  DROP INDEX `user_note_spelling_active`,
  ADD UNIQUE KEY `user_note_spelling_active` (
    `user_id`,
    `note_id`,
    `spelling`,
    `property_key`,
    (if((`deleted_at` is null), 1, NULL))
  );
