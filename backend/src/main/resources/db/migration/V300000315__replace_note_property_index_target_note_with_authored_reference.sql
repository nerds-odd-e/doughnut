-- Replace note_property_index.target_note_id (a persisted resolved Note) with a nullable relation
-- to authored_note_reference: the one authored reference selected from that property's value.
-- Drop the target column and add the authored-reference relation with fresh identity semantics.

ALTER TABLE `note_property_index`
  DROP FOREIGN KEY `fk_note_property_index_target_note`;

ALTER TABLE `note_property_index`
  DROP KEY `idx_note_property_index_target_note`,
  DROP COLUMN `target_note_id`;

ALTER TABLE `note_property_index`
  ADD COLUMN `authored_note_reference_id` int unsigned DEFAULT NULL AFTER `item_index`,
  ADD KEY `idx_note_property_index_authored_reference` (`authored_note_reference_id`),
  ADD CONSTRAINT `fk_note_property_index_authored_reference` FOREIGN KEY (`authored_note_reference_id`) REFERENCES `authored_note_reference` (`id`) ON DELETE SET NULL;
