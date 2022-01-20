CREATE TABLE `text_content` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `language` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_note_id` (`note_id`),
  KEY `text_language` (`language`),
  KEY `title_index` (`title`),
  CONSTRAINT `fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
);

ALTER TABLE note ADD text_content_id int unsigned DEFAULT NULL;
ALTER TABLE note ADD FOREIGN KEY (text_content_id) REFERENCES text_content(id) ON DELETE CASCADE;
