CREATE TABLE `note_creator` (
  `note_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  PRIMARY KEY (`note_id`),
  UNIQUE KEY `uk_note_creator_note_user` (`note_id`, `user_id`),
  CONSTRAINT `fk_note_creator_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_creator_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `note_creator` (`note_id`, `user_id`)
SELECT `id`, `creator_id` FROM `note` WHERE `creator_id` IS NOT NULL;

ALTER TABLE `note` DROP FOREIGN KEY `FK_note_creator_id`;
ALTER TABLE `note` DROP COLUMN `creator_id`;
