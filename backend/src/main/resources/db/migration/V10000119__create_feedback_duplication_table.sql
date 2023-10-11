DROP TABLE IF EXISTS `feedback_duplication`;
CREATE TABLE `feedback_duplication` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `source_feedback_id` int unsigned DEFAULT NULL,
  `quiz_question_id` int unsigned DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `preserved_question` text DEFAULT NULL,
  PRIMARY KEY (`id`)
);
