CREATE TABLE `note_level_index` (
  `note_id` int unsigned NOT NULL,
  `level` tinyint NOT NULL,
  PRIMARY KEY (`note_id`),
  CONSTRAINT `fk_note_level_index_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_note_level_index_level` CHECK ((`level` BETWEEN 1 AND 6))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
