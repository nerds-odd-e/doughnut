CREATE TABLE `note_property_index` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `property_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_note_property_index_note_key` (`note_id`, `property_key`),
  KEY `idx_note_property_index_property_key` (`property_key`),
  CONSTRAINT `fk_note_property_index_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
