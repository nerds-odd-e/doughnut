CREATE TABLE `note_alias_index` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `notebook_id` int unsigned NOT NULL,
  `alias_display` varchar(767) COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_lookup_key` varchar(767) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_note_alias_index_note_lookup` (`note_id`, `alias_lookup_key`),
  KEY `idx_note_alias_index_note_id` (`note_id`),
  KEY `idx_note_alias_index_notebook_lookup` (`notebook_id`, `alias_lookup_key`),
  CONSTRAINT `fk_note_alias_index_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_alias_index_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
