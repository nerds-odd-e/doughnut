SET foreign_key_checks = 0;

--
-- Table structure for table `audio`
--

CREATE TABLE `audio` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `storage_type` varchar(20) NOT NULL,
  `audio_blob_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_audio_blob_id` (`audio_blob_id`),
  KEY `fk_audio_user_id` (`user_id`),
  CONSTRAINT `fk_audio_blob_id` FOREIGN KEY (`audio_blob_id`) REFERENCES `audio_blob` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_audio_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `audio_blob`
--

CREATE TABLE `audio_blob` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `data` mediumblob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET foreign_key_checks = 1;
