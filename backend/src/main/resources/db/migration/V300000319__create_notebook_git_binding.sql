CREATE TABLE `notebook_git_binding` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `accepted_git_object_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `bundle_bytes` longblob NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notebook_git_binding_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_notebook_git_binding_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
