ALTER TABLE `conversation` DROP FOREIGN KEY `fk_conversation_assessment_question`;
ALTER TABLE `conversation` DROP COLUMN `assessment_question_instance_id`;

DROP TABLE IF EXISTS `assessment_question_instance`;
DROP TABLE IF EXISTS `assessment_attempt`;

ALTER TABLE `notebook` DROP COLUMN `number_of_questions_in_assessment`;
