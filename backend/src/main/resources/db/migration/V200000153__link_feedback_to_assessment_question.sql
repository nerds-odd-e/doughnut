-- Drop the old conversation table
DROP TABLE IF EXISTS `conversation`;

-- Create the new conversation table
CREATE TABLE `conversation` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `subject_ownership_id` int unsigned NOT NULL,
  `conversation_initiator_id` int unsigned NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `assessment_question_instance_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `subject_ownership_id` (`subject_ownership_id`),
  KEY `conversation_initiator_id` (`conversation_initiator_id`),
  KEY `fk_conversation_assessment_question` (`assessment_question_instance_id`),
  CONSTRAINT `conversation_ibfk_1` FOREIGN KEY (`subject_ownership_id`) REFERENCES `ownership` (`id`),
  CONSTRAINT `conversation_ibfk_2` FOREIGN KEY (`conversation_initiator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_conversation_assessment_question` FOREIGN KEY (`assessment_question_instance_id`) REFERENCES `assessment_question_instance` (`id`)
);
