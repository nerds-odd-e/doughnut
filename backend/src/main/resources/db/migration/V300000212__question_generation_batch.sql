CREATE TABLE `question_generation_batch` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `status` varchar(32) NOT NULL,
  `planned_at` timestamp(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_generation_batch_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_question_generation_batch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `question_generation_batch_request` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `batch_id` int unsigned NOT NULL,
  `memory_tracker_id` int unsigned NOT NULL,
  `custom_id` varchar(128) NOT NULL,
  `context_seed` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_question_generation_batch_request_custom_id` (`custom_id`),
  UNIQUE KEY `uq_question_generation_batch_request_batch_tracker` (`batch_id`, `memory_tracker_id`),
  CONSTRAINT `fk_question_generation_batch_request_batch` FOREIGN KEY (`batch_id`) REFERENCES `question_generation_batch` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_question_generation_batch_request_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
