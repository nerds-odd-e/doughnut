CREATE TABLE `question_generation_batch_maintenance_run` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `trigger_source` varchar(32) NOT NULL,
  `started_at` timestamp(3) NOT NULL,
  `finished_at` timestamp(3) DEFAULT NULL,
  `error` text,
  `considered_user_count` int DEFAULT NULL,
  `submitted_count` int DEFAULT NULL,
  `failed_count` int DEFAULT NULL,
  `skipped_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_generation_batch_maintenance_run_trigger_started` (`trigger_source`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
