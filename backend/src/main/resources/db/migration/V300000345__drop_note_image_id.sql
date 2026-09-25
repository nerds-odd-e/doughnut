ALTER TABLE `note`
  DROP FOREIGN KEY `fk_note_image_id`,
  DROP INDEX `fk_note_image_id`,
  DROP COLUMN `image_id`;
