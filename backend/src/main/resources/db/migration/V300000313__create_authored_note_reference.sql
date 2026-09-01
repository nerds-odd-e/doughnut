-- Persists authored note references (wiki Portable-path and note-ID URL targets) parsed from a
-- note's own content, one row per reference in document order. Replaced wholesale on each
-- content save (see Note.replaceContent) — not yet read by any production query.
--
-- note_id_url_note_id is authored data, not a foreign key: a note-ID URL can name an id that has
-- no live note. Wiki locator columns and note-ID-url columns are mutually exclusive per kind.

CREATE TABLE `authored_note_reference` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `source_note_id` int unsigned NOT NULL,
  `kind` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `authored_link` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `display_text` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `document_order` int unsigned NOT NULL,
  `wiki_notebook_qualifier` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `wiki_note_portion` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `wiki_encoded_property_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `note_id_url_note_id` int unsigned DEFAULT NULL,
  `note_id_url_href` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_authored_note_reference_source_note_id` (`source_note_id`),
  KEY `idx_authored_note_reference_note_id_url_note_id` (`note_id_url_note_id`),
  KEY `idx_authored_note_reference_wiki_locator` (`wiki_notebook_qualifier`,`wiki_note_portion`(300)),
  CONSTRAINT `fk_authored_note_reference_source_note` FOREIGN KEY (`source_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_authored_note_reference_kind_locator` CHECK (
    (
      `kind` = 'WIKI_PORTABLE_PATH'
      AND `wiki_note_portion` IS NOT NULL
      AND `note_id_url_note_id` IS NULL
      AND `note_id_url_href` IS NULL
    )
    OR
    (
      `kind` = 'NOTE_ID_URL'
      AND `wiki_notebook_qualifier` IS NULL
      AND `wiki_note_portion` IS NULL
      AND `wiki_encoded_property_key` IS NULL
      AND `note_id_url_note_id` IS NOT NULL
      AND `note_id_url_href` IS NOT NULL
    )
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
