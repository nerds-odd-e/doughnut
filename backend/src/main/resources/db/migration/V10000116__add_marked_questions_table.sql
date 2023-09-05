CREATE TABLE `marked_questions` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `note_id` int unsigned NOT NULL,
  `quiz_question_id` int unsigned DEFAULT NULL,
  `comment` text,
  `is_good` bit,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_note_id` (`note_id`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_quiz_answer_id_2` FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);
