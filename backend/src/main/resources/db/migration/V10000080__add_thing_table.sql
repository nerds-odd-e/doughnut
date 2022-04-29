CREATE TABLE `thing` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` timestamp DEFAULT NULL,
  `note_id` int unsigned NOT NULL,
  `link_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fk_thing_note_source` (`note_id`),
  UNIQUE KEY `fk_thing_link_target` (`link_id`),
  KEY `FK_thing_user_id` (`user_id`),
  KEY `FK_created_at` (`created_at`),
  CONSTRAINT `fk_thing_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_thing_link_id` FOREIGN KEY (`link_id`) REFERENCES `link` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_thing_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO thing (user_id, created_at, deleted_at, note_id)
SELECT user_id, created_at, deleted_at, id FROM note;

INSERT INTO thing (user_id, created_at, link_id)
SELECT user_id, created_at, id FROM link;
