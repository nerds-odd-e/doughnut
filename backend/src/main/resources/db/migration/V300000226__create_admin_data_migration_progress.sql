CREATE TABLE `admin_data_migration_progress` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `step_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_count` int NOT NULL DEFAULT '0',
  `processed_count` int NOT NULL DEFAULT '0',
  `last_processed_note_id` int unsigned DEFAULT NULL,
  `last_error` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_admin_data_migration_progress_step` (`step_name`),
  KEY `idx_admin_data_migration_progress_status` (`status`),
  KEY `fk_admin_data_migration_progress_last_note` (`last_processed_note_id`),
  CONSTRAINT `fk_admin_data_migration_progress_last_note` FOREIGN KEY (`last_processed_note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
