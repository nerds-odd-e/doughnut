ALTER TABLE `note_property_index`
  ADD COLUMN `item_index` int unsigned NOT NULL DEFAULT 0 AFTER `property_key`,
  DROP INDEX `uq_note_property_index_note_key`,
  ADD UNIQUE KEY `uq_note_property_index_note_key_item` (`note_id`, `property_key`, `item_index`),
  ADD KEY `idx_note_property_index_note_key` (`note_id`, `property_key`);
