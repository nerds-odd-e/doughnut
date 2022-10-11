CREATE TABLE IF NOT EXISTS `location` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `latitude` DOUBLE,
  `longitude` DOUBLE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fk_location_note` (`note_id`),
  CONSTRAINT `fk_location_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
