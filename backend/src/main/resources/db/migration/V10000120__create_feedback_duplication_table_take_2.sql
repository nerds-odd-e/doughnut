DROP TABLE IF EXISTS `feedback_duplication`;
CREATE TABLE `feedback_duplication` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned DEFAULT NULL, 
  `source_feedback_id` int unsigned DEFAULT NULL,
  `quiz_question_id` int unsigned DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `preserved_question` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_feedback_duplication_source_feedback_id` FOREIGN KEY (`source_feedback_id`) REFERENCES `suggested_question_for_fine_tuning` (`id`),
  CONSTRAINT `fk_feedback_duplication_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);
