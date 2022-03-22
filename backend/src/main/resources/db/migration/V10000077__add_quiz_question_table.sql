CREATE TABLE `quiz_question` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `review_point_id` int unsigned DEFAULT NULL,
  `question_type` int unsigned DEFAULT NULL,
  `category_link_id` int unsigned DEFAULT NULL,
  `vice_review_point_ids` varchar(50) NOT NULL,
  `option_notes` varchar(50) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_review_point_id` FOREIGN KEY (`review_point_id`) REFERENCES `review_point` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE TABLE `quiz_answer` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `quiz_question_id` int unsigned DEFAULT NULL,
  `answer` varchar(100) DEFAULT NULL,
  `result` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_quiz_answer_id` FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
);
