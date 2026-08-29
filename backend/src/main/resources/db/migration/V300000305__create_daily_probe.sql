CREATE TABLE `daily_probe` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `completed_at` timestamp(3) NOT NULL,
  `speed` DOUBLE NULL,
  `accuracy` int NOT NULL,
  `lapse_count` int NOT NULL,
  `variability` DOUBLE NULL,
  `trials_json` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `daily_probe_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
