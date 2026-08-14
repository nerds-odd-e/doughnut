CREATE TABLE `assimilation_sequence_skip` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `note_id` int unsigned NOT NULL,
  `property_key` varchar(255) NOT NULL DEFAULT '',
  `skipped_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assimilation_sequence_skip_user_note_property` (`user_id`,`note_id`,`property_key`),
  KEY `fk_assimilation_sequence_skip_note` (`note_id`),
  CONSTRAINT `fk_assimilation_sequence_skip_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assimilation_sequence_skip_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
