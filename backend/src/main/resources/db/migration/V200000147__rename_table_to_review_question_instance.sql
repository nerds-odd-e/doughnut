RENAME TABLE `quiz_question` TO `review_question_instance`;

ALTER TABLE `quiz_answer`
RENAME COLUMN `quiz_question_id` TO `review_question_instance_id`;
ALTER TABLE `conversation`
RENAME COLUMN `quiz_question_id` TO `review_question_instance_id`;
