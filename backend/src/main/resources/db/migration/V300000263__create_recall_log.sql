CREATE TABLE `recall_log` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `memory_tracker_id` int unsigned NOT NULL,
  `recorded_at` timestamp(3) NOT NULL,
  `elapsed_hours` int DEFAULT NULL,
  `product_outcome` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `answer_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_recall_log_memory_tracker` (`memory_tracker_id`),
  KEY `fk_recall_log_answer` (`answer_id`),
  CONSTRAINT `fk_recall_log_memory_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recall_log_answer` FOREIGN KEY (`answer_id`) REFERENCES `answer` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
