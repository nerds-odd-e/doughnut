DROP TABLE IF EXISTS `marked_questions`;
CREATE TABLE `marked_questions` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `note_id` int unsigned NOT NULL,
  `quiz_question_id` int unsigned DEFAULT NULL,
  `comment` text,
  `is_good` bit NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_quiz_answer_id_2` FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);
