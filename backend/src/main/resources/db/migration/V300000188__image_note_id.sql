ALTER TABLE `image`
  ADD COLUMN `note_id` int unsigned DEFAULT NULL AFTER `attachment_blob_id`;

UPDATE `image` `i`
INNER JOIN (
  SELECT `image_id`, MIN(`note_id`) AS `note_id`
  FROM `note_accessory`
  WHERE `image_id` IS NOT NULL AND `note_id` IS NOT NULL
  GROUP BY `image_id`
) `na` ON `na`.`image_id` = `i`.`id`
SET `i`.`note_id` = `na`.`note_id`;

ALTER TABLE `image`
  ADD KEY `fk_image_note_id` (`note_id`),
  ADD CONSTRAINT `fk_image_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT;
