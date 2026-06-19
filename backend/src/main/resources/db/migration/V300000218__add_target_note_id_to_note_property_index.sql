ALTER TABLE `note_property_index`
  ADD COLUMN `target_note_id` int unsigned NULL,
  ADD CONSTRAINT `fk_note_property_index_target_note` FOREIGN KEY (`target_note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL,
  ADD KEY `idx_note_property_index_target_note` (`target_note_id`);
