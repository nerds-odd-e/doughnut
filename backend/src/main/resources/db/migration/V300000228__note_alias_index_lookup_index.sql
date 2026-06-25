ALTER TABLE `note_alias_index`
  DROP FOREIGN KEY `fk_note_alias_index_notebook`,
  DROP INDEX `idx_note_alias_index_notebook_lookup`,
  DROP COLUMN `notebook_id`,
  ADD KEY `idx_note_alias_index_lookup` (`alias_lookup_key`);
