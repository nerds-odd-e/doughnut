-- Unique (note_id, link_text) used utf8mb4_unicode_ci, which treats some distinct
-- spellings as equal (e.g. hiragana vs katakana). Cache rows must match exact
-- [[...]] inner text for replacements, so uniqueness must be byte-accurate.
ALTER TABLE `note_wiki_title_cache`
  DROP INDEX `uq_note_wiki_title_cache_note_link`,
  MODIFY `link_text` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  ADD UNIQUE KEY `uq_note_wiki_title_cache_note_link` (`note_id`, `link_text`);
