CREATE TABLE `comment_read_status` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `comment_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL,
  PRIMARY KEY (`id`),
  KEY `fk_comment_id` (`comment_id`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_comment_read_status_comment_id` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_comment_read_status_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
);