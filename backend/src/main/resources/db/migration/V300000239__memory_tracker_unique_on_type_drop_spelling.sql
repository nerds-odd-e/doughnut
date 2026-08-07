ALTER TABLE `memory_tracker`
  DROP INDEX `user_note_spelling_active`,
  ADD UNIQUE KEY `user_note_spelling_active` (
    `user_id`,
    `note_id`,
    `type`,
    `property_key`,
    (if((`deleted_at` is null), 1, NULL))
  ),
  DROP COLUMN `spelling`;
