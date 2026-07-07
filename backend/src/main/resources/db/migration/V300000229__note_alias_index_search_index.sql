ALTER TABLE `note_alias_index`
  DROP INDEX `idx_note_alias_index_lookup`,
  ADD KEY `idx_note_alias_index_lookup_note` (`alias_lookup_key`, `note_id`);
