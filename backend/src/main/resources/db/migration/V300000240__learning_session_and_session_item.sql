CREATE TABLE `learning_session` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `notebook_id` int unsigned NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `commissioned_at` timestamp(3) NOT NULL,
  `recorded_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_learning_session_user_notebook_status` (`user_id`,`notebook_id`,`status`),
  CONSTRAINT `fk_learning_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_learning_session_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `session_item` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `learning_session_id` int unsigned NOT NULL,
  `memory_tracker_id` int unsigned NOT NULL,
  `note_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `feedback_score` int NULL DEFAULT NULL,
  `feedback_recorded_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_session_item_learning_session` (`learning_session_id`),
  KEY `fk_session_item_memory_tracker` (`memory_tracker_id`),
  CONSTRAINT `fk_session_item_learning_session` FOREIGN KEY (`learning_session_id`) REFERENCES `learning_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_session_item_memory_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
